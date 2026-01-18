package com.ai_health.core.domain.usecase

import com.ai_health.core.domain.model.DashboardData
import com.ai_health.core.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetDashboardDataUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    operator fun invoke(): Flow<DashboardData> = flow {
        // 1. Sync latest data from Health Connect
        repository.syncHealthData()

        // 2. Define strict business rule for "Today"
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 3. Aggregate data from various granular sources
        val data = DashboardData(
            steps = repository.getSteps(startOfToday),
            sleepMinutes = repository.getSleepMinutes(startOfToday),
            avgHeartRate = repository.getAvgHeartRate(startOfToday),
            calories = repository.getCalories(startOfToday),
            distanceKm = repository.getDistanceKm(startOfToday),
            oxygenSaturation = repository.getOxygenSaturation(startOfToday)
        )

        emit(data)
    }
}
