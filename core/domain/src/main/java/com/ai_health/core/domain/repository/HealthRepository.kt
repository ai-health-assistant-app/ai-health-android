package com.ai_health.core.domain.repository

import com.ai_health.core.domain.model.DashboardData

interface HealthRepository {
    suspend fun syncHealthData()
    suspend fun getSteps(startTime: Long): Int
    suspend fun getSleepMinutes(startTime: Long): Int
    suspend fun getAvgHeartRate(startTime: Long): Int
    suspend fun getCalories(startTime: Long): Int
    suspend fun getDistanceKm(startTime: Long): Double
    suspend fun getOxygenSaturation(startTime: Long): Double
}
