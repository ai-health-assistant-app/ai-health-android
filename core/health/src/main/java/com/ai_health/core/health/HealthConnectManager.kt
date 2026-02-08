package com.ai_health.core.health

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

/**
 * HealthConnectManager is a "pure" component (Clean Architecture compliant).
 * It talks only to Android APIs and returns DTOs.
 */
class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val TAG = "HealthManager"

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRecentWearablePackage(): String? {
        try {
            val startTime = Instant.now().minus(7, ChronoUnit.DAYS)
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startTime, Instant.now()))
            )
            return response.records.firstOrNull {
                it.metadata.device?.type == Device.TYPE_WATCH ||
                        it.metadata.device?.type == Device.TYPE_FITNESS_BAND
            }?.metadata?.dataOrigin?.packageName
        } catch (e: Exception) {
            return null
        }
    }

    fun openCompanionApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "App companion not found", Toast.LENGTH_SHORT).show()
        }
    }

    // --- CHANGES API (Differential Sync) ---
    
    /**
     * Ottiene un token per la sincronizzazione differenziale.
     * Il token cattura lo stato corrente e permette di richiedere solo i cambiamenti futuri.
     */
    suspend fun getChangesToken(): String {
        val request = ChangesTokenRequest(
            recordTypes = setOf(
                StepsRecord::class,
                HeartRateRecord::class,
                SleepSessionRecord::class,
                DistanceRecord::class,
                ActiveCaloriesBurnedRecord::class,
                OxygenSaturationRecord::class,
                ExerciseSessionRecord::class,
                BasalMetabolicRateRecord::class
            )
        )
        return healthConnectClient.getChangesToken(request)
    }
    
    /**
     * Recupera i cambiamenti (delta) dal token specificato.
     * Ritorna Upsert e Deletion changes per la sincronizzazione differenziale.
     * 
     * PRIVACY-PROOF: I DeletionChange DEVONO essere processati per rimuovere
     * i dati dal database locale quando l'utente li cancella da Health Connect.
     */
    suspend fun getChanges(token: String): HealthChangesResult {
        return try {
            val response = healthConnectClient.getChanges(token)
            
            val changes = response.changes.mapNotNull { change ->
                when (change) {
                    is UpsertionChange -> {
                        val record = change.record
                        mapRecordToUpsertChange(record)
                    }
                    is DeletionChange -> {
                        // Per le deletion, non sappiamo il tipo esatto dal change
                        // ma abbiamo l'ID che è sufficiente per cercare nelle tabelle
                        HealthChange.Deletion(
                            recordType = "UNKNOWN", // Sarà determinato cercando in tutte le tabelle
                            recordId = change.recordId
                        )
                    }
                    else -> null
                }
            }
            
            HealthChangesResult(
                changes = changes,
                nextToken = response.nextChangesToken,
                hasMore = response.hasMore,
                tokenExpired = false
            )
        } catch (e: Exception) {
            // TOKEN_EXPIRED o altri errori
            if (e.message?.contains("TOKEN_EXPIRED", ignoreCase = true) == true ||
                e.message?.contains("invalid", ignoreCase = true) == true) {
                HealthChangesResult(
                    changes = emptyList(),
                    nextToken = "",
                    hasMore = false,
                    tokenExpired = true
                )
            } else {
                throw e
            }
        }
    }
    
    /**
     * Mappa un Record di Health Connect in un HealthChange.Upsert.
     */
    private fun mapRecordToUpsertChange(record: Record): HealthChange.Upsert? {
        return when (record) {
            is StepsRecord -> HealthChange.Upsert(
                recordType = "STEPS",
                recordId = record.metadata.id,
                data = RawStep(
                    id = record.metadata.id,
                    count = record.count,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            )
            is HeartRateRecord -> {
                val startTimeMs = record.startTime.toEpochMilli()
                HealthChange.Upsert(
                    recordType = "HEART_RATE",
                    recordId = record.metadata.id,
                    data = RawHeartRate(
                        id = record.metadata.id,
                        samples = record.samples.map { sample ->
                            RawHeartRateSample(
                                offsetMs = (sample.time.toEpochMilli() - startTimeMs).toInt(),
                                bpm = sample.beatsPerMinute.toInt()
                            )
                        },
                        startTime = startTimeMs,
                        endTime = record.endTime.toEpochMilli(),
                        sourcePackage = record.metadata.dataOrigin.packageName,
                        deviceType = getDeviceType(record.metadata.device)
                    )
                )
            }
            is SleepSessionRecord -> HealthChange.Upsert(
                recordType = "SLEEP",
                recordId = record.metadata.id,
                data = RawSleep(
                    id = record.metadata.id,
                    durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
                    stage = 0,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    stages = record.stages.map { RawSleepStage(it.stage, it.startTime.toEpochMilli(), it.endTime.toEpochMilli()) },
                    deviceType = getDeviceType(record.metadata.device)
                )
            )
            is DistanceRecord -> HealthChange.Upsert(
                recordType = "DISTANCE",
                recordId = record.metadata.id,
                data = RawDistance(
                    id = record.metadata.id,
                    distanceMeters = record.distance.inMeters,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            )
            is ActiveCaloriesBurnedRecord -> HealthChange.Upsert(
                recordType = "CALORIES",
                recordId = record.metadata.id,
                data = RawCalories(
                    id = record.metadata.id,
                    kilocalories = record.energy.inKilocalories,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            )
            is OxygenSaturationRecord -> HealthChange.Upsert(
                recordType = "OXYGEN",
                recordId = record.metadata.id,
                data = RawOxygen(
                    id = record.metadata.id,
                    percentage = record.percentage.value,
                    startTime = record.time.toEpochMilli(),
                    endTime = record.time.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            )
            is ExerciseSessionRecord -> HealthChange.Upsert(
                recordType = "EXERCISE",
                recordId = record.metadata.id,
                data = RawExercise(
                    id = record.metadata.id,
                    type = "Exercise_${record.exerciseType}",
                    durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
                    title = record.title,
                    notes = record.notes,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            )
            is BasalMetabolicRateRecord -> HealthChange.Upsert(
                recordType = "BMR",
                recordId = record.metadata.id,
                data = RawBMR(
                    id = record.metadata.id,
                    kcalPerDay = record.basalMetabolicRate.inKilocaloriesPerDay,
                    startTime = record.time.toEpochMilli(),
                    endTime = record.time.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            )
            else -> null
        }
    }
    
    /**
     * Helper per estrarre il tipo di dispositivo dai metadati.
     * Usato per il filtering di dispositivi affidabili.
     */
    private fun getDeviceType(device: Device?): String {
        return when (device?.type) {
            Device.TYPE_WATCH -> "WATCH"
            Device.TYPE_FITNESS_BAND -> "FITNESS_BAND"
            Device.TYPE_PHONE -> "PHONE"
            Device.TYPE_CHEST_STRAP -> "CHEST_STRAP"
            Device.TYPE_RING -> "RING"
            Device.TYPE_SCALE -> "SCALE"
            else -> "UNKNOWN"
        }
    }

    // --- FETCH METHODS (Legacy - utilizzati per cold-start sync) ---

    suspend fun fetchSteps(start: Instant, end: Instant): List<RawStep> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                RawStep(
                    id = record.metadata.id,
                    count = record.count,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching steps", e)
            emptyList()
        }
    }

    /**
     * Fetch HeartRate con campioni offset-based per ottimizzazione HRV.
     */
    suspend fun fetchHeartRate(start: Instant, end: Instant): List<RawHeartRate> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                val startTimeMs = record.startTime.toEpochMilli()
                RawHeartRate(
                    id = record.metadata.id,
                    samples = record.samples.map { sample ->
                        RawHeartRateSample(
                            offsetMs = (sample.time.toEpochMilli() - startTimeMs).toInt(),
                            bpm = sample.beatsPerMinute.toInt()
                        )
                    },
                    startTime = startTimeMs,
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    deviceType = getDeviceType(record.metadata.device)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching heart rate", e)
            emptyList()
        }
    }

    suspend fun fetchSleep(start: Instant, end: Instant): List<RawSleep> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                val durationMin = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toDouble()

                RawSleep(
                    id = record.metadata.id,
                    durationMinutes = durationMin,
                    stage = 0, 
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName,
                    stages = record.stages.map { stageRecord ->
                        RawSleepStage(
                            stage = stageRecord.stage,
                            startTime = stageRecord.startTime.toEpochMilli(),
                            endTime = stageRecord.endTime.toEpochMilli()
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sleep", e)
            emptyList()
        }
    }

    suspend fun fetchOxygenSaturation(start: Instant, end: Instant): List<RawOxygen> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                RawOxygen(
                    id = record.metadata.id,
                    percentage = record.percentage.value,
                    startTime = record.time.toEpochMilli(), 
                    endTime = record.time.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching SpO2", e)
            emptyList()
        }
    }

    suspend fun fetchDistance(start: Instant, end: Instant): List<RawDistance> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(DistanceRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                RawDistance(
                    id = record.metadata.id,
                    distanceMeters = record.distance.inMeters,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching distance", e)
            emptyList()
        }
    }

    suspend fun fetchCalories(start: Instant, end: Instant): List<RawCalories> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                RawCalories(
                    id = record.metadata.id,
                    kilocalories = record.energy.inKilocalories,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching calories", e)
            emptyList()
        }
    }

    suspend fun fetchBMR(start: Instant, end: Instant): List<RawBMR> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(BasalMetabolicRateRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                RawBMR(
                    id = record.metadata.id,
                    kcalPerDay = record.basalMetabolicRate.inKilocaloriesPerDay,
                    startTime = record.time.toEpochMilli(), 
                    endTime = record.time.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching BMR", e)
            emptyList()
        }
    }

    suspend fun fetchExercise(start: Instant, end: Instant): List<RawExercise> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                val duration = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toDouble()
                RawExercise(
                    id = record.metadata.id,
                    type = "Exercise_${record.exerciseType}", 
                    durationMinutes = duration,
                    title = record.title,
                    notes = record.notes,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching exercises", e)
            emptyList()
        }
    }

    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
        )
    }
}
