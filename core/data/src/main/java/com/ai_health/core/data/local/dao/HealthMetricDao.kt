package com.ai_health.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ai_health.core.data.local.entity.BasalMetabolicRateEntity
import com.ai_health.core.data.local.entity.CaloriesEntity
import com.ai_health.core.data.local.entity.DistanceEntity
import com.ai_health.core.data.local.entity.ExerciseSessionEntity

import com.ai_health.core.data.local.entity.HeartRateSessionEntity
import com.ai_health.core.data.local.entity.OxygenSaturationEntity
import com.ai_health.core.data.local.entity.StepsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

// Ideally, we could use a BaseDao if we had a common interface, but for simple Room usage with concrete types, separate DAOs or one big DAO is fine.
// User requested "Crea i DAO necessari". I will make one `HealthMetricDao` for simple metrics and a separate `SleepDao`.

@Dao
interface HealthMetricDao {

    // --- HEART RATE (Legacy) ---

    
    // --- HEART RATE SESSION (Optimized for HRV) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRateSessions(entities: List<HeartRateSessionEntity>)
    
    @Query("SELECT * FROM heart_rate_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getHeartRateSessions(startTime: Instant): Flow<List<HeartRateSessionEntity>>
    
    @Query("DELETE FROM heart_rate_sessions WHERE id = :recordId")
    suspend fun deleteHeartRateSessionById(recordId: String)


    // --- STEPS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(entities: List<StepsEntity>)

    @Query("SELECT * FROM steps WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getSteps(startTime: Instant): Flow<List<StepsEntity>>

    @Query("DELETE FROM steps WHERE id = :recordId")
    suspend fun deleteStepsById(recordId: String)

    // --- CALORIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalories(entities: List<CaloriesEntity>)

    @Query("SELECT * FROM calories WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getCalories(startTime: Instant): Flow<List<CaloriesEntity>>

    @Query("DELETE FROM calories WHERE id = :recordId")
    suspend fun deleteCaloriesById(recordId: String)


    // --- DISTANCE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistances(entities: List<DistanceEntity>)

    @Query("SELECT * FROM distance WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getDistances(startTime: Instant): Flow<List<DistanceEntity>>

    @Query("DELETE FROM distance WHERE id = :recordId")
    suspend fun deleteDistanceById(recordId: String)


    // --- OXYGEN ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOxygen(entities: List<OxygenSaturationEntity>)

    @Query("SELECT * FROM oxygen_saturation WHERE time >= :startTime ORDER BY time DESC")
    fun getOxygenSaturation(startTime: Instant): Flow<List<OxygenSaturationEntity>>

    @Query("DELETE FROM oxygen_saturation WHERE id = :recordId")
    suspend fun deleteOxygenById(recordId: String)
    
    // --- BMR ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBmr(entities: List<BasalMetabolicRateEntity>)

    @Query("SELECT * FROM basal_metabolic_rate WHERE time >= :startTime ORDER BY time DESC")
    fun getBmr(startTime: Instant): Flow<List<BasalMetabolicRateEntity>>

    @Query("DELETE FROM basal_metabolic_rate WHERE id = :recordId")
    suspend fun deleteBmrById(recordId: String)

    // --- EXERCISE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(entities: List<ExerciseSessionEntity>)

    @Query("SELECT * FROM exercise_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getExercises(startTime: Instant): Flow<List<ExerciseSessionEntity>>

    @Query("DELETE FROM exercise_sessions WHERE id = :recordId")
    suspend fun deleteExerciseById(recordId: String)
}
