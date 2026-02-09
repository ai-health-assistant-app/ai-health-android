package com.ai_health.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ai_health.core.data.local.entity.SleepSessionEntity
import com.ai_health.core.data.local.entity.SleepSessionWithStages
import com.ai_health.core.data.local.entity.SleepStageEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SleepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<SleepStageEntity>)

    @Transaction // Necessary for @Relation
    @Query("SELECT * FROM sleep_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getSleepSessions(startTime: Instant): Flow<List<SleepSessionWithStages>>
    
    // Helper to insert whole aggregate
    @Transaction
    suspend fun insertSleepWithStages(session: SleepSessionEntity, stages: List<SleepStageEntity>) {
        insertSession(session)
        // Ensure the foreign key is satisfied. The Session MUST be inserted first.
        // The stages passed here should already have sleepSessionId set correctly.
        insertStages(stages)
    }

    // PRIVACY COMPLIANCE: Cancellazione fisica per DeletionChange
    // SleepStageEntity ha ForeignKey con onDelete = CASCADE, quindi le stages vengono eliminate automaticamente
    @Query("DELETE FROM sleep_sessions WHERE id = :recordId")
    suspend fun deleteSleepSessionById(recordId: String)
}
