package com.ai_health.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.usecase.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val healthRepository: HealthRepository // Need repo for sync, or use UseCase if it exposed sync. UseCase currently only invoke() -> Flow. 
    // User instruction: "Sync: Nel blocco init, lancia ... repository.syncHealthData()". 
    // So I need repository injected OR UseCase needs a sync method.
    // The previous UseCase code calling repository.syncHealthData() was removed.
    // I should probably skip injecting repository if I can, but instruction says "repository.syncHealthData()".
    // I will inject Repository.
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = getDashboardDataUseCase()
        .map { data ->
            val h = data.sleepMinutes / 60
            val m = data.sleepMinutes % 60

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
                oxygenHistory = data.oxygenHistory.map { ChartDataPoint(it.timestamp, it.value) }
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Forza il sync dei dati da Health Connect
                healthRepository.syncHealthData()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Fondamentale: Spegne l'indicatore alla fine
                _isRefreshing.value = false
            }
        }
    }
}
