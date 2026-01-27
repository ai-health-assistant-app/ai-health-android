package com.ai_health.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HealthCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HealthCacheEntity>)

    @Query("DELETE FROM health_cache WHERE type = :type")
    suspend fun clearByType(type: String)

    @Query("SELECT * FROM health_cache WHERE startTime >= :startTime ORDER BY startTime ASC")
    suspend fun getAllData(startTime: Long): List<HealthCacheEntity>

    @Query("SELECT * FROM health_cache WHERE type = :type AND startTime >= :startTime ORDER BY startTime ASC")
    suspend fun getRecordsByTime(type: String, startTime: Long): List<HealthCacheEntity>

    // Aggregations
    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'STEPS' AND startTime >= :startTime")
    suspend fun getTotalSteps(startTime: Long): Double?

    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'SLEEP' AND startTime >= :startTime")
    suspend fun getTotalSleepMinutes(startTime: Long): Double?

    @Query("SELECT AVG(value) FROM health_cache WHERE type = 'HEART_RATE' AND startTime >= :startTime")
    suspend fun getAverageHeartRate(startTime: Long): Double?

    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'CALORIES' AND startTime >= :startTime")
    suspend fun getTotalCalories(startTime: Long): Double?

    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'DISTANCE' AND startTime >= :startTime")
    suspend fun getTotalDistance(startTime: Long): Double?

    @Query("SELECT AVG(value) FROM health_cache WHERE type = 'OXYGEN_SATURATION' AND startTime >= :startTime")
    suspend fun getAverageOxygenSaturation(startTime: Long): Double?
}
