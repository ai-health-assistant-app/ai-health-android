package com.ai_health.core.domain.usecase

import com.ai_health.core.domain.model.DashboardData
import com.ai_health.core.domain.model.HealthMetricPoint
import com.ai_health.core.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.StepsRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.CaloriesRec
import com.ai_health.core.domain.model.DistanceRec
import com.ai_health.core.domain.model.OxygenSaturationRec
import java.time.Instant

class GetDashboardDataUseCase @Inject constructor(
    private val repository: HealthRepository,
    private val validateStepCountUseCase: ValidateStepCountUseCase
) {
    operator fun invoke(): Flow<DashboardData> {
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        // 1. Validated Steps Flow (Optimized)
        val validatedStepsFlow = repository.getStepsHistory(startOfToday)
            .distinctUntilChanged()
            .map { stepsList ->
                // Map generic StepsRec to Domain RawStep
                val rawSteps = stepsList.map {
                    com.ai_health.core.domain.model.RawStep(
                        startTime = it.startTime,
                        endTime = it.endTime,
                        source = classifySource(it.source),
                        rawCount = it.count
                    )
                }
                // Apply Validation Logic only when steps actually change
                validateStepCountUseCase(rawSteps)
            }

        val sleepFlow = repository.getSleepHistory(startOfToday)
        val heartFlow = repository.getHeartRateHistory(startOfToday)
        val caloriesFlow = repository.getCaloriesHistory(startOfToday)
        val distanceFlow = repository.getDistanceHistory(startOfToday)
        val oxygenFlow = repository.getOxygenHistory(startOfToday)

        return combine(
            combine(validatedStepsFlow, sleepFlow, heartFlow, ::Triple),
            combine(caloriesFlow, distanceFlow, oxygenFlow, ::Triple)
        ) { (validatedSteps, sleep, heart), (calories, distance, oxygen) ->

            DashboardData(
                // Use validated steps for total and history
                steps = validatedSteps.sumOf { it.effectiveCount }.toInt(),
                
                sleepMinutes = sleep.sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() }.toInt(),
                avgHeartRate = if (heart.isNotEmpty()) heart.map { it.beatsPerMinute }.average().toInt() else 0,
                calories = calories.sumOf { it.energyKilocalories }.toInt(),
                distanceKm = distance.sumOf { it.distanceMeters } / 1000.0,
                oxygenSaturation = if (oxygen.isNotEmpty()) oxygen.map { it.percentage }.average() else 0.0,

                stepsHistory = validatedSteps.map { 
                    HealthMetricPoint(it.startTime.toEpochMilli(), it.effectiveCount.toDouble()) 
                },
                sleepHistory = sleep.map { 
                    val minutes = java.time.Duration.between(it.startTime, it.endTime).toMinutes().toDouble()
                    HealthMetricPoint(it.startTime.toEpochMilli(), minutes) 
                },
                heartRateHistory = heart.map { HealthMetricPoint(it.time.toEpochMilli(), it.beatsPerMinute.toDouble()) },
                caloriesHistory = calories.map { HealthMetricPoint(it.startTime.toEpochMilli(), it.energyKilocalories) },
                distanceHistory = distance.map { HealthMetricPoint(it.startTime.toEpochMilli(), it.distanceMeters) },
                oxygenHistory = oxygen.map { HealthMetricPoint(it.time.toEpochMilli(), it.percentage) }
            )
        }
    }

    private fun classifySource(packageName: String): com.ai_health.core.domain.model.StepSource {
        val lower = packageName.lowercase()
        println("StepSourceDebug: Classifying '$packageName' (lower: '$lower')")
        
        // Rule 1: Specific keywords imply WEARABLE source
        // This includes "com.xiaomi.wearable" which organizes data from Xiaomi devices
        if (lower.contains("wearable") ||
            lower.contains("watch") ||
            lower.contains("garmin") ||
            lower.contains("fitbit") ||
            lower.contains("xiaomi")) {
            return com.ai_health.core.domain.model.StepSource.WEARABLE
        }

        // Rule 2: Google Fit and generic apps -> PHONE
        return com.ai_health.core.domain.model.StepSource.PHONE
    }
}
