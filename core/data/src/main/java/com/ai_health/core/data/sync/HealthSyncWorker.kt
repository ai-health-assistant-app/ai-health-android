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
                // REMOVED BLOCKING FILTER: TrustedDeviceFilter.isTrusted(...)
                val entity = StepsEntity(
                    id = raw.id,
                    count = raw.count,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime),
                    source = raw.sourcePackage,
                    deviceType = raw.deviceType // Capture device type for future prioritization
                )
                healthDao.insertSteps(listOf(entity))
            }
            
            "HEART_RATE" -> {
                val raw = data as RawHeartRate
                // REMOVED BLOCKING FILTER
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
            
            "SLEEP" -> {
                val raw = data as RawSleep
                // REMOVED BLOCKING FILTER
                val sessionEntity = SleepSessionEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    title = null,
                    notes = null,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime),
                    deviceType = raw.deviceType
                )
                
                val stageEntities = raw.stages.map { stageDto ->
                    SleepStageEntity(
                        id = HealthMappers.generateId("SLEEP_STAGE", Instant.ofEpochMilli(stageDto.startTime)),
                        sleepSessionId = raw.id,
                        source = raw.sourcePackage,
                        stage = stageDto.stage,
                        startTime = Instant.ofEpochMilli(stageDto.startTime),
                        endTime = Instant.ofEpochMilli(stageDto.endTime),
                        deviceType = raw.deviceType
                    )
                }
                
                sleepDao.insertSleepWithStages(sessionEntity, stageEntities)
            }
            
            "DISTANCE" -> {
                val raw = data as RawDistance
                // REMOVED BLOCKING FILTER
                val entity = DistanceEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    distanceMeters = raw.distanceMeters,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime),
                    deviceType = raw.deviceType
                )
                healthDao.insertDistances(listOf(entity))
            }
            
            "CALORIES" -> {
                val raw = data as RawCalories
                // REMOVED BLOCKING FILTER
                val entity = CaloriesEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    energyKilocalories = raw.kilocalories,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime),
                    deviceType = raw.deviceType
                )
                healthDao.insertCalories(listOf(entity))
            }
            
            "OXYGEN" -> {
                val raw = data as RawOxygen
                // REMOVED BLOCKING FILTER
                val entity = OxygenSaturationEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    percentage = raw.percentage,
                    time = Instant.ofEpochMilli(raw.startTime),
                    deviceType = raw.deviceType
                )
                healthDao.insertOxygen(listOf(entity))
            }
            
            "EXERCISE" -> {
                val raw = data as RawExercise
                // REMOVED BLOCKING FILTER
                val entity = ExerciseSessionEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    exerciseType = raw.type,
                    title = raw.title,
                    notes = raw.notes,
                    startTime = Instant.ofEpochMilli(raw.startTime),
                    endTime = Instant.ofEpochMilli(raw.endTime),
                    deviceType = raw.deviceType
                )
                healthDao.insertExercises(listOf(entity))
            }
            
            "BMR" -> {
                val raw = data as RawBMR
                // REMOVED BLOCKING FILTER
                val entity = BasalMetabolicRateEntity(
                    id = raw.id,
                    source = raw.sourcePackage,
                    energyKilocaloriesPerDay = raw.kcalPerDay,
                    time = Instant.ofEpochMilli(raw.startTime),
                    deviceType = raw.deviceType
                )
                healthDao.insertBmr(listOf(entity))
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
        healthDao.deleteHeartRateSessionById(recordId)
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
            // Fetch e inserimento per ogni tipo di dato gestito individualmente
            
            // A. STEPS
            runCatching {
                val steps = healthConnectManager.fetchSteps(startTime, now)
                // Filter REMOVED, saving all data
                healthDao.insertSteps(steps.map { raw ->
                    StepsEntity(
                        id = raw.id,
                        count = raw.count,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime),
                        source = raw.sourcePackage,
                        deviceType = raw.deviceType
                    )
                })
                Log.d(TAG, "Inserted ${steps.size} steps records")
            }.onFailure { e -> Log.e(TAG, "Error syncing steps", e) }
            
            // B. HEART RATE
            runCatching {
                val heartRate = healthConnectManager.fetchHeartRate(startTime, now)
                // Filter REMOVED, saving all data
                healthDao.insertHeartRateSessions(heartRate.map { raw ->
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
                Log.d(TAG, "Inserted ${heartRate.size} heart rate sessions")
            }.onFailure { e -> Log.e(TAG, "Error syncing heart rate", e) }

            // C. SLEEP
            runCatching {
                val sleep = healthConnectManager.fetchSleep(startTime, now)
                // Filter REMOVED, saving all data
                sleep.forEach { raw ->
                    val sessionEntity = SleepSessionEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        title = null,
                        notes = null,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime),
                        deviceType = raw.deviceType
                    )
                    val stageEntities = raw.stages.map { stageDto ->
                        SleepStageEntity(
                            id = HealthMappers.generateId("SLEEP_STAGE", Instant.ofEpochMilli(stageDto.startTime)),
                            sleepSessionId = raw.id,
                            source = raw.sourcePackage,
                            stage = stageDto.stage,
                            startTime = Instant.ofEpochMilli(stageDto.startTime),
                            endTime = Instant.ofEpochMilli(stageDto.endTime),
                            deviceType = raw.deviceType
                        )
                    }
                    sleepDao.insertSleepWithStages(sessionEntity, stageEntities)
                }
                Log.d(TAG, "Inserted ${sleep.size} sleep sessions")
            }.onFailure { e -> Log.e(TAG, "Error syncing sleep", e) }
            
            // D. DISTANCE
            runCatching {
                val distance = healthConnectManager.fetchDistance(startTime, now)
                // Filter REMOVED, saving all data
                healthDao.insertDistances(distance.map { raw ->
                    DistanceEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        distanceMeters = raw.distanceMeters,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime),
                        deviceType = raw.deviceType
                    )
                })
                Log.d(TAG, "Inserted ${distance.size} distance records")
            }.onFailure { e -> Log.e(TAG, "Error syncing distance", e) }
            
            // E. CALORIES
            runCatching {
                val calories = healthConnectManager.fetchCalories(startTime, now)
                // Filter REMOVED, saving all data
                healthDao.insertCalories(calories.map { raw ->
                    CaloriesEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        energyKilocalories = raw.kilocalories,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime),
                        deviceType = raw.deviceType
                    )
                })
                Log.d(TAG, "Inserted ${calories.size} calories records")
            }.onFailure { e -> Log.e(TAG, "Error syncing calories", e) }

            // F. OXYGEN
            runCatching {
                val oxygen = healthConnectManager.fetchOxygenSaturation(startTime, now)
                // Filter REMOVED, saving all data
                healthDao.insertOxygen(oxygen.map { raw ->
                    OxygenSaturationEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        percentage = raw.percentage,
                        time = Instant.ofEpochMilli(raw.startTime),
                        deviceType = raw.deviceType
                    )
                })
                Log.d(TAG, "Inserted ${oxygen.size} oxygen records")
            }.onFailure { e -> Log.e(TAG, "Error syncing oxygen", e) }

            // G. EXERCISE
            runCatching {
                val exercise = healthConnectManager.fetchExercise(startTime, now)
                // Filter REMOVED, saving all data
                healthDao.insertExercises(exercise.map { raw ->
                    ExerciseSessionEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        exerciseType = raw.type,
                        title = raw.title,
                        notes = raw.notes,
                        startTime = Instant.ofEpochMilli(raw.startTime),
                        endTime = Instant.ofEpochMilli(raw.endTime),
                        deviceType = raw.deviceType
                    )
                })
                Log.d(TAG, "Inserted ${exercise.size} exercise sessions")
            }.onFailure { e -> Log.e(TAG, "Error syncing exercises", e) }

            // H. BMR
            runCatching {
                val bmr = healthConnectManager.fetchBMR(startTime, now)
                // Filter REMOVED, saving all data
                healthDao.insertBmr(bmr.map { raw ->
                    BasalMetabolicRateEntity(
                        id = raw.id,
                        source = raw.sourcePackage,
                        energyKilocaloriesPerDay = raw.kcalPerDay,
                        time = Instant.ofEpochMilli(raw.startTime),
                        deviceType = raw.deviceType
                    )
                })
                Log.d(TAG, "Inserted ${bmr.size} BMR records")
            }.onFailure { e -> Log.e(TAG, "Error syncing BMR", e) }
            
            // Ottieni nuovo token per future sync differenziali
            val newToken = healthConnectManager.getChangesToken()
            syncTokenDao.saveToken(
                SyncTokenEntity(
                    dataType = TOKEN_KEY,
                    token = newToken,
                    lastSyncTime = System.currentTimeMillis()
                )
            )
            
            Log.d(TAG, "Cold-start sync completed successfully.")
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
