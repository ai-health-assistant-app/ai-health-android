package com.ai_health.core.domain.model

import java.time.Instant

data class UserActivity(
    val type: UserActivityType,
    val confidence: Int, // 0-100
    val timestamp: Instant
)
