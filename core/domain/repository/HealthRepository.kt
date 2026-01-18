package com.ai_health.assistant.core.domain.repository

import com.ai_health.assistant.core.domain.model.DashboardData

interface HealthRepository {
    suspend fun syncAndGetMetrics(): DashboardData
}