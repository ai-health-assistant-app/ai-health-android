package com.ai_health.feature.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.usecase.GetDashboardDataUseCase
import com.ai_health.core.domain.usecase.sleep.AnalyzeSleepQualityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale
import java.time.Instant
import java.time.temporal.ChronoUnit
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
        private const val DAYTIME_WINDOW_HOURS = 16L
        private const val BASELINE_DAYS = 7
    }

    // Fetch sleep sessions for the last 30 days
    private val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
    private val sleepSessionsFlow = healthRepository.getSleepHistory(thirtyDaysAgo)
    
    // Fetch heart rate history for baseline and session analysis
    private val heartRateFlow = healthRepository.getHeartRateHistory(thirtyDaysAgo)

    val uiState: StateFlow<DashboardUiState> = getDashboardDataUseCase()
        .combine(sleepSessionsFlow) { data, sleepSessions -> Pair(data, sleepSessions) }
        .combine(heartRateFlow) { (data, sleepSessions), allHeartRates ->
            val h = data.sleepMinutes / 60
            val m = data.sleepMinutes % 60
            
            // Reverse the list so newest sessions are first (index 0)
            val reversedSessions = sleepSessions.reversed()
            
            // Calculate baseline RHR from last 7 days of data
            val baselineRhr = calculateBaselineRhr(allHeartRates)
            
            // Analyze each sleep session with per-session HR data
            val sleepAnalyses = reversedSessions.associate { session ->
                val sessionHrData = getHeartRatesForSession(allHeartRates, session)
                session.id to analyzeSleepQualityUseCase(session, sessionHrData, baselineRhr)
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
                sleepQualityAnalysis = data.latestSleepSession?.let { session ->
                    val hrData = getHeartRatesForSession(allHeartRates, session)
                    analyzeSleepQualityUseCase(session, hrData, baselineRhr)
                },
                
                sleepSessions = reversedSessions,
                sleepAnalyses = sleepAnalyses
            )
        }
        .catch { e ->
            e.printStackTrace()
            emit(DashboardUiState(isLoading = false))
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

    /**
     * Estrae i dati HR per una specifica sessione di sonno.
     * Finestra: sleepStart - 16h (dati diurni per dipping) fino a sleepEnd.
     */
    private fun getHeartRatesForSession(
        allHeartRates: List<HeartRateRec>,
        session: SleepSessionRec
    ): List<HeartRateRec> {
        val windowStart = session.startTime.minus(DAYTIME_WINDOW_HOURS, ChronoUnit.HOURS)
        val windowEnd = session.endTime
        
        return allHeartRates.filter { hr ->
            hr.time >= windowStart && hr.time <= windowEnd
        }
    }

    /**
     * Calcola la baseline RHR come media degli ultimi 7 giorni.
     * Considera i valori minimi notturni quando possibile.
     */
    private fun calculateBaselineRhr(heartRates: List<HeartRateRec>): Int {
        if (heartRates.isEmpty()) return 60 // Default fallback
        
        val now = Instant.now()
        val sevenDaysAgo = now.minus(BASELINE_DAYS.toLong(), ChronoUnit.DAYS)
        
        // Filtra gli ultimi 7 giorni e prendi i valori più bassi (proxy per RHR)
        val recentHr = heartRates.filter { it.time >= sevenDaysAgo }
        
        if (recentHr.isEmpty()) return 60
        
        // Calcola la media dei 10% valori più bassi (approx. minimo notturno)
        val sorted = recentHr.sortedBy { it.beatsPerMinute }
        val lowestCount = (sorted.size * 0.1).toInt().coerceAtLeast(1)
        val lowestValues = sorted.take(lowestCount)
        
        return lowestValues.map { it.beatsPerMinute }.average().toInt().coerceIn(40, 100)
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
