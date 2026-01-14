package com.ai_health.assistant.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import android.util.Log
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import com.ai_health.assistant.data.repository.HealthCacheEntity
import com.ai_health.assistant.data.repository.AppDatabase
import com.ai_health.assistant.domain.model.StepsRec
import kotlinx.coroutines.flow.first
import com.ai_health.assistant.domain.model.*

/**
 * Data class representing a collection of health data retrieved from Health Connect.
 */
data class HealthDataResult(
    val heartRates: List<HeartRateRecord>,
    val steps: List<StepsRecord>,
    val exercises: List<ExerciseSessionRecord>,
    val calories: List<TotalCaloriesBurnedRecord>,
    val sleep: List<SleepSessionRecord>,
    val oxygen: List<OxygenSaturationRecord>,
    val distance: List<DistanceRecord>,
    val basal: List<BasalMetabolicRateRecord>
)

/**
 * Manager class responsible for interacting with the Health Connect API.
 * It handles permission checks, data reading, and caching retrieved data into the local database.
 */
class HealthConnectManager(private val context: Context) {
    
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val TAG = "HealthManager"
    private val db by lazy { AppDatabase.getDatabase(context) }
    private val healthCacheDao by lazy { db.healthCacheDao() }

    private val DEFAULT_HISTORY_DAYS = 1L

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fullResetAndResync() {
        try {
            healthCacheDao.clearAll()
            syncAllData()
            Log.d(TAG, "✅ Full reset and resync completed.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during full reset", e)
        }
    }

    suspend fun syncAllData() {
        syncSteps()
        syncHeartRate()
        syncExercise()
        syncOxygenSaturation()
        syncDistance()
        syncSleep()
        syncCalories()
        syncBasalMetabolicRate()
    }

    suspend fun syncSteps(): List<StepsRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("STEPS")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            Log.d(TAG, "Syncing steps from $startTime to $endTime")

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val stepsRecList = response.records.map { record ->
                StepsRec(
                    count = record.count,
                    startTime = record.startTime,
                    endTime = record.endTime
                )
            }

            val entities = response.records.map { record ->
                HealthCacheEntity(
                    type = "STEPS",
                    value = record.count.toDouble(),
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName
                )
            }
            
            if (entities.isNotEmpty()) {
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} new step records to Room")
            }

            return stepsRecList
        } catch (e: Exception) {
            Log.e(TAG, "Error during steps sync", e)
            return emptyList()
        }
    }

    suspend fun syncHeartRate(): List<HeartRateRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("HEART_RATE")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val heartRateRecList = mutableListOf<HeartRateRec>()
            val entities = mutableListOf<HealthCacheEntity>()

            response.records.forEach { record ->
                record.samples.forEach { sample ->
                    heartRateRecList.add(HeartRateRec(beatsPerMinute = sample.beatsPerMinute, time = sample.time))
                    entities.add(
                        HealthCacheEntity(
                            type = "HEART_RATE",
                            value = sample.beatsPerMinute.toDouble(),
                            startTime = sample.time.toEpochMilli(),
                            endTime = sample.time.toEpochMilli(),
                            sourceApp = record.metadata.dataOrigin.packageName
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} heart rate samples to Room")
            }
            return heartRateRecList
        } catch (e: Exception) {
            Log.e(TAG, "Error during heart rate sync", e)
            return emptyList()
        }
    }

    suspend fun syncExercise(): List<ExerciseSessionRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("EXERCISE")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val domainList = response.records.map { record ->
                ExerciseSessionRec(
                    exerciseType = record.exerciseType,
                    title = record.title,
                    notes = record.notes,
                    startTime = record.startTime,
                    endTime = record.endTime
                )
            }

            val entities = response.records.map { record ->
                HealthCacheEntity(
                    type = "EXERCISE",
                    value = record.exerciseType.toDouble(),
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName,
                    metadata = record.title ?: ""
                )
            }

            if (entities.isNotEmpty()) {
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} new exercise records to Room")
            }
            return domainList
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing exercises", e)
            return emptyList()
        }
    }

    suspend fun syncOxygenSaturation(): List<OxygenSaturationRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("OXYGEN_SATURATION")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val domainList = response.records.map { OxygenSaturationRec(it.percentage.value, it.time) }
            val entities = response.records.map { record ->
                HealthCacheEntity(
                    type = "OXYGEN_SATURATION",
                    value = record.percentage.value,
                    startTime = record.time.toEpochMilli(),
                    endTime = record.time.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName
                )
            }

            if (entities.isNotEmpty()) {
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} new oxygen saturation records to Room")
            }
            return domainList
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing oxygen saturation", e)
            return emptyList()
        }
    }

    suspend fun syncDistance(): List<DistanceRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("DISTANCE")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val domainList = response.records.map { DistanceRec(it.distance.inMeters, it.startTime, it.endTime) }
            val entities = response.records.map { record ->
                HealthCacheEntity(
                    type = "DISTANCE",
                    value = record.distance.inMeters,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName
                )
            }

            if (entities.isNotEmpty()) {
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} new distance records to Room")
            }

            return domainList
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing distance", e)
            return emptyList()
        }
    }

    suspend fun syncSleep(): List<SleepSessionRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("SLEEP")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val domainList = response.records.map { SleepSessionRec(it.title, it.notes, it.startTime, it.endTime) }
            val entities = response.records.map { record ->
                val durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                HealthCacheEntity(
                    type = "SLEEP",
                    value = durationMinutes.toDouble(),
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName
                )
            }

            if (entities.isNotEmpty()){
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} new sleep records to Room")
            }
            return domainList
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing sleep", e)
            return emptyList()
        }
    }

    suspend fun syncCalories(): List<CaloriesRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("CALORIES")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val domainList = response.records.map { CaloriesRec(it.energy.inKilocalories, it.startTime, it.endTime) }
            val entities = response.records.map { record ->
                HealthCacheEntity(
                    type = "CALORIES",
                    value = record.energy.inKilocalories,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName
                )
            }

            if (entities.isNotEmpty()) {
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} new calories records to Room")
            }
            return domainList
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing calories", e)
            return emptyList()
        }
    }

    suspend fun syncBasalMetabolicRate(): List<BasalMetabolicRateRec> {
        try {
            val lastSyncTime = healthCacheDao.getLastSyncTime("BASAL_METABOLIC_RATE")
            val startTime = lastSyncTime?.let { Instant.ofEpochMilli(it).plusMillis(1) }
                ?: ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()

            if (startTime.isAfter(endTime)) return emptyList()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = BasalMetabolicRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val domainList = response.records.map { BasalMetabolicRateRec(it.basalMetabolicRate.inKilocaloriesPerDay, it.time) }
            val entities = response.records.map { record ->
                HealthCacheEntity(
                    type = "BASAL_METABOLIC_RATE",
                    value = record.basalMetabolicRate.inKilocaloriesPerDay,
                    startTime = record.time.toEpochMilli(),
                    endTime = record.time.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName
                )
            }

            if (entities.isNotEmpty()) {
                healthCacheDao.insertAll(entities)
                Log.d(TAG, "Saved ${entities.size} new BMR records to Room")
            }
            return domainList
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing BMR", e)
            return emptyList()
        }
    }

    suspend fun getTotalStepsFromCache(): Long {
        return healthCacheDao.getTotalSteps()?.toLong() ?: 0L
    }

    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
        )
    }
}
