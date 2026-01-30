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
    // --- Deterministic ID Generation ---
    fun generateId(type: String, time: Instant, source: String? = null): String {
        val input = if (source != null) {
            type + time.toEpochMilli() + source
        } else {
            type + time.toEpochMilli()
        }
        return generateHash(input)
    }

    private fun generateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return UUID.nameUUIDFromBytes(hash).toString()
    }

    // --- Heart Rate ---
    fun HeartRateRec.toEntity(): HeartRateEntity {
        return HeartRateEntity(
            id = generateId("HEART_RATE", this.time, this.source),
            beatsPerMinute = this.beatsPerMinute,
            time = this.time,
            source = this.source
        )
    }

    fun HeartRateEntity.toDomain(): HeartRateRec {
        return HeartRateRec(
            beatsPerMinute = this.beatsPerMinute,
            time = this.time,
            source = this.source
        )
    }

    // --- Steps ---
    fun StepsRec.toEntity(): StepsEntity {
        return StepsEntity(
            id = generateId("STEPS", this.startTime, this.source),
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
            id = generateId("CALORIES", this.startTime, this.source),
            energyKilocalories = this.energyKilocalories,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    fun CaloriesEntity.toDomain(): CaloriesRec {
        return CaloriesRec(
            energyKilocalories = this.energyKilocalories,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Distance ---
    fun DistanceRec.toEntity(): DistanceEntity {
        return DistanceEntity(
            id = generateId("DISTANCE", this.startTime, this.source),
            distanceMeters = this.distanceMeters,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    fun DistanceEntity.toDomain(): DistanceRec {
        return DistanceRec(
            distanceMeters = this.distanceMeters,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Oxygen ---
    fun OxygenSaturationRec.toEntity(): OxygenSaturationEntity {
        return OxygenSaturationEntity(
            id = generateId("OXYGEN", this.time, this.source),
            percentage = this.percentage,
            time = this.time,
            source = this.source
        )
    }

    fun OxygenSaturationEntity.toDomain(): OxygenSaturationRec {
        return OxygenSaturationRec(
            percentage = this.percentage,
            time = this.time,
            source = this.source
        )
    }

    // --- Exercise ---
    fun ExerciseSessionRec.toEntity(): ExerciseSessionEntity {
        return ExerciseSessionEntity(
            id = generateId("EXERCISE", this.startTime, this.source),
            exerciseType = this.exerciseType,
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    fun ExerciseSessionEntity.toDomain(): ExerciseSessionRec {
        return ExerciseSessionRec(
            exerciseType = this.exerciseType,
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Sleep ---
    fun SleepSessionRec.toEntity(): SleepSessionEntity {
        return SleepSessionEntity(
            id = generateId("SLEEP_SESSION", this.startTime, this.source),
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
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
            stages = this.stages.map { it.toDomain() },
            source = this.session.source
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
