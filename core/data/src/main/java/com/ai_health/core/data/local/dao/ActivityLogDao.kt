package com.ai_health.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ai_health.core.data.local.entity.UserActivityEntity
import java.time.Instant

@Dao
interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(activity: UserActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLogs(activities: List<UserActivityEntity>)

    // Fetch activities within a range.
    // We fetch a bit more context if needed, but strict range is fine for now.
    @Query("""
        SELECT * FROM user_activity_log 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """)
    suspend fun getActivitiesInRange(startTime: Instant, endTime: Instant): List<UserActivityEntity>
}
