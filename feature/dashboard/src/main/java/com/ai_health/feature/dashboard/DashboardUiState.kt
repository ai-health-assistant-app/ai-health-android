package com.ai_health.feature.dashboard

import com.ai_health.ui.components.ChartDataPoint

data class DashboardUiState(
    val isLoading: Boolean = false,
    val stepsFormatted: String = "0",
    val sleepTimeFormatted: String = "0h 0m",
    val heartRateFormatted: String = "0 bpm",
    val caloriesFormatted: String = "0 kcal",
    val distanceFormatted: String = "0.00 km",
    val oxygenFormatted: String = "0.0 %",
    
    // Charts Data
    val stepsHistory: List<ChartDataPoint> = emptyList(),
    val heartRateHistory: List<ChartDataPoint> = emptyList(),
    val caloriesHistory: List<ChartDataPoint> = emptyList(),
    val distanceHistory: List<ChartDataPoint> = emptyList(),
    val oxygenHistory: List<ChartDataPoint> = emptyList(),
    val sleepHistory: List<ChartDataPoint> = emptyList()
)
