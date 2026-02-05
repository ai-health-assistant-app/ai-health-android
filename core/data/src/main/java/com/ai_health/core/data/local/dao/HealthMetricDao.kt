package com.ai_health.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ai_health.core.data.local.entity.BasalMetabolicRateEntity
import com.ai_health.core.data.local.entity.CaloriesEntity
import com.ai_health.core.data.local.entity.DistanceEntity
import com.ai_health.core.data.local.entity.ExerciseSessionEntity
import com.ai_health.core.data.local.entity.HeartRateEntity
import com.ai_health.core.data.local.entity.OxygenSaturationEntity
import com.ai_health.core.data.local.entity.StepsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

// Ideally, we could use a BaseDao if we had a common interface, but for simple Room usage with concrete types, separate DAOs or one big DAO is fine.
// User requested "Crea i DAO necessari". I will make one `HealthMetricDao` for simple metrics and a separate `SleepDao`.

@Dao
interface HealthMetricDao {

    // --- HEART RATE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRates(entities: List<HeartRateEntity>)

    @Query("SELECT * FROM heart_rate WHERE time >= :startTime ORDER BY time DESC")
    fun getHeartRates(startTime: Instant): Flow<List<HeartRateEntity>>


    // --- STEPS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(entities: List<StepsEntity>)

    @Query("SELECT * FROM steps WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getSteps(startTime: Instant): Flow<List<StepsEntity>>

    // --- CALORIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalories(entities: List<CaloriesEntity>)

    @Query("SELECT * FROM calories WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getCalories(startTime: Instant): Flow<List<CaloriesEntity>>


    // --- DISTANCE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistances(entities: List<DistanceEntity>)

    @Query("SELECT * FROM distance WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getDistances(startTime: Instant): Flow<List<DistanceEntity>>


    // --- OXYGEN ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOxygen(entities: List<OxygenSaturationEntity>)

    @Query("SELECT * FROM oxygen_saturation WHERE time >= :startTime ORDER BY time DESC")
    fun getOxygenSaturation(startTime: Instant): Flow<List<OxygenSaturationEntity>>
    
    // --- BMR ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBmr(entities: List<BasalMetabolicRateEntity>)

    @Query("SELECT * FROM basal_metabolic_rate WHERE time >= :startTime ORDER BY time DESC")
    fun getBmr(startTime: Instant): Flow<List<BasalMetabolicRateEntity>>

    // --- EXERCISE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(entities: List<ExerciseSessionEntity>)

    @Query("SELECT * FROM exercise_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getExercises(startTime: Instant): Flow<List<ExerciseSessionEntity>>

    // --- HELPER METHODS ---

    @Query("SELECT COUNT(*) FROM steps WHERE startTime >= :start AND startTime < :end")
    suspend fun getStepsCountForIntervall(start: Instant, end: Instant): Int

    @Transaction
    suspend fun deleteRecordById(id: String) {
        deleteStepById(id)
        deleteHeartRateById(id)
        deleteCaloriesById(id)
        deleteDistanceById(id)
        deleteOxygenById(id)
        deleteExerciseById(id)
        deleteBmrById(id)
    }

    @Query("DELETE FROM steps WHERE id = :id")
    suspend fun deleteStepById(id: String)

    @Query("DELETE FROM heart_rate WHERE id = :id")
    suspend fun deleteHeartRateById(id: String)

    @Query("DELETE FROM calories WHERE id = :id")
    suspend fun deleteCaloriesById(id: String)

    @Query("DELETE FROM distance WHERE id = :id")
    suspend fun deleteDistanceById(id: String)

    @Query("DELETE FROM oxygen_saturation WHERE id = :id")
    suspend fun deleteOxygenById(id: String)

    @Query("DELETE FROM exercise_sessions WHERE id = :id")
    suspend fun deleteExerciseById(id: String)

    @Query("DELETE FROM basal_metabolic_rate WHERE id = :id")
    suspend fun deleteBmrById(id: String)
}
