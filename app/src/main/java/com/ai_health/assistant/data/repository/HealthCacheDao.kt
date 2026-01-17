package com.ai_health.assistant.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HealthCacheEntity>)

    @Query("DELETE FROM health_cache")
    suspend fun clearAll()

    @Query("SELECT * FROM health_cache ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<HealthCacheEntity>>

    @Query("SELECT * FROM health_cache WHERE type = :dataType ORDER BY startTime DESC")
    suspend fun getRecordsByType(dataType: String): List<HealthCacheEntity>

    @Query("SELECT * FROM health_cache WHERE type = :dataType AND startTime >= :fromTime ORDER BY startTime ASC")
    suspend fun getRecordsByTime(dataType: String, fromTime: Long): List<HealthCacheEntity>

    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'STEPS' AND startTime >= :fromTime")
    suspend fun getTotalSteps(fromTime: Long): Double?

    @Query("SELECT sourceApp, SUM(value) as total FROM health_cache WHERE type = 'STEPS' AND startTime >= :fromTime GROUP BY sourceApp")
    suspend fun getStepsBySource(fromTime: Long): List<SourceStat>

    @Query("SELECT MAX(endTime) FROM health_cache WHERE type = :dataType")
    suspend fun getLastSyncTime(dataType: String): Long?

    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'SLEEP' AND startTime >= :fromTime")
    suspend fun getTotalSleepMinutes(fromTime: Long): Double?

    @Query("SELECT * FROM health_cache WHERE type = 'SLEEP_STAGE' AND startTime >= :fromTime ORDER BY startTime ASC")
    suspend fun getSleepStages(fromTime: Long): List<HealthCacheEntity>

    @Query("SELECT AVG(value) FROM health_cache WHERE type = 'HEART_RATE' AND startTime >= :fromTime")
    suspend fun getAverageHeartRate(fromTime: Long): Double?

    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'CALORIES' AND startTime >= :fromTime")
    suspend fun getTotalCalories(fromTime: Long): Double?

    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'DISTANCE' AND startTime >= :fromTime")
    suspend fun getTotalDistance(fromTime: Long): Double?

    @Query("SELECT AVG(value) FROM health_cache WHERE type = 'OXYGEN_SATURATION' AND startTime >= :fromTime")
    suspend fun getAverageOxygenSaturation(fromTime: Long): Double?
    
    @Query("SELECT * FROM health_cache WHERE type = 'EXERCISE' AND startTime >= :fromTime ORDER BY startTime DESC")
    suspend fun getRecentExercises(fromTime: Long): List<HealthCacheEntity>
}

data class SourceStat(val sourceApp: String, val total: Double)
