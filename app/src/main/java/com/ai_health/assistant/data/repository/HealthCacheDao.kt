package com.ai_health.assistant.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing health data cache in the local database.
 */
@Dao
interface HealthCacheDao {

    /**
     * Inserts a list of health records into the cache.
     * Replaces existing records if there is a conflict.
     *
     * @param records The list of [HealthCacheEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HealthCacheEntity>)

    /**
     * Clears all records from the health cache table.
     * Useful for refreshing data or during re-synchronization.
     */
    @Query("DELETE FROM health_cache")
    suspend fun clearAll()

    /**
     * Retrieves all health records from the cache, ordered by start time descending.
     * Returns a [Flow] to observe changes in the database.
     *
     * @return A [Flow] containing a list of all [HealthCacheEntity].
     */
    @Query("SELECT * FROM health_cache ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<HealthCacheEntity>>

    /**
     * Retrieves health records of a specific type from the cache, ordered by start time descending.
     *
     * @param dataType The type of data to retrieve (e.g., "STEPS").
     * @return A list of [HealthCacheEntity] matching the specified type.
     */
    @Query("SELECT * FROM health_cache WHERE type = :dataType ORDER BY startTime DESC")
    suspend fun getRecordsByType(dataType: String): List<HealthCacheEntity>

    /**
     * Calculates the total sum of steps grouped by the source application.
     *
     * @return A list of [SourceStat] containing the source app name and total steps.
     */
    @Query("SELECT sourceApp, SUM(value) as total FROM health_cache WHERE type = 'STEPS' GROUP BY sourceApp")
    suspend fun getStepsGroupedBySource(): List<SourceStat>


    @Query("SELECT SUM(value) FROM health_cache WHERE type = 'STEPS'")
    suspend fun getTotalSteps(): Double?

    /**
     * Gets the latest end time for a specific data type.
     *
     * @param dataType The type of data (e.g., "STEPS").
     * @return The maximum endTime in epoch milliseconds, or null if no records exist.
     */
    @Query("SELECT MAX(endTime) FROM health_cache WHERE type = :dataType")
    suspend fun getLastSyncTime(dataType: String): Long?
}

/**
 * Data class representing statistical information for a specific data source.
 *
 * @property sourceApp The name of the application that provided the data.
 * @property total The aggregated sum of values for this source.
 */
data class SourceStat(val sourceApp: String, val total: Double)
