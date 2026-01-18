package com.ai_health.assistant.presentation.dashboard

data class DashboardUiState(
    val stepsFormatted: String = "--",
    val sleepTimeFormatted: String = "--",
    val heartRateFormatted: String = "--",
    val caloriesFormatted: String = "--",
    val distanceFormatted: String = "--",
    val oxygenFormatted: String = "--",
    val isLoading: Boolean = false
)
