package com.ai_health.core.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ai_health.core.data.local.dao.HealthMetricDao
import com.ai_health.core.data.local.dao.SleepDao
import com.ai_health.core.data.local.dao.SyncTokenDao
import com.ai_health.core.data.local.entity.CaloriesEntity
import com.ai_health.core.data.local.entity.DistanceEntity
import com.ai_health.core.data.local.entity.ExerciseSessionEntity
import com.ai_health.core.data.local.entity.HeartRateSessionEntity
import com.ai_health.core.data.local.entity.OxygenSaturationEntity
import com.ai_health.core.data.local.entity.SleepSessionEntity
import com.ai_health.core.data.local.entity.SleepStageEntity
import com.ai_health.core.data.local.entity.StepsEntity
import com.ai_health.core.data.local.entity.SyncTokenEntity
import com.ai_health.core.data.local.entity.BasalMetabolicRateEntity
import com.ai_health.core.data.mapper.HealthMappers
import com.ai_health.core.health.HealthChange
import com.ai_health.core.health.HealthConnectManager
import com.ai_health.core.health.RawBMR
import com.ai_health.core.health.RawCalories
import com.ai_health.core.health.RawDistance
import com.ai_health.core.health.RawExercise
import com.ai_health.core.health.RawHeartRate
import com.ai_health.core.health.RawOxygen
import com.ai_health.core.health.RawSleep
import com.ai_health.core.health.RawStep
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.ai_health.core.data.local.entity.HeartRateSample

/**
 * HealthSyncWorker - Worker per sincronizzazione differenziale con Health Connect.
 * 
 * PRIVACY-PROOF FOREGROUND-FIRST SYNC:
 * - Attivato all'apertura dell'app (OneTimeWorkRequest)
 * - Utilizza la Changes API per scaricare solo i delta (nuovi, aggiornati, cancellati)
 * - Gestisce i DeletionChange per conformità privacy (l'utente cancella -> noi cancelliamo)
 * - Pagination loop per consumare tutte le pagine di cambiamenti
 * - Rate limiting con Result.retry() per gestire throttling di Health Connect
 * 
 * Cold-Start Sync: Se il token è scaduto o mancante, esegue sync degli ultimi 30 giorni.
 */
