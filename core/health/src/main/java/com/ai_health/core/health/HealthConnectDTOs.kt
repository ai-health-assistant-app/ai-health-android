package com.ai_health.core.health

/**
 * These DTOs (Data Transfer Objects) serve to decouple
 * the Health Connect manager from the local Database.
 */

data class RawStep(
    val id: String,
    val count: Long,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawHeartRate(
    val id : String,
    val bpm: Double, 
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawSleepStage(
    val stage: Int,
    val startTime: Long,
    val endTime: Long
)

data class RawSleep(
    val id: String,
    val durationMinutes: Double,
    val stage: Int, // Deprecated or for summary? Kept for compatibility
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val stages: List<RawSleepStage> = emptyList()
)

data class RawDistance(
    val id: String,
    val distanceMeters: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawCalories(
    val id: String,
    val kilocalories: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawOxygen(
    val id: String,
    val percentage: Double, 
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawExercise(
    val id: String,
    val type: String, 
    val durationMinutes: Double,
    val title: String? = null,
    val notes: String? = null,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawBMR(
    val id: String,
    val kcalPerDay: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)
