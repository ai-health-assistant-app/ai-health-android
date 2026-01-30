package com.ai_health.core.domain.model

import java.time.Instant

enum class StepSource {
    PHONE,
    WEARABLE
}

enum class UserActivityType {
    STILL,
    WALKING,
    RUNNING,
    ON_BICYCLE,
    IN_VEHICLE,
    TILTING,
    UNKNOWN
}

data class RawStep(
    val startTime: Instant,
    val endTime: Instant,
    val source: StepSource,
    val rawCount: Long
)

data class ValidatedStep(
    val startTime: Instant,
    val endTime: Instant,
    val effectiveCount: Long
)
