package com.ai_health.core.domain.model

data class DashboardData(
    val steps: Int = 0,
    val sleepMinutes: Int = 0,
    val avgHeartRate: Int = 0,
    val calories: Int = 0,
    val distanceKm: Double = 0.0,
    val oxygenSaturation: Double = 0.0,
    
    // Sync Info
    val lastSyncTime: java.time.Instant? = null,
    val sourcePackage: String? = null,
    
    // History Data for Charts
    val stepsHistory: List<HealthMetricPoint> = emptyList(),
    val heartRateHistory: List<HealthMetricPoint> = emptyList(),
    val caloriesHistory: List<HealthMetricPoint> = emptyList(),
    val distanceHistory: List<HealthMetricPoint> = emptyList(),
    val oxygenHistory: List<HealthMetricPoint> = emptyList(),
    val sleepHistory: List<HealthMetricPoint> = emptyList()
)
