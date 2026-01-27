package com.ai_health.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

// Common interface just to enforce ID presence if needed, or just individual classes.
// User requested "Crea una @Entity per ogni modello".

@Entity(tableName = "heart_rate")
data class HeartRateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val beatsPerMinute: Long,
    val time: Instant
)

@Entity(tableName = "steps")
data class StepsEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val count: Long,
    val startTime: Instant,
    val endTime: Instant,
    val source: String
)

@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val exerciseType: String,
    val title: String?,
    val notes: String?,
    val startTime: Instant,
    val endTime: Instant
)

@Entity(tableName = "oxygen_saturation")
data class OxygenSaturationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val percentage: Double,
    val time: Instant
)

@Entity(tableName = "distance")
data class DistanceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val distanceMeters: Double,
    val startTime: Instant,
    val endTime: Instant
)

@Entity(tableName = "calories")
data class CaloriesEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val energyKilocalories: Double,
    val startTime: Instant,
    val endTime: Instant
)

@Entity(tableName = "basal_metabolic_rate")
data class BasalMetabolicRateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val energyKilocaloriesPerDay: Double,
    val time: Instant
)
