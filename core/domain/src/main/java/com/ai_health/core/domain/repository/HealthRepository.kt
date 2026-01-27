package com.ai_health.core.domain.repository

import com.ai_health.core.domain.model.DashboardData

import com.ai_health.core.domain.model.HealthMetricPoint

interface HealthRepository {
    suspend fun syncHealthData()
    suspend fun getSteps(startTime: Long): Int
    suspend fun getSleepMinutes(startTime: Long): Int
    suspend fun getAvgHeartRate(startTime: Long): Int
    suspend fun getCalories(startTime: Long): Int
    suspend fun getDistanceKm(startTime: Long): Double
    suspend fun getOxygenSaturation(startTime: Long): Double

    // History methods
    suspend fun getStepsHistory(startTime: Long): List<HealthMetricPoint>
    suspend fun getSleepHistory(startTime: Long): List<HealthMetricPoint>
    suspend fun getHeartRateHistory(startTime: Long): List<HealthMetricPoint>
    suspend fun getCaloriesHistory(startTime: Long): List<HealthMetricPoint>
    suspend fun getDistanceHistory(startTime: Long): List<HealthMetricPoint>
    suspend fun getOxygenHistory(startTime: Long): List<HealthMetricPoint>
}
