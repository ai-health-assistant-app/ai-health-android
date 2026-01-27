package com.ai_health.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.usecase.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale
import com.ai_health.ui.components.ChartDataPoint

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                getDashboardDataUseCase().collect { data ->
                    val h = data.sleepMinutes / 60
                    val m = data.sleepMinutes % 60

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            stepsFormatted = "${data.steps}",
                            sleepTimeFormatted = "${h}h ${m}m",
                            heartRateFormatted = "${data.avgHeartRate} bpm",
                            caloriesFormatted = "${data.calories} kcal",
                            distanceFormatted = String.format(Locale.US, "%.2f km", data.distanceKm),
                            oxygenFormatted = String.format(Locale.US, "%.1f %%", data.oxygenSaturation),
                            
                            // History mapping
                            stepsHistory = data.stepsHistory.map { ChartDataPoint(it.timestamp, it.value) },
                            sleepHistory = data.sleepHistory.map { ChartDataPoint(it.timestamp, it.value) },
                            heartRateHistory = data.heartRateHistory.map { ChartDataPoint(it.timestamp, it.value) },
                            caloriesHistory = data.caloriesHistory.map { ChartDataPoint(it.timestamp, it.value) },
                            distanceHistory = data.distanceHistory.map { ChartDataPoint(it.timestamp, it.value) },
                            oxygenHistory = data.oxygenHistory.map { ChartDataPoint(it.timestamp, it.value) }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                e.printStackTrace()
            }
        }
    }
}