@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val healthConnectManager: HealthConnectManager,
    private val syncTokenDao: SyncTokenDao,
    private val healthDao: HealthMetricDao,
    private val sleepDao: SleepDao
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "HealthSyncWorker"
        private const val TOKEN_KEY = "ALL"
        private const val COLD_START_DAYS = 30L
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting health sync worker")
        
        // Verifica permessi
        if (!healthConnectManager.hasAllPermissions()) {
            Log.w(TAG, "Missing Health Connect permissions, skipping sync")
            return Result.success()
        }
        
        return try {
            // Recupera token esistente o richiedi nuovo
            val savedToken = syncTokenDao.getToken(TOKEN_KEY)
            
            if (savedToken == null) {
                Log.d(TAG, "No token found, performing cold-start sync")
                return performColdStartSync()
            }
            
            // Pagination loop: consuma TUTTE le pagine fino a hasMore = false
            var hasMore = true
            var processedChanges = 0
            var currentToken: String = savedToken  // Non-null after the check above
            
            while (hasMore) {
                Log.d(TAG, "Fetching changes with token: ${currentToken.take(10)}...")
                val result = healthConnectManager.getChanges(currentToken)
                
                // Token scaduto: reset e cold-start
                if (result.tokenExpired) {
                    Log.w(TAG, "Token expired, clearing and performing cold-start sync")
                    syncTokenDao.clearToken(TOKEN_KEY)
                    return performColdStartSync()
                }
                
                // Processa i cambiamenti
                processChanges(result.changes)
                processedChanges += result.changes.size
                
                // Aggiorna token e controlla se ci sono altre pagine
                currentToken = result.nextToken
                hasMore = result.hasMore
                
                Log.d(TAG, "Processed ${result.changes.size} changes, hasMore=$hasMore")
            }
            
            // Salva il nuovo token per la prossima sync
            syncTokenDao.saveToken(
                SyncTokenEntity(
                    dataType = TOKEN_KEY,
                    token = currentToken,
                    lastSyncTime = System.currentTimeMillis()
                )
            )
            
            Log.d(TAG, "Sync completed successfully. Total changes: $processedChanges")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            
            // Rate limiting o errori temporanei: retry con backoff
            if (isRetryableError(e)) {
                Log.w(TAG, "Retryable error, scheduling retry")
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Processa lista di cambiamenti (Upsert + Deletion).
     */
    private suspend fun processChanges(changes: List<HealthChange>) {
        changes.forEach { change ->
            when (change) {
                is HealthChange.Upsert -> handleUpsert(change)
                is HealthChange.Deletion -> handleDeletion(change)
            }
        }
    }
    
    /**
     * Gestisce inserimento/aggiornamento di un record.
     * Applica filtering per dispositivi affidabili.
     */
    private suspend fun handleUpsert(change: HealthChange.Upsert) {
        val data = change.data
        
        when (change.recordType) {
            "STEPS" -> {
                val raw = data as RawStep
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    val entity = StepsEntity(
                        id = raw.id,
                        count = raw.count,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime),
                        source = raw.sourcePackage
                    )
                    healthDao.insertSteps(listOf(entity))
                }
            }
            
            "HEART_RATE" -> {
                val raw = data as RawHeartRate
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    // Converti samples in JSON offset-based
                    val samples = raw.samples.map { 
                        HeartRateSample(offsetMs = it.offsetMs, bpm = it.bpm) 
                    }
                    val samplesJson = Json.encodeToString(samples)
                    
                    val entity = HeartRateSessionEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        deviceType = raw.deviceType,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime),
                        samplesJson = samplesJson
                    )
                    healthDao.insertHeartRateSessions(listOf(entity))
                }
            }
            
            "SLEEP" -> {
                val raw = data as RawSleep
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    val sessionEntity = SleepSessionEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        title = null,
                        notes = null,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime)
                    )
                    
                    val stageEntities = raw.stages.map { stageDto ->
                        SleepStageEntity(
                            id = HealthMappers.generateId("SLEEP_STAGE", Instant.ofEpochMilli(stageDto.startTime)),
                            sleepSessionId = raw.id,
                            source = raw.sourcePackage,
                            stage = stageDto.stage,
                            startTime = Instant.ofEpochMilli(stageDto.startTime),
                            endTime = Instant.ofEpochMilli(stageDto.endTime)
                        )
                    }
                    
                    sleepDao.insertSleepWithStages(sessionEntity, stageEntities)
                }
            }
            
            "DISTANCE" -> {
                val raw = data as RawDistance
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    val entity = DistanceEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        distanceMeters = raw.distanceMeters,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime)
                    )
                    healthDao.insertDistances(listOf(entity))
                }
            }
            
            "CALORIES" -> {
                val raw = data as RawCalories
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    val entity = CaloriesEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        energyKilocalories = raw.kilocalories,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime)
                    )
                    healthDao.insertCalories(listOf(entity))
                }
            }
            
            "OXYGEN" -> {
                val raw = data as RawOxygen
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    val entity = OxygenSaturationEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        percentage = raw.percentage,
                        time = Instant.ofEpochMilli(raw.startTime)
                    )
                    healthDao.insertOxygen(listOf(entity))
                }
            }
            
            "EXERCISE" -> {
                val raw = data as RawExercise
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    val entity = ExerciseSessionEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        exerciseType = raw.type,
                        title = raw.title,
                        notes = raw.notes,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime)
                    )
                    healthDao.insertExercises(listOf(entity))
                }
            }
            
            "BMR" -> {
                val raw = data as RawBMR
                if (TrustedDeviceFilter.isTrusted(raw.deviceType, raw.sourcePackage)) {
                    val entity = BasalMetabolicRateEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        energyKilocaloriesPerDay = raw.kcalPerDay,
                        time = Instant.ofEpochMilli(raw.startTime)
                    )
                    healthDao.insertBmr(listOf(entity))
                }
            }
        }
    }
    
    /**
     * PRIVACY COMPLIANCE: Gestisce cancellazione fisica di un record.
     * Quando l'utente cancella un dato da Health Connect, DEVE essere rimosso anche localmente.
     */
    private suspend fun handleDeletion(change: HealthChange.Deletion) {
        val recordId = change.recordId
        Log.d(TAG, "Processing deletion for record: $recordId")
        
        // Poiché non sappiamo il tipo esatto, proviamo a cancellare da tutte le tabelle
        // Solo una avrà successo (l'ID è univoco globalmente in Health Connect)
        healthDao.deleteStepsById(recordId)
        healthDao.deleteHeartRateById(recordId)
        healthDao.deleteDistanceById(recordId)
        healthDao.deleteCaloriesById(recordId)
        healthDao.deleteOxygenById(recordId)
        healthDao.deleteExerciseById(recordId)
        healthDao.deleteBmrById(recordId)
        sleepDao.deleteSleepSessionById(recordId)
    }
    
    /**
     * Cold-Start Sync: Sincronizza gli ultimi 30 giorni quando non c'è token.
     */
    private suspend fun performColdStartSync(): Result {
        Log.d(TAG, "Performing cold-start sync for last $COLD_START_DAYS days")
        
        val now = Instant.now()
        val startTime = now.minus(COLD_START_DAYS, ChronoUnit.DAYS)
        
        try {
            // Fetch tutti i tipi di dati
            val steps = healthConnectManager.fetchSteps(startTime, now)
            val heartRate = healthConnectManager.fetchHeartRate(startTime, now)
            val sleep = healthConnectManager.fetchSleep(startTime, now)
            val distance = healthConnectManager.fetchDistance(startTime, now)
            val calories = healthConnectManager.fetchCalories(startTime, now)
            val oxygen = healthConnectManager.fetchOxygenSaturation(startTime, now)
            val exercise = healthConnectManager.fetchExercise(startTime, now)
            val bmr = healthConnectManager.fetchBMR(startTime, now)
            
            // Filtra per dispositivi affidabili e inserisci
            val trustedSteps = TrustedDeviceFilter.filterTrusted(steps, { it.deviceType }, { it.sourcePackage })
            healthDao.insertSteps(trustedSteps.map { raw ->
                StepsEntity(
                    id = raw.id,
                    count = raw.count,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime),
                    source = raw.sourcePackage
                )
            })
            
            // HEART RATE SESSIONS (offset-based)
            val trustedHeartRate = TrustedDeviceFilter.filterTrusted(heartRate, { it.deviceType }, { it.sourcePackage })
            healthDao.insertHeartRateSessions(trustedHeartRate.map { raw ->
                val samples = raw.samples.map { HeartRateSample(offsetMs = it.offsetMs, bpm = it.bpm) }
                HeartRateSessionEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    deviceType = raw.deviceType,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime),
                    samplesJson = Json.encodeToString(samples)
                )
            })
            Log.d(TAG, "Inserted ${trustedHeartRate.size} heart rate sessions")
            
            // SLEEP
            val trustedSleep = TrustedDeviceFilter.filterTrusted(sleep, { it.deviceType }, { it.sourcePackage })
            trustedSleep.forEach { raw ->
                val sessionEntity = SleepSessionEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    title = null,
                    notes = null,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime)
                )
                val stageEntities = raw.stages.map { stageDto ->
                    SleepStageEntity(
                        id = HealthMappers.generateId("SLEEP_STAGE", Instant.ofEpochMilli(stageDto.startTime)),
                        sleepSessionId = raw.id,
                        source = raw.sourcePackage,
                        stage = stageDto.stage,
                        startTime = Instant.ofEpochMilli(stageDto.startTime),
                        endTime = Instant.ofEpochMilli(stageDto.endTime)
                    )
                }
                sleepDao.insertSleepWithStages(sessionEntity, stageEntities)
            }
            
            // DISTANCE, CALORIES, OXYGEN, etc.
            val trustedDistance = TrustedDeviceFilter.filterTrusted(distance, { it.deviceType }, { it.sourcePackage })
            healthDao.insertDistances(trustedDistance.map { raw ->
                DistanceEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    distanceMeters = raw.distanceMeters,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime)
                )
            })
            
            val trustedCalories = TrustedDeviceFilter.filterTrusted(calories, { it.deviceType }, { it.sourcePackage })
            healthDao.insertCalories(trustedCalories.map { raw ->
                CaloriesEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    energyKilocalories = raw.kilocalories,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime)
                )
            })
            
            // Ottieni nuovo token per future sync differenziali
            val newToken = healthConnectManager.getChangesToken()
            syncTokenDao.saveToken(
                SyncTokenEntity(
                    dataType = TOKEN_KEY,
                    token = newToken,
                    lastSyncTime = System.currentTimeMillis()
                )
            )
            
            Log.d(TAG, "Cold-start sync completed. Fetched ${steps.size} steps, ${heartRate.size} HR sessions, ${sleep.size} sleep sessions")
            return Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cold-start sync", e)
            return if (isRetryableError(e)) Result.retry() else Result.failure()
        }
    }
    
    /**
     * Determina se l'errore è temporaneo e può essere ritentato.
     */
    private fun isRetryableError(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: ""
        return message.contains("rate") || 
               message.contains("limit") ||
               message.contains("timeout") ||
               message.contains("temporary")
    }
}
