package com.ai_health.core.data.mapper

import com.ai_health.core.data.local.entity.BasalMetabolicRateEntity
import com.ai_health.core.data.local.entity.CaloriesEntity
import com.ai_health.core.data.local.entity.DistanceEntity
import com.ai_health.core.data.local.entity.ExerciseSessionEntity
import com.ai_health.core.data.local.entity.HeartRateEntity
import com.ai_health.core.data.local.entity.OxygenSaturationEntity
import com.ai_health.core.data.local.entity.SleepSessionEntity
import com.ai_health.core.data.local.entity.SleepSessionWithStages
import com.ai_health.core.data.local.entity.SleepStageEntity
import com.ai_health.core.data.local.entity.StepsEntity
import com.ai_health.core.domain.model.BasalMetabolicRateRec
import com.ai_health.core.domain.model.CaloriesRec
import com.ai_health.core.domain.model.DistanceRec
import com.ai_health.core.domain.model.ExerciseSessionRec
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.OxygenSaturationRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.SleepStageRec
import com.ai_health.core.domain.model.StepsRec
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

object HealthMappers {

    // --- Deterministic ID Generation ---
    fun generateId(type: String, time: Instant): String {
        return generateHash(type + time.toEpochMilli())
    }

    private fun generateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return UUID.nameUUIDFromBytes(hash).toString()
    }

    // --- Heart Rate ---
    fun HeartRateRec.toEntity(): HeartRateEntity {
        return HeartRateEntity(
            id = generateId("HEART_RATE", this.time),
            beatsPerMinute = this.beatsPerMinute,
            time = this.time
        )
    }

    fun HeartRateEntity.toDomain(): HeartRateRec {
        return HeartRateRec(
            beatsPerMinute = this.beatsPerMinute,
            time = this.time
        )
    }

    // --- Steps ---
    fun StepsRec.toEntity(): StepsEntity {
        return StepsEntity(
            id = generateId("STEPS", this.startTime),
            count = this.count,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    fun StepsEntity.toDomain(): StepsRec {
        return StepsRec(
            count = this.count,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Calories ---
    fun CaloriesRec.toEntity(): CaloriesEntity {
        return CaloriesEntity(
            id = generateId("CALORIES", this.startTime),
            energyKilocalories = this.energyKilocalories,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    fun CaloriesEntity.toDomain(): CaloriesRec {
        return CaloriesRec(
            energyKilocalories = this.energyKilocalories,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    // --- Distance ---
    fun DistanceRec.toEntity(): DistanceEntity {
        return DistanceEntity(
            id = generateId("DISTANCE", this.startTime),
            distanceMeters = this.distanceMeters,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    fun DistanceEntity.toDomain(): DistanceRec {
        return DistanceRec(
            distanceMeters = this.distanceMeters,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    // --- Oxygen ---
    fun OxygenSaturationRec.toEntity(): OxygenSaturationEntity {
        return OxygenSaturationEntity(
            id = generateId("OXYGEN", this.time),
            percentage = this.percentage,
            time = this.time
        )
    }

    fun OxygenSaturationEntity.toDomain(): OxygenSaturationRec {
        return OxygenSaturationRec(
            percentage = this.percentage,
            time = this.time
        )
    }

    // --- Exercise ---
    fun ExerciseSessionRec.toEntity(): ExerciseSessionEntity {
        return ExerciseSessionEntity(
            id = generateId("EXERCISE", this.startTime),
            exerciseType = this.exerciseType,
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    fun ExerciseSessionEntity.toDomain(): ExerciseSessionRec {
        return ExerciseSessionRec(
            exerciseType = this.exerciseType,
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    // --- Sleep ---
    fun SleepSessionRec.toEntity(): SleepSessionEntity {
        return SleepSessionEntity(
            id = generateId("SLEEP_SESSION", this.startTime),
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    fun SleepStageRec.toEntity(sessionId: String): SleepStageEntity {
        return SleepStageEntity(
            id = generateId("SLEEP_STAGE", this.startTime), // Unique per stage time
            sleepSessionId = sessionId,
            stage = this.stage,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    fun SleepSessionWithStages.toDomain(): SleepSessionRec {
        return SleepSessionRec(
            title = this.session.title,
            notes = this.session.notes,
            startTime = this.session.startTime,
            endTime = this.session.endTime,
            stages = this.stages.map { it.toDomain() }
        )
    }

    fun SleepStageEntity.toDomain(): SleepStageRec {
        return SleepStageRec(
            stage = this.stage,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }
}
