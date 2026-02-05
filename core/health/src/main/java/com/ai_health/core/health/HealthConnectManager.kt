package com.ai_health.core.health

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

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

    // --- FETCH METHODS ---

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
                    sourcePackage = record.metadata.dataOrigin.packageName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching steps", e)
            emptyList()
        }
    }

    suspend fun fetchHeartRate(start: Instant, end: Instant): List<RawHeartRate> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.map { record ->
                val avgBpm = if (record.samples.isNotEmpty()) {
                    record.samples.map { it.beatsPerMinute }.average()
                } else 0.0

                RawHeartRate(
                    id = record.metadata.id,
                    bpm = avgBpm,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourcePackage = record.metadata.dataOrigin.packageName
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

    // --- SYNC METHODS ---

    suspend fun getChangesToken(): String? {
        return try {
            val request = androidx.health.connect.client.request.ChangesTokenRequest(
                recordTypes = setOf(
                    StepsRecord::class,
                    HeartRateRecord::class,
                    SleepSessionRecord::class,
                    OxygenSaturationRecord::class,
                    DistanceRecord::class,
                    ActiveCaloriesBurnedRecord::class,
                    BasalMetabolicRateRecord::class,
                    ExerciseSessionRecord::class
                )
            )
            healthConnectClient.getChangesToken(request)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting changes token", e)
            null
        }
    }

    suspend fun getChanges(token: String): androidx.health.connect.client.response.ChangesResponse? {
        return try {
            healthConnectClient.getChanges(token)
        } catch (e: Exception) {
            // Let the repository handle specific exceptions like "Expired Token"
            // But we log it here.
            Log.e(TAG, "Error getting changes", e)
            throw e
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
