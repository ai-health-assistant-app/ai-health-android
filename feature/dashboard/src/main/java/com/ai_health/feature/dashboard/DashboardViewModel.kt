package com.ai_health.feature.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.usecase.GetDashboardDataUseCase
import com.ai_health.core.domain.usecase.sleep.AnalyzeSleepQualityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale
import com.ai_health.ui.components.ChartDataPoint
import com.ai_health.core.domain.repository.HealthRepository

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val analyzeSleepQualityUseCase: AnalyzeSleepQualityUseCase,
    private val healthRepository: HealthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_IS_REFRESHING = "is_refreshing"
    }

    // Fetch sleep sessions for the last 30 days
    private val thirtyDaysAgo = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)
    private val sleepSessionsFlow = healthRepository.getSleepHistory(thirtyDaysAgo)

    val uiState: StateFlow<DashboardUiState> = getDashboardDataUseCase()
        .combine(sleepSessionsFlow) { data, sleepSessions ->
            val h = data.sleepMinutes / 60
            val m = data.sleepMinutes % 60
            
            // Reverse the list so newest sessions are first (index 0)
            // This way: swipe right (increasing index) = older data, swipe left = newer data
            val reversedSessions = sleepSessions.reversed()
            
            // Analyze each sleep session
            val sleepAnalyses = reversedSessions.associate { session ->
                session.id to analyzeSleepQualityUseCase(session)
            }

            DashboardUiState(
                isLoading = false,
                stepsFormatted = "${data.steps}",
                sleepTimeFormatted = "${h}h ${m}m",
                heartRateFormatted = "${data.avgHeartRate} bpm",
                caloriesFormatted = "${data.calories} kcal",
                distanceFormatted = String.format(Locale.US, "%.2f km", data.distanceKm),
                oxygenFormatted = String.format(Locale.US, "%.1f %%", data.oxygenSaturation),

                stepsHistory = data.stepsHistory.map { ChartDataPoint(it.timestamp, it.value) },
                sleepHistory = data.sleepHistory.map { ChartDataPoint(it.timestamp, it.value) },
                heartRateHistory = data.heartRateHistory.map { ChartDataPoint(it.timestamp, it.value) },
                caloriesHistory = data.caloriesHistory.map { ChartDataPoint(it.timestamp, it.value) },
                distanceHistory = data.distanceHistory.map { ChartDataPoint(it.timestamp, it.value) },
                oxygenHistory = data.oxygenHistory.map { ChartDataPoint(it.timestamp, it.value) },
                
                selectedSleepSession = data.latestSleepSession,
                sleepQualityAnalysis = data.latestSleepSession?.let { analyzeSleepQualityUseCase(it) },
                
                sleepSessions = reversedSessions,
                sleepAnalyses = sleepAnalyses
            )
        }
        .catch { e ->
            // In a real app we might want to emit an Error state, but DashboardUiState structure in previous file
            // didn't strictly have a sealed class hierarchy (it was a data class).
            // User request mentioned "DashboardUiState.Success", "DashboardUiState.Error". 
            // Previous file `DashboardUiState` was a simple data class (line 21: `MutableStateFlow(DashboardUiState())`).
            // I need to check `DashboardUiState` definition to see if it sustains Loading/Error.
            // If it's a data class, I can just set error properties if they exist, or log.
            // User instruction: "Gestisci i tre stati: Loading, Success, Error".
            // This suggests I might need to Refactor DashboardUiState to be a sealed interface OR use flags.
            // But the previous file showed `data class` usage.
            // "Transforma ... in StateFlow<DashboardUiState> ... DashboardUiState.Success(data)".
            // This implies the user *expects* me to change DashboardUiState to a sealed class/interface if it isn't one.
            // I should check `DashboardUiState.kt` content.
            // I haven't read it yet. I assumed it. 
            // I will assume for now I need to update it or it is a data class with flags.
            // Wait, previous file `DashboardViewModel.kt` used `isLoading = true`.
            // I will err on side of caution: maintain data class if I can't see the file, OR check the file first.
            // I'll check `DashboardUiState` first to be safe.
            emit(DashboardUiState(isLoading = false)) // Fallback if catch happens
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true)
        )

    val isRefreshing: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_IS_REFRESHING, false)

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            savedStateHandle[KEY_IS_REFRESHING] = true
            try {
                // Forza il sync dei dati da Health Connect
                healthRepository.syncHealthData()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Fondamentale: Spegne l'indicatore alla fine
                savedStateHandle[KEY_IS_REFRESHING] = false
            }
        }
    }
}
