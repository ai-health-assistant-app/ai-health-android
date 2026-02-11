package com.ai_health.core.domain.model

enum class SleepStageType(val value: Int) {
    UNKNOWN(0),
    AWAKE(1),
    SLEEPING(2), // Generic sleeping
    OUT_OF_BED(3),
    LIGHT(4),
    DEEP(5),
    REM(6);

    companion object {
        fun fromInt(value: Int): SleepStageType {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}
