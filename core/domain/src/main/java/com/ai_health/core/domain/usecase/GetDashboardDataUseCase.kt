package com.ai_health.core.domain.usecase

import com.ai_health.core.domain.model.DashboardData
import com.ai_health.core.domain.model.HealthMetricPoint
import com.ai_health.core.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    private val repository: HealthRepository
) {
    operator fun invoke(): Flow<DashboardData> {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        // 1. Finestra per le metriche giornaliere (Passi, Calorie, ecc.) -> Da 00:00 di OGGI
        val startOfToday = today.atStartOfDay(zoneId).toInstant()

        // 2. Finestra per il Sonno -> Da 12:00 (Mezzogiorno) di IERI
        // Questo cattura le dormite iniziate la sera prima (es. 4 Feb 23:00)
        val startOfSleepWindow = today.minusDays(1)
            .atTime(12, 0) 
            .atZone(zoneId)
            .toInstant()

        val stepsFlow = repository.getStepsHistory(startOfToday)
        
        // USA startOfSleepWindow QUI:
        val sleepFlow = repository.getSleepHistory(startOfSleepWindow)
        
        val heartFlow = repository.getHeartRateHistory(startOfToday)
        val caloriesFlow = repository.getCaloriesHistory(startOfToday)
        val distanceFlow = repository.getDistanceHistory(startOfToday)
        val oxygenFlow = repository.getOxygenHistory(startOfToday)

        return combine(
            combine(stepsFlow, sleepFlow, heartFlow, ::Triple),
            combine(caloriesFlow, distanceFlow, oxygenFlow, ::Triple)
        ) { (steps, sleep, heart), (calories, distance, oxygen) ->
            
            DashboardData(
                steps = calculateSmartSteps(steps),
                sleepMinutes = calculateTotalSleepMinutes(sleep),
                avgHeartRate = if (heart.isNotEmpty()) heart.map { it.beatsPerMinute }.average().toInt() else 0,
                calories = calories.sumOf { it.energyKilocalories }.toInt(),
                distanceKm = distance.sumOf { it.distanceMeters } / 1000.0,
                oxygenSaturation = if (oxygen.isNotEmpty()) oxygen.map { it.percentage }.average() else 0.0,
                
                // Prendi la sessione più recente trovata nella finestra "allargata"
                latestSleepSession = sleep.maxByOrNull { it.endTime },

                stepsHistory = steps.map { HealthMetricPoint(it.startTime.toEpochMilli(), it.count.toDouble()) },
                // Nota: La history del sonno mostrerà anche l'inizio della sessione (ieri sera), che è corretto.
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

    // ... (Il resto delle funzioni private calculateSmartSteps e calculateTotalSleepMinutes rimangono uguali)
    private fun calculateSmartSteps(records: List<StepsRec>): Int {
        if (records.isEmpty()) return 0
        return records
            .groupBy { it.startTime to it.endTime }
            .map { (_, duplicates) -> duplicates.maxOf { it.count } }
            .sum()
            .toInt()
    }

    private fun calculateTotalSleepMinutes(sessions: List<SleepSessionRec>): Int {
        if (sessions.isEmpty()) return 0
        val sorted = sessions.sortedBy { it.startTime }
        val merged = mutableListOf<Pair<Instant, Instant>>()
        var currentStart = sorted[0].startTime
        var currentEnd = sorted[0].endTime

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startTime.isBefore(currentEnd)) {
                if (next.endTime.isAfter(currentEnd)) {
                    currentEnd = next.endTime
                }
            } else {
                merged.add(currentStart to currentEnd)
                currentStart = next.startTime
                currentEnd = next.endTime
            }
        }
        merged.add(currentStart to currentEnd)
        return merged.sumOf { (start, end) ->
            java.time.Duration.between(start, end).toMinutes()
        }.toInt()
    }
}