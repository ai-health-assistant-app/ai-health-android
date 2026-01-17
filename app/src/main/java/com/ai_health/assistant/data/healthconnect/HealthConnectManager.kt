package com.ai_health.assistant.data.healthconnect

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import android.util.Log
import com.ai_health.assistant.data.repository.HealthCacheEntity
import com.ai_health.assistant.data.repository.AppDatabase
import com.ai_health.assistant.domain.model.*
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {
    
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val TAG = "HealthManager"
    private val db by lazy { AppDatabase.getDatabase(context) }
    private val healthCacheDao by lazy { db.healthCacheDao() }

    private val normalizer = HealthConnectNormalizer()

    private val DEFAULT_HISTORY_DAYS = 1L

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Trova il pacchetto dell'app wearable più recente che ha inviato dati a Health Connect.
     * È agnostico perché controlla il tipo di dispositivo nei metadati (WATCH/BAND).
     */
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
            Toast.makeText(context, "App companion non trovata", Toast.LENGTH_SHORT).show()
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

    suspend fun syncSteps(): List<HealthCacheEntity> {
        try {
            val startOfToday = Instant.ofEpochMilli(getStartOfToday())
            val lastSyncTime = healthCacheDao.getLastSyncTime("STEPS")
            val startTime = lastSyncTime?.let { 
                val lastSyncInstant = Instant.ofEpochMilli(it).minusSeconds(3600)
                if (lastSyncInstant.isBefore(startOfToday)) startOfToday else lastSyncInstant
            } ?: startOfToday
            val endTime = Instant.now()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startTime, endTime))
            )

            val normalizedEntities = normalizer.normalizeSteps(response.records)
            if (normalizedEntities.isNotEmpty()) {
                healthCacheDao.insertAll(normalizedEntities)
            }
            return normalizedEntities
        } catch (e: Exception) {
            Log.e(TAG, "Error syncSteps", e)
            return emptyList()
        }
    }

    suspend fun syncHeartRate(): List<HealthCacheEntity> {
        try {
            val startTime = ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(startTime, endTime)))
            
            val normalized = normalizer.normalizeHeartRate(response.records)
            if (normalized.isNotEmpty()) healthCacheDao.insertAll(normalized)
            return normalized
        } catch (e: Exception) {
            Log.e(TAG, "Error syncHeartRate", e)
            return emptyList()
        }
    }

    suspend fun syncExercise(): List<HealthCacheEntity> {
        try {
            val startTime = ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(startTime, endTime)))
            
            val normalized = normalizer.normalizeExercise(response.records)
            if (normalized.isNotEmpty()) healthCacheDao.insertAll(normalized)
            return normalized
        } catch (e: Exception) {
            Log.e(TAG, "Error syncExercise", e)
            return emptyList()
        }
    }

    suspend fun syncOxygenSaturation(): List<HealthCacheEntity> {
        try {
            val startTime = ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(startTime, endTime)))
            
            val normalized = normalizer.normalizeOxygen(response.records)
            if (normalized.isNotEmpty()) healthCacheDao.insertAll(normalized)
            return normalized
        } catch (e: Exception) {
            Log.e(TAG, "Error syncOxygen", e)
            return emptyList()
        }
    }

    suspend fun syncDistance(): List<HealthCacheEntity> {
        try {
            val startTime = ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(DistanceRecord::class, TimeRangeFilter.between(startTime, endTime)))
            
            val normalized = normalizer.normalizeDistance(response.records)
            if (normalized.isNotEmpty()) healthCacheDao.insertAll(normalized)
            return normalized
        } catch (e: Exception) {
            Log.e(TAG, "Error syncDistance", e)
            return emptyList()
        }
    }

    suspend fun syncSleep(): List<HealthCacheEntity> {
        try {
            val startTime = ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(startTime, endTime)))
            
            val normalized = normalizer.normalizeSleep(response.records)
            if (normalized.isNotEmpty()) healthCacheDao.insertAll(normalized)
            return normalized
        } catch (e: Exception) {
            Log.e(TAG, "Error syncSleep", e)
            return emptyList()
        }
    }

    suspend fun syncCalories(): List<HealthCacheEntity> {
        try {
            val startTime = ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(startTime, endTime)))
            
            val normalized = normalizer.normalizeCalories(response.records)
            if (normalized.isNotEmpty()) healthCacheDao.insertAll(normalized)
            return normalized
        } catch (e: Exception) {
            Log.e(TAG, "Error syncCalories", e)
            return emptyList()
        }
    }

    suspend fun syncBasalMetabolicRate(): List<HealthCacheEntity> {
        try {
            val startTime = ZonedDateTime.now().minus(DEFAULT_HISTORY_DAYS, ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            val response = healthConnectClient.readRecords(ReadRecordsRequest(BasalMetabolicRateRecord::class, TimeRangeFilter.between(startTime, endTime)))
            
            val normalized = normalizer.normalizeBMR(response.records)
            if (normalized.isNotEmpty()) healthCacheDao.insertAll(normalized)
            return normalized
        } catch (e: Exception) {
            Log.e(TAG, "Error syncBMR", e)
            return emptyList()
        }
    }

    private fun getStartOfToday(): Long {
        return ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    suspend fun getTotalStepsFromCache() = healthCacheDao.getTotalSteps(getStartOfToday()) ?: 0.0
    suspend fun getStepsBySourceFromCache() = healthCacheDao.getStepsBySource(getStartOfToday())
    suspend fun getTotalSleepFromCache() = healthCacheDao.getTotalSleepMinutes(getStartOfToday())?.toInt() ?: 0
    suspend fun getSleepStagesFromCache() = healthCacheDao.getSleepStages(getStartOfToday())
    suspend fun getAvgHeartRateFromCache() = healthCacheDao.getAverageHeartRate(getStartOfToday()) ?: 0.0
    suspend fun getTotalCaloriesFromCache() = healthCacheDao.getTotalCalories(getStartOfToday()) ?: 0.0
    suspend fun getTotalDistanceFromCache() = healthCacheDao.getTotalDistance(getStartOfToday()) ?: 0.0
    suspend fun getAvgOxygenFromCache() = healthCacheDao.getAverageOxygenSaturation(getStartOfToday()) ?: 0.0
    suspend fun getRecentExercisesFromCache() = healthCacheDao.getRecentExercises(getStartOfToday())

    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
        )
    }
}
