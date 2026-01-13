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
import com.ai_health.assistant.data.cache.HealthCacheEntity
import com.ai_health.assistant.data.cache.AppDatabase
import kotlinx.coroutines.flow.first

/**
 * Data class representing a collection of health data retrieved from Health Connect.
 *
 * @property heartRates List of heart rate records.
 * @property steps List of step count records.
 * @property exercises List of exercise session records.
 * @property calories List of total calories burned records.
 */
data class HealthDataResult(
    val heartRates: List<HeartRateRecord>,
    val steps: List<StepsRecord>,
    val exercises: List<ExerciseSessionRecord>,
    val calories: List<TotalCaloriesBurnedRecord>
)

/**
 * Manager class responsible for interacting with the Health Connect API.
 * It handles permission checks, data reading, and caching retrieved data into the local database.
 *
 * @property context The application context used to initialize the Health Connect client and database.
 */
class HealthConnectManager(private val context: Context) {
    
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val TAG = "HealthManager"

    /**
     * Checks if all required health permissions are granted.
     *
     * @return True if all permissions defined in [permissions] are granted, false otherwise.
     */
    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads the total number of steps recorded for the current day.
     *
     * @return The total step count for today, or 0 if an error occurs.
     */
    suspend fun readStepsToday(): Long {
        try {
            val startTime = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
            val endTime = Instant.now()
            
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            
            return response.records.sumOf { it.count }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    /**
     * Reads health data (heart rate, steps, exercises, calories) for the last 7 days.
     * The retrieved data is automatically saved to the local cache.
     *
     * @return A [HealthDataResult] object containing the data, or null if the operation fails.
     */
    suspend fun readAllData(): HealthDataResult? {
        try {
            val startTime = Instant.now().minus(7, ChronoUnit.DAYS)
            val endTime = Instant.now()
            
            val heartResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val stepsResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val exerciseResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val caloriesResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val result = HealthDataResult(
                heartRates = heartResponse.records,
                steps = stepsResponse.records,
                exercises = exerciseResponse.records,
                calories = caloriesResponse.records
            )

            // ✅ Saving to cache
            saveToCache(result)

            return result

        } catch (e: Exception) {
            Log.e(TAG, "Error while reading data", e)
            return null
        }
    }

    /**
     * Saves the retrieved health data into the local Room database.
     * It clears the existing cache before inserting new records.
     *
     * @param healthData The data to be cached.
     */
    private suspend fun saveToCache(healthData: HealthDataResult) {
        val listToSave = mutableListOf<HealthCacheEntity>()
        val dao = AppDatabase.getDatabase(context).healthCacheDao()

        Log.d("RoomDebug", "🧹 Cleaning cache...")
        dao.clearAll()

        // 👣 1. Steps
        healthData.steps.forEach {
            listToSave.add(HealthCacheEntity(
                type = "STEPS",
                value = it.count.toDouble(),
                startTime = it.startTime.toEpochMilli(),
                endTime = it.endTime.toEpochMilli(),
                sourceApp = it.metadata.dataOrigin.packageName
            ))
        }

        // ❤️ 2. Heart Rate
        healthData.heartRates.forEach { record ->
            val avgBpm = record.samples.map { it.beatsPerMinute }.average()
            if (!avgBpm.isNaN()) {
                listToSave.add(HealthCacheEntity(
                    type = "HEART_RATE",
                    value = avgBpm,
                    startTime = record.startTime.toEpochMilli(),
                    endTime = record.endTime.toEpochMilli(),
                    sourceApp = record.metadata.dataOrigin.packageName
                ))
            }
        }

        // 🔥 3. Calories
        healthData.calories.forEach { record ->
            listToSave.add(HealthCacheEntity(
                type = "CALORIES",
                value = record.energy.inKilocalories,
                startTime = record.startTime.toEpochMilli(),
                endTime = record.endTime.toEpochMilli(),
                sourceApp = record.metadata.dataOrigin.packageName
            ))
        }

        // 🏃 4. Exercises
        healthData.exercises.forEach { record ->
            listToSave.add(HealthCacheEntity(
                type = "EXERCISE",
                value = 1.0, // Symbolic value to indicate a session
                startTime = record.startTime.toEpochMilli(),
                endTime = record.endTime.toEpochMilli(),
                sourceApp = record.metadata.dataOrigin.packageName,
                metadata = record.exerciseType.toString() // Saving the exercise type (e.g., RUNNING)
            ))
        }

        if (listToSave.isNotEmpty()) {
            Log.d("RoomDebug", "💾 Inserting ${listToSave.size} records...")
            dao.insertAll(listToSave)
            
            val check = dao.getAllRecords().first()
            Log.d("RoomDebug", "✅ Database updated. Total records: ${check.size}")
        } else {
            Log.d("RoomDebug", "⚠️ No useful data to save.")
        }
    }

    companion object {
        /**
         * Set of required permissions for reading health data.
         */
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        )
    }
}
