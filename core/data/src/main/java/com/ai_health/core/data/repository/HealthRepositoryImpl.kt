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
import com.ai_health.core.data.local.entity.HeartRateSessionEntity
import com.ai_health.core.data.local.entity.HeartRateSample
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.OxygenSaturationRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.StepsRec
import com.ai_health.core.domain.repository.HealthRepository
import com.ai_health.core.health.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.ai_health.core.data.mapper.HealthMappers.toDomainList
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val db: AppDatabase

) : HealthRepository {

    private val healthDao = db.healthMetricDao()
    private val sleepDao = db.sleepDao()

    override suspend fun syncHealthData() {
        if (!healthConnectManager.hasAllPermissions()) return

        val TAG = "HealthSync"
        Log.d(TAG, "Starting fault-tolerant syncHealthData")

        val now = Instant.now()
        val startOfToday = java.time.LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        
        // --- COLD START LOGIC ---
        // Check if we have any data locally. If not, fetch 30 days.
        // If we have data, fetch from yesterday (to cover late syncs)
        val sleepCount = sleepDao.getSessionCount()
        val stepsCount = healthDao.getStepsCount()
        
        val isColdStart = sleepCount == 0 && stepsCount == 0
        val daysToFetch = if (isColdStart) 30L else 1L
        
        Log.d(TAG, "Sync strategy: ColdStart=$isColdStart (sleep=$sleepCount, steps=$stepsCount). Fetching last $daysToFetch days.")

        val fetchStart = startOfToday.minus(daysToFetch, ChronoUnit.DAYS)

        // --- FAULT-TOLERANT SYNC ---
        // Each data type syncs independently. Failures in one won't block others.
        val results = listOf(
            "Steps" to syncSteps(fetchStart, now),
            "Sleep" to syncSleep(fetchStart, now),
            "HeartRate" to syncHeartRate(fetchStart, now),
            "Calories" to syncCalories(fetchStart, now),
            "Distance" to syncDistance(fetchStart, now),
            "Oxygen" to syncOxygen(fetchStart, now),
            "Exercise" to syncExercise(fetchStart, now),
            "BMR" to syncBMR(fetchStart, now)
        )
        
        // Aggregate results and log
        var successCount = 0
        var failureCount = 0
        
        results.forEach { (dataType, result) ->
            result.fold(
                onSuccess = { count ->
                    successCount++
                    Log.d(TAG, "✓ Synced $count $dataType records")
                },
                onFailure = { e ->
                    failureCount++
                    Log.e(TAG, "✗ Failed to sync $dataType: ${e.message}", e)
                }
            )
        }
        
        Log.i(TAG, "Sync complete: $successCount succeeded, $failureCount failed")
    }

    // --- INDIVIDUAL SYNC METHODS ---
    // Each method is fault-tolerant and returns Result<Int> with count of synced records
    
    private suspend fun syncSteps(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val steps = healthConnectManager.fetchSteps(fetchStart, now)
        val stepEntities = steps.map {
            com.ai_health.core.data.local.entity.StepsEntity(
                id = it.id,
                count = it.count,
                startTime = Instant.ofEpochMilli(it.startTime),
                endTime = Instant.ofEpochMilli(it.endTime),
                source = it.sourcePackage,
                deviceType = it.deviceType
            )
        }
        healthDao.insertSteps(stepEntities)
        stepEntities.size
    }

    private suspend fun syncSleep(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val sleep = healthConnectManager.fetchSleep(fetchStart, now)
        sleep.forEach { sessionDto ->
            val sessionEntity = com.ai_health.core.data.local.entity.SleepSessionEntity(
                id = sessionDto.id,
                title = null,
                notes = null,
                startTime = Instant.ofEpochMilli(sessionDto.startTime),
                endTime = Instant.ofEpochMilli(sessionDto.endTime),
                source = sessionDto.sourcePackage,
                deviceType = sessionDto.deviceType
            )
            
            val stageEntities = sessionDto.stages.map { stageDto ->
                com.ai_health.core.data.local.entity.SleepStageEntity(
                    id = HealthMappers.generateId("SLEEP_STAGE", Instant.ofEpochMilli(stageDto.startTime)),
                    sleepSessionId = sessionEntity.id,
                    source = sessionEntity.source,
                    stage = stageDto.stage,
                    startTime = Instant.ofEpochMilli(stageDto.startTime),
                    endTime = Instant.ofEpochMilli(stageDto.endTime),
                    deviceType = sessionDto.deviceType
                )
            }
            
            sleepDao.insertSleepWithStages(sessionEntity, stageEntities)
        }
        sleep.size
    }

    private suspend fun syncHeartRate(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val heart = healthConnectManager.fetchHeartRate(fetchStart, now)
        val heartEntities = heart.map { raw ->
            val samples = raw.samples.map { 
                HeartRateSample(offsetMs = it.offsetMs, bpm = it.bpm) 
            }
            val samplesJson = Json.encodeToString(samples)
            
            HeartRateSessionEntity(
                id = raw.id,
                source = raw.sourcePackage,
                deviceType = raw.deviceType,
                startTime = Instant.ofEpochMilli(raw.startTime),
                endTime = Instant.ofEpochMilli(raw.endTime),
                samplesJson = samplesJson
            )
        }
        healthDao.insertHeartRateSessions(heartEntities)
        heartEntities.size
    }

    private suspend fun syncCalories(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val calories = healthConnectManager.fetchCalories(fetchStart, now)
        val calEntities = calories.map {
            com.ai_health.core.data.local.entity.CaloriesEntity(
                id = it.id,
                energyKilocalories = it.kilocalories,
                startTime = Instant.ofEpochMilli(it.startTime),
                endTime = Instant.ofEpochMilli(it.endTime),
                source = it.sourcePackage,
                deviceType = it.deviceType
            )
        }
        healthDao.insertCalories(calEntities)
        calEntities.size
    }

    private suspend fun syncDistance(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val distance = healthConnectManager.fetchDistance(fetchStart, now)
        val distEntities = distance.map {
            com.ai_health.core.data.local.entity.DistanceEntity(
                id = it.id,
                distanceMeters = it.distanceMeters,
                startTime = Instant.ofEpochMilli(it.startTime),
                endTime = Instant.ofEpochMilli(it.endTime),
                source = it.sourcePackage,
                deviceType = it.deviceType
            )
        }
        healthDao.insertDistances(distEntities)
        distEntities.size
    }

    private suspend fun syncOxygen(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val oxygen = healthConnectManager.fetchOxygenSaturation(fetchStart, now)
        val oxyEntities = oxygen.map {
            com.ai_health.core.data.local.entity.OxygenSaturationEntity(
                id = it.id,
                percentage = it.percentage,
                time = Instant.ofEpochMilli(it.startTime),
                source = it.sourcePackage,
                deviceType = it.deviceType
            )
        }
        healthDao.insertOxygen(oxyEntities)
        oxyEntities.size
    }

    private suspend fun syncExercise(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val exercise = healthConnectManager.fetchExercise(fetchStart, now)
        val exEntities = exercise.map {
            com.ai_health.core.data.local.entity.ExerciseSessionEntity(
                id = it.id,
                exerciseType = it.type,
                title = it.title,
                notes = it.notes,
                startTime = Instant.ofEpochMilli(it.startTime),
                endTime = Instant.ofEpochMilli(it.endTime),
                source = it.sourcePackage,
                deviceType = it.deviceType
            )
        }
        healthDao.insertExercises(exEntities)
        exEntities.size
    }

    private suspend fun syncBMR(fetchStart: Instant, now: Instant): Result<Int> = runCatching {
        val bmr = healthConnectManager.fetchBMR(fetchStart, now)
        val bmrEntities = bmr.map {
            com.ai_health.core.data.local.entity.BasalMetabolicRateEntity(
                id = it.id,
                energyKilocaloriesPerDay = it.kcalPerDay,
                time = Instant.ofEpochMilli(it.startTime),
                source = it.sourcePackage,
                deviceType = it.deviceType
            )
        }
        healthDao.insertBmr(bmrEntities)
        bmrEntities.size
    }



    override fun getStepsHistory(startTime: Instant): Flow<List<StepsRec>> {
        return healthDao.getSteps(startTime).map { list -> list.map { it.toDomain() } }
    }

    override fun getSleepHistory(startTime: Instant): Flow<List<SleepSessionRec>> {
        return sleepDao.getSleepSessions(startTime).map { list -> list.map { it.toDomain() } }
    }

    override fun getHeartRateHistory(startTime: Instant): Flow<List<HeartRateRec>> {
        return healthDao.getHeartRateSessions(startTime).map { sessions -> 
            sessions.flatMap { it.toDomainList() } 
        }
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
