package com.ai_health.feature.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.SleepQualityResult
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.ai_health.ui.components.ChartDataPoint
import com.ai_health.core.domain.repository.HealthRepository

// Helper data class for combining multiple flows
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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
        private const val INITIAL_WEEKS_TO_LOAD = 1 // Start with 1 week
        private const val PAGINATION_LOAD_WEEKS = 1 // Load 1 week at a time when paginating
    }

    // In-memory cache per evitare ricalcoli costosi dell'analisi del sonno
    private val sleepAnalysisCache = mutableMapOf<String, SleepQualityResult>()
    
    // Pagination state
    private val _weeksLoaded = MutableStateFlow(INITIAL_WEEKS_TO_LOAD)
    private val _isLoadingMoreSleep = MutableStateFlow(false)


    // Fetch sleep sessions for the last 30 days
    private val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
    private val sleepSessionsFlow = healthRepository.getSleepHistory(thirtyDaysAgo)
    
    // Fetch heart rate history for baseline and session analysis
    private val heartRateFlow = healthRepository.getHeartRateHistory(thirtyDaysAgo)

    val uiState: StateFlow<DashboardUiState> = getDashboardDataUseCase()
        .combine(sleepSessionsFlow) { data, sleepSessions -> Pair(data, sleepSessions) }
        .combine(heartRateFlow) { (data, sleepSessions), allHeartRates -> 
            Triple(data, sleepSessions, allHeartRates) 
        }
        .combine(_weeksLoaded) { (data, sleepSessions, allHeartRates), weeksLoaded ->
            Quadruple(data, sleepSessions, allHeartRates, weeksLoaded)
        }
        .combine(_isLoadingMoreSleep) { (data, sleepSessions, allHeartRates, weeksLoaded), isLoadingMore ->
            val h = data.sleepMinutes / 60
            val m = data.sleepMinutes % 60
            
            // Calculate baseline RHR from last 7 days of data
            val baselineRhr = calculateBaselineRhr(allHeartRates)
            
            // Generate date sequence based on weeks loaded
            val sleepNights = generateSleepNights(
                sleepSessions = sleepSessions,
                allHeartRates = allHeartRates,
                baselineRhr = baselineRhr,
                weeksToShow = weeksLoaded
            )

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
                    sleepAnalysisCache.getOrPut(session.id) {
                        val hrData = getHeartRatesForSession(allHeartRates, session)
                        analyzeSleepQualityUseCase(session, hrData, baselineRhr)
                    }
                },
                
                sleepNights = sleepNights,
                isLoadingMoreSleep = isLoadingMore
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
                // Invalida la cache per forzare il ricalcolo con nuovi dati
                sleepAnalysisCache.clear()
                
                // Reset pagination to initial state
                _weeksLoaded.value = INITIAL_WEEKS_TO_LOAD
                
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
    
    /**
     * Genera la lista di SleepNightData per il numero di settimane specificate.
     * Crea una sequenza completa di date (dalla più recente alla più vecchia) e mappa
     * le sessioni di sonno disponibili. Le notti senza dati avranno session=null.
     */
    private fun generateSleepNights(
        sleepSessions: List<SleepSessionRec>,
        allHeartRates: List<HeartRateRec>,
        baselineRhr: Int,
        weeksToShow: Int
    ): List<SleepNightData> {
        val today = LocalDate.now(ZoneId.systemDefault())
        val daysToShow = weeksToShow * 7
        
        // Generate complete date sequence from today going back
        // Index 0 = most recent (today), higher indices = older dates
        // Swipe right (increasing index) = older dates
        // Swipe left (decreasing index) = newer dates
        val dateSequence = (0 until daysToShow).map { dayOffset ->
            today.minusDays(dayOffset.toLong())
        }
        
        // Map sessions to their corresponding dates (based on sleep start time)
        // Group by date, taking the most recent session if multiple exist for the same night
        val sessionsByDate = sleepSessions
            .groupBy { session ->
                session.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .mapValues { (_, sessions) ->
                // Take the most recent session (last in list chronologically)
                sessions.maxByOrNull { it.startTime }
            }
        
        // Create SleepNightData for each date
        return dateSequence.map { date ->
            val session = sessionsByDate[date]
            val analysis = session?.let { sess ->
                sleepAnalysisCache.getOrPut(sess.id) {
                    val hrData = getHeartRatesForSession(allHeartRates, sess)
                    analyzeSleepQualityUseCase(sess, hrData, baselineRhr)
                }
            }
            
            SleepNightData(
                date = date,
                session = session,
                analysis = analysis
            )
        }
    }
    
    /**
     * Chiamato quando l'utente naviga verso il penultimo elemento.
     * Carica la settimana precedente di dati.
     */
    fun loadPreviousWeek() {
        if (_isLoadingMoreSleep.value) return // Avoid duplicate loads
        
        viewModelScope.launch {
            _isLoadingMoreSleep.value = true
            try {
                // Incrementa il numero di settimane caricate
                _weeksLoaded.value += PAGINATION_LOAD_WEEKS
            } finally {
                _isLoadingMoreSleep.value = false
            }
        }
    }
    
    /**
     * Callback da chiamare quando l'utente cambia pagina nel pager.
     * Se raggiunge il penultimo elemento, triggera il caricamento.
     */
    fun onPageChanged(currentPage: Int, totalPages: Int) {
        // Triggera il caricamento quando raggiunge il penultimo elemento
        if (currentPage >= totalPages - 2 && !_isLoadingMoreSleep.value) {
            loadPreviousWeek()
        }
    }
}

