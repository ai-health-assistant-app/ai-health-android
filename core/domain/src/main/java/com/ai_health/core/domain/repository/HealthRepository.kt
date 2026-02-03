package com.ai_health.core.domain.repository

import com.ai_health.core.domain.model.BasalMetabolicRateRec
import com.ai_health.core.domain.model.CaloriesRec
import com.ai_health.core.domain.model.DistanceRec
import com.ai_health.core.domain.model.ExerciseSessionRec
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.OxygenSaturationRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.StepsRec
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface HealthRepository {
    suspend fun syncHealthData()

    fun getHeartRateHistory(startTime: Instant): Flow<List<HeartRateRec>>
    fun getStepsHistory(startTime: Instant): Flow<List<StepsRec>>
    fun getSleepHistory(startTime: Instant): Flow<List<SleepSessionRec>>
    fun getCaloriesHistory(startTime: Instant): Flow<List<CaloriesRec>>
    fun getDistanceHistory(startTime: Instant): Flow<List<DistanceRec>>
    fun getOxygenHistory(startTime: Instant): Flow<List<OxygenSaturationRec>>
    fun getExerciseHistory(startTime: Instant): Flow<List<ExerciseSessionRec>>
    fun getBasalMetabolicRateHistory(startTime: Instant): Flow<List<BasalMetabolicRateRec>>

    fun getLastSyncInfo(): Flow<Pair<Instant?, String?>>
}
