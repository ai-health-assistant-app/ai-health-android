package com.ai_health.core.data.mapper

import com.ai_health.core.data.local.entity.BasalMetabolicRateEntity
import com.ai_health.core.data.local.entity.CaloriesEntity
import com.ai_health.core.data.local.entity.DistanceEntity
import com.ai_health.core.data.local.entity.ExerciseSessionEntity
import com.ai_health.core.data.local.entity.HeartRateSessionEntity
import com.ai_health.core.data.local.entity.HeartRateSample
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
import kotlinx.serialization.json.Json

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
    // NO toEntity() needed for domain->entity in this direction for now, 
    // as we usually sync FROM Health Connect TO DB (entity).
    // If we need to save manual HR, we'd need a different approach or create a session.
    
    fun HeartRateSessionEntity.toDomainList(): List<HeartRateRec> {
        return try {
            val samples = Json.decodeFromString<List<HeartRateSample>>(this.samplesJson)
            samples.map { sample ->
                HeartRateRec(
                    // Generate unique ID for each point based on session ID and offset
                    id = "${this.id}_${sample.offsetMs}",
                    beatsPerMinute = sample.bpm.toLong(),
                    time = this.startTime.plusMillis(sample.offsetMs.toLong()),
                    source = this.source
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Steps ---
    fun StepsRec.toEntity(): StepsEntity {
        return StepsEntity(
            id = this.id,
            count = this.count,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source,
            deviceType = null
        )
    }

    fun StepsEntity.toDomain(): StepsRec {
        return StepsRec(
            id = this.id,
            count = this.count,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Calories ---
    fun CaloriesRec.toEntity(): CaloriesEntity {
        return CaloriesEntity(
            id = this.id,
            energyKilocalories = this.energyKilocalories,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source,
            deviceType = null
        )
    }

    fun CaloriesEntity.toDomain(): CaloriesRec {
        return CaloriesRec(
            id = this.id,
            energyKilocalories = this.energyKilocalories,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Distance ---
    fun DistanceRec.toEntity(): DistanceEntity {
        return DistanceEntity(
            id = this.id,
            distanceMeters = this.distanceMeters,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source,
            deviceType = null
        )
    }

    fun DistanceEntity.toDomain(): DistanceRec {
        return DistanceRec(
            id = this.id,
            distanceMeters = this.distanceMeters,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Oxygen ---
    fun OxygenSaturationRec.toEntity(): OxygenSaturationEntity {
        return OxygenSaturationEntity(
            id = this.id,
            percentage = this.percentage,
            time = this.time,
            source = this.source,
            deviceType = null
        )
    }

    fun OxygenSaturationEntity.toDomain(): OxygenSaturationRec {
        return OxygenSaturationRec(
            id = this.id,
            percentage = this.percentage,
            time = this.time,
            source = this.source
        )
    }

    // --- Exercise ---
    fun ExerciseSessionRec.toEntity(): ExerciseSessionEntity {
        return ExerciseSessionEntity(
            id = this.id,
            exerciseType = this.exerciseType,
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source,
            deviceType = null
        )
    }

    fun ExerciseSessionEntity.toDomain(): ExerciseSessionRec {
        return ExerciseSessionRec(
            id = this.id,
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
            id = this.id,
            title = this.title,
            notes = this.notes,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source,
            deviceType = null
        )
    }

    fun SleepStageRec.toEntity(sessionId: String, source: String): SleepStageEntity {
        return SleepStageEntity(
            id = this.id,
            sleepSessionId = sessionId,
            source = source,
            stage = this.stage,
            startTime = this.startTime,
            endTime = this.endTime,
            deviceType = null
        )
    }

    fun SleepSessionWithStages.toDomain(): SleepSessionRec {
        return SleepSessionRec(
            id = this.session.id,
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
            id = this.id,
            stage = this.stage,
            startTime = this.startTime,
            endTime = this.endTime,
            source = this.source
        )
    }

    // --- Basal Metabolic Rate ---
    fun BasalMetabolicRateRec.toEntity(): BasalMetabolicRateEntity {
        return BasalMetabolicRateEntity(
            id = this.id,
            energyKilocaloriesPerDay = this.energyKilocaloriesPerDay,
            time = this.time,
            source = this.source,
            deviceType = null
        )
    }

    fun BasalMetabolicRateEntity.toDomain(): BasalMetabolicRateRec {
        return BasalMetabolicRateRec(
            id = this.id,
            energyKilocaloriesPerDay = this.energyKilocaloriesPerDay,
            time = this.time,
            source = this.source
        )
    }
}
