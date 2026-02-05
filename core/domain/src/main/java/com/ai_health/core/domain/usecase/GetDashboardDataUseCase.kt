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
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val stepsFlow = repository.getStepsHistory(startOfToday)
        val sleepFlow = repository.getSleepHistory(startOfToday)
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

                stepsHistory = steps.map { HealthMetricPoint(it.startTime.toEpochMilli(), it.count.toDouble()) },
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

    private fun calculateSmartSteps(records: List<StepsRec>): Int {
        if (records.isEmpty()) return 0
        
        // Esempio naive di deduplica:
        // Raggruppa per intervallo temporale (startTime + endTime)
        // Se due app scrivono passi nello stesso esatto intervallo, prendi il max.
        // Nota: Questo non risolve sovrapposizioni parziali (es. 14:00-14:15 vs 14:10-14:20)
        return records
            .groupBy { it.startTime to it.endTime }
            .map { (_, duplicates) -> duplicates.maxOf { it.count } } // Prendi il valore più alto tra i duplicati
            .sum()
            .toInt()
    }

    private fun calculateTotalSleepMinutes(sessions: List<SleepSessionRec>): Int {
        if (sessions.isEmpty()) return 0
        
        // 1. Sort by start time
        val sorted = sessions.sortedBy { it.startTime }
        
        // 2. Merge overlaps
        val merged = mutableListOf<Pair<Instant, Instant>>()
        var currentStart = sorted[0].startTime
        var currentEnd = sorted[0].endTime

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startTime.isBefore(currentEnd)) {
                // Overlap: extend current end if next ends later
                if (next.endTime.isAfter(currentEnd)) {
                    currentEnd = next.endTime
                }
            } else {
                // No overlap: push current, start new
                merged.add(currentStart to currentEnd)
                currentStart = next.startTime
                currentEnd = next.endTime
            }
        }
        merged.add(currentStart to currentEnd)

        // 3. Sum durations
        return merged.sumOf { (start, end) ->
            java.time.Duration.between(start, end).toMinutes()
        }.toInt()
    }
}
