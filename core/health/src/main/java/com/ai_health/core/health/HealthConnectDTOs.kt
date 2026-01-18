package com.ai_health.core.health

/**
 * These DTOs (Data Transfer Objects) serve to decouple
 * the Health Connect manager from the local Database.
 */

data class RawStep(
    val count: Long,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawHeartRate(
    val bpm: Double, 
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawSleep(
    val durationMinutes: Double,
    val stage: Int, 
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawDistance(
    val distanceMeters: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawCalories(
    val kilocalories: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawOxygen(
    val percentage: Double, 
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawExercise(
    val type: String, 
    val durationMinutes: Double,
    val title: String? = null,
    val notes: String? = null,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)

data class RawBMR(
    val kcalPerDay: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String
)
