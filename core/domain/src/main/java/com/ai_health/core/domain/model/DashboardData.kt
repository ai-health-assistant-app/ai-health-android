package com.ai_health.core.domain.model

data class DashboardData(
    val steps: Int = 0,
    val sleepMinutes: Int = 0,
    val avgHeartRate: Int = 0,
    val calories: Int = 0,
    val distanceKm: Double = 0.0,
    val oxygenSaturation: Double = 0.0
)
