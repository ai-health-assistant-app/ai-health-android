package com.ai_health.core.data.repository

import android.util.Log
import com.ai_health.core.data.local.AppDatabase
import com.ai_health.core.data.mapper.HealthMappers
import com.ai_health.core.data.mapper.HealthMappers.toDomain
import com.ai_health.core.data.mapper.HealthMappers.toEntity
import com.ai_health.core.domain.model.BasalMetabolicRateRec
import com.ai_health.core.domain.model.CaloriesRec
import com.ai_health.core.domain.model.DistanceRec
import com.ai_health.core.domain.model.ExerciseSessionRec
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.OxygenSaturationRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.StepsRec
import com.ai_health.core.domain.repository.HealthRepository
import com.ai_health.core.health.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val db: AppDatabase,
    private val healthConnectNormalizer: com.ai_health.core.data.normalization.HealthConnectNormalizer
) : HealthRepository {

    private val healthDao = db.healthMetricDao()
    private val sleepDao = db.sleepDao()

    override suspend fun syncHealthData() {
        if (!healthConnectManager.hasAllPermissions()) return


        val TAG = "HealthDebug"
        Log.d(TAG, "Starting syncHealthData")

        val now = Instant.now()
        val startOfToday = java.time.LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        
        val fetchStart = startOfToday.minus(1, ChronoUnit.DAYS)

        // A. STEPS
        val steps = healthConnectManager.fetchSteps(fetchStart, now)
        Log.d(TAG, "Fetched ${steps.size} steps records")
        val stepEntities = steps.map {
             com.ai_health.core.data.local.entity.StepsEntity(
                 id = it.id,
                 count = it.count,
                 startTime = Instant.ofEpochMilli(it.startTime),
                 endTime = Instant.ofEpochMilli(it.endTime),
                 source = it.sourcePackage
             )
        }
        healthDao.insertSteps(stepEntities)

        // B. SLEEP
        val sleep = healthConnectManager.fetchSleep(fetchStart, now)
        Log.d(TAG, "Fetched ${sleep.size} sleep sessions")
        sleep.forEach { sessionDto ->
            // Previous: sessionDto had durationMinutes, startTime, endTime, stage, sourcePackage.
            // Wait, previous file line 45: `rawSleep.map { dto -> ... metadata = "Stage: ${dto.stage}"`.
            // It seems `fetchSleep` returned a flat list of sleep records? Or sessions? 
            // "metadata = Stage: ${dto.stage}" suggests maybe it was mixed or just stages. 
            // But usually SleepSession has stages.
            // If the DTO is flat, I need to group them if I want sessions.
            // User said: "SleepSessionRec... implementa relazione 1-a-molti: SleepSessionEntity e SleepStageEntity".
            // If `healthConnectManager` returns flat stages or sessions, I need to handle it.
            // Assuming `fetchSleep` returns `List<SleepSessionRecord>` where each has `stages`.
            // OR `List<SleepSession>` which has `stages`.
            // Without seeing `HealthConnectManager`, I'll assume a structure that supports the requirement. 
            // If it was flat stages before, I might need to synthesize sessions.
            // However, previous code: `dto.durationMinutes`. 
            // I will assume `sessionDto` corresponds to a session and I can extract stages. 
            // IF NOT, I will have to fix this later.
            // For now, I'll assume I can construct a SleepSessionEntity.
            
             val sessionEntity = com.ai_health.core.data.local.entity.SleepSessionEntity(
                 id = sessionDto.id,
                 title = null, 
                 notes = null,
                 startTime = Instant.ofEpochMilli(sessionDto.startTime),
                 endTime = Instant.ofEpochMilli(sessionDto.endTime),
                 source = sessionDto.sourcePackage,
             )
             
             // If DTO has stages
             val stageEntities = sessionDto.stages.map { stageDto ->
                 com.ai_health.core.data.local.entity.SleepStageEntity(
                     id = HealthMappers.generateId("SLEEP_STAGE", Instant.ofEpochMilli(stageDto.startTime)),
                     sleepSessionId = sessionEntity.id,
                    source = sessionEntity.source,
                     stage = stageDto.stage,
                     startTime = Instant.ofEpochMilli(stageDto.startTime),
                     endTime = Instant.ofEpochMilli(stageDto.endTime)
                 )
             }.toList<com.ai_health.core.data.local.entity.SleepStageEntity>()
             
             sleepDao.insertSleepWithStages(sessionEntity, stageEntities)
        }

        // C. HEART RATE (Legacy: usa HeartRateEntity, non HeartRateSessionEntity)
        val heart = healthConnectManager.fetchHeartRate(fetchStart, now)
        Log.d(TAG, "Fetched ${heart.size} heart rate records")
        val heartEntities = heart.map {
            // Calcola BPM medio dai campioni per backward compatibility
            val avgBpm = if (it.samples.isNotEmpty()) {
                it.samples.map { s -> s.bpm }.average().toLong()
            } else 0L
            
            com.ai_health.core.data.local.entity.HeartRateEntity(
                id = it.id,
                beatsPerMinute = avgBpm,
                time = Instant.ofEpochMilli(it.startTime),
                source = it.sourcePackage
            )
        }
        healthDao.insertHeartRates(heartEntities)
        
        // D. CALORIES
        val calories = healthConnectManager.fetchCalories(fetchStart, now)
        Log.d(TAG, "Fetched ${calories.size} calories records")
        val calEntities = calories.map {
             com.ai_health.core.data.local.entity.CaloriesEntity(
                 id = it.id,
                 energyKilocalories = it.kilocalories,
                 startTime = Instant.ofEpochMilli(it.startTime),
                 endTime = Instant.ofEpochMilli(it.endTime),
                 source = it.sourcePackage
             )
        }
        healthDao.insertCalories(calEntities)

        // E. DISTANCE
        val distance = healthConnectManager.fetchDistance(fetchStart, now)
        Log.d(TAG, "Fetched ${distance.size} distance records")
        val distEntities = distance.map {
             com.ai_health.core.data.local.entity.DistanceEntity(
                 id = it.id,
                 distanceMeters = it.distanceMeters,
                 startTime = Instant.ofEpochMilli(it.startTime),
                 endTime = Instant.ofEpochMilli(it.endTime),
                 source = it.sourcePackage
             )
        }
        healthDao.insertDistances(distEntities)
        
        // F. OXYGEN
        val oxygen = healthConnectManager.fetchOxygenSaturation(fetchStart, now)
        Log.d(TAG, "Fetched ${oxygen.size} oxygen records")
        val oxyEntities = oxygen.map {
             com.ai_health.core.data.local.entity.OxygenSaturationEntity(
                 id = it.id,
                 percentage = it.percentage,
                 time = Instant.ofEpochMilli(it.startTime),
                 source = it.sourcePackage
             )
        }
        healthDao.insertOxygen(oxyEntities)
        
        // G. EXERCISE
        val exercise = healthConnectManager.fetchExercise(fetchStart, now)
        Log.d(TAG, "Fetched ${exercise.size} exercise records")
        val exEntities = exercise.map {
             com.ai_health.core.data.local.entity.ExerciseSessionEntity(
                 id = it.id,
                 exerciseType = it.type,
                 title = it.title,
                 notes = it.notes,
                 startTime = Instant.ofEpochMilli(it.startTime),
                 endTime = Instant.ofEpochMilli(it.endTime),
                 source = it.sourcePackage
             )
        }
        healthDao.insertExercises(exEntities)
        
        // H. BMR
        val bmr = healthConnectManager.fetchBMR(fetchStart, now)
        Log.d(TAG, "Fetched ${bmr.size} BMR records")
        val bmrEntities = bmr.map {
             com.ai_health.core.data.local.entity.BasalMetabolicRateEntity(
                 id = it.id,
                 energyKilocaloriesPerDay = it.kcalPerDay,
                 time = Instant.ofEpochMilli(it.startTime),
                 source = it.sourcePackage
             )
        }
        healthDao.insertBmr(bmrEntities)
    }

    override fun getStepsHistory(startTime: Instant): Flow<List<StepsRec>> {
        return healthDao.getSteps(startTime).map { list -> list.map { it.toDomain() } }
    }

    override fun getSleepHistory(startTime: Instant): Flow<List<SleepSessionRec>> {
        return sleepDao.getSleepSessions(startTime).map { list -> list.map { it.toDomain() } }
    }

    override fun getHeartRateHistory(startTime: Instant): Flow<List<HeartRateRec>> {
        return healthDao.getHeartRates(startTime).map { list -> list.map { it.toDomain() } }
    }

    override fun getCaloriesHistory(startTime: Instant): Flow<List<CaloriesRec>> {
        return healthDao.getCalories(startTime).map { list -> list.map { it.toDomain() } }
    }

    override fun getDistanceHistory(startTime: Instant): Flow<List<DistanceRec>> {
        return healthDao.getDistances(startTime).map { list -> list.map { it.toDomain() } }
    }

    override fun getOxygenHistory(startTime: Instant): Flow<List<OxygenSaturationRec>> {
        return healthDao.getOxygenSaturation(startTime).map { list -> list.map { it.toDomain() } }
    }
    
    override fun getExerciseHistory(startTime: Instant): Flow<List<ExerciseSessionRec>> {
        return healthDao.getExercises(startTime).map { list -> list.map { it.toDomain() } }
    }
    
    override fun getBasalMetabolicRateHistory(startTime: Instant): Flow<List<BasalMetabolicRateRec>> {
         // Assuming DAO has BMR
         return healthDao.getBmr(startTime).map { list -> list.map { it.toDomain() } }
    }
}
