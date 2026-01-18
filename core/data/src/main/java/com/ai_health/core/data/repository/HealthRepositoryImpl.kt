package com.ai_health.core.data.repository

import com.ai_health.core.data.local.AppDatabase
import com.ai_health.core.data.local.HealthCacheEntity
import com.ai_health.core.domain.repository.HealthRepository
import com.ai_health.core.health.HealthConnectManager
import javax.inject.Inject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val db: AppDatabase,
    private val healthConnectNormalizer: com.ai_health.core.data.normalization.HealthConnectNormalizer
) : HealthRepository {

    private val dao = db.healthCacheDao()

    override suspend fun syncHealthData() {
        // 1. Check permissions
        if (!healthConnectManager.hasAllPermissions()) {
            return
        }

        // 2. Define time range
        val startOfToday = getStartOfToday()
        val startInstant = Instant.ofEpochMilli(startOfToday)
        val now = Instant.now()
        val startOfYesterday = now.minus(1, ChronoUnit.DAYS)

        // --- PHASE 1: FETCH & SAVE ---

        // A. STEPS
        val rawSteps = healthConnectManager.fetchSteps(startInstant, now)
        if (rawSteps.isNotEmpty()) {
            val normalizedSteps = healthConnectNormalizer.normalizeSteps(rawSteps)
            dao.insertAll(normalizedSteps)
        }

        // B. SLEEP
        val rawSleep = healthConnectManager.fetchSleep(startOfYesterday, now)
        if (rawSleep.isNotEmpty()) {
            dao.insertAll(rawSleep.map { dto ->
                HealthCacheEntity(
                    type = "SLEEP",
                    value = dto.durationMinutes,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    sourceApp = dto.sourcePackage,
                    metadata = "Stage: ${dto.stage}"
                )
            })
        }

        // C. HEART RATE
        val rawHeart = healthConnectManager.fetchHeartRate(startInstant, now)
        if (rawHeart.isNotEmpty()) {
            dao.insertAll(rawHeart.map { dto ->
                HealthCacheEntity(
                    type = "HEART_RATE",
                    value = dto.bpm,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    sourceApp = dto.sourcePackage
                )
            })
        }

        // D. CALORIES
        val rawCalories = healthConnectManager.fetchCalories(startInstant, now)
        if (rawCalories.isNotEmpty()) {
            dao.insertAll(rawCalories.map { dto ->
                HealthCacheEntity(
                    type = "CALORIES",
                    value = dto.kilocalories,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    sourceApp = dto.sourcePackage
                )
            })
        }

        // E. DISTANCE
        val rawDist = healthConnectManager.fetchDistance(startInstant, now)
        if (rawDist.isNotEmpty()) {
            dao.insertAll(rawDist.map { dto ->
                HealthCacheEntity(
                    type = "DISTANCE",
                    value = dto.distanceMeters,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    sourceApp = dto.sourcePackage
                )
            })
        }

        // F. OXYGEN
        val rawOxygen = healthConnectManager.fetchOxygenSaturation(startInstant, now)
        if (rawOxygen.isNotEmpty()) {
            dao.insertAll(rawOxygen.map { dto ->
                HealthCacheEntity(
                    type = "OXYGEN_SATURATION",
                    value = dto.percentage,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    sourceApp = dto.sourcePackage
                )
            })
        }

        // G. EXERCISE
        val rawExercises = healthConnectManager.fetchExercise(startInstant, now)
        if (rawExercises.isNotEmpty()) {
            dao.insertAll(rawExercises.map { dto ->
                HealthCacheEntity(
                    type = "EXERCISE",
                    value = dto.durationMinutes,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    sourceApp = dto.sourcePackage,
                    metadata = "${dto.type}|${dto.title ?: ""}"
                )
            })
        }
    }

    override suspend fun getSteps(startTime: Long): Int {
        return dao.getTotalSteps(startTime)?.toInt() ?: 0
    }

    override suspend fun getSleepMinutes(startTime: Long): Int {
        return dao.getTotalSleepMinutes(startTime)?.toInt() ?: 0
    }

    override suspend fun getAvgHeartRate(startTime: Long): Int {
        return dao.getAverageHeartRate(startTime)?.toInt() ?: 0
    }

    override suspend fun getCalories(startTime: Long): Int {
        return dao.getTotalCalories(startTime)?.toInt() ?: 0
    }

    override suspend fun getDistanceKm(startTime: Long): Double {
        return (dao.getTotalDistance(startTime) ?: 0.0) / 1000.0
    }

    override suspend fun getOxygenSaturation(startTime: Long): Double {
        return dao.getAverageOxygenSaturation(startTime) ?: 0.0
    }

    private fun getStartOfToday(): Long {
        return java.time.LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
