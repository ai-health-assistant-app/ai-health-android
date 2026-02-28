package com.ai_health.feature.dashboard

import com.ai_health.core.domain.model.BiometricReport
import com.ai_health.ui.components.ChartDataPoint
import java.time.LocalDate

/**
 * Represents a single night's sleep data, including the date.
 * If session is null, it means there's no sleep data available for that night.
 */
data class SleepNightData(
    val date: LocalDate,
    val session: com.ai_health.core.domain.model.SleepSessionRec?,
    val analysis: com.ai_health.core.domain.model.SleepQualityResult?
)

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
    val sleepHistory: List<ChartDataPoint> = emptyList(),
    
    // Sleep Detail Data
    val selectedSleepSession: com.ai_health.core.domain.model.SleepSessionRec? = null,
    val sleepQualityAnalysis: com.ai_health.core.domain.model.SleepQualityResult? = null,
    
    // Sleep History for Date Navigation
    val sleepNights: List<SleepNightData> = emptyList(),
    val isLoadingMoreSleep: Boolean = false,
    
    // Biometric Engine
    val biometricReport: BiometricReport? = null,
    val isBiometricLoading: Boolean = false
)
