package com.ai_health.feature.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.model.BiometricReport
import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.HrSample
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.SleepQualityResult
import com.ai_health.core.domain.model.UserBiometricProfile
import com.ai_health.core.domain.usecase.GetDashboardDataUseCase
import com.ai_health.core.domain.usecase.biometric.BiometricEngineUseCase
import com.ai_health.core.domain.usecase.sleep.AnalyzeSleepQualityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val analyzeSleepQualityUseCase: AnalyzeSleepQualityUseCase,
    private val biometricEngineUseCase: BiometricEngineUseCase,
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
    
    // Biometric engine state
    private val _biometricReport = MutableStateFlow<BiometricReport?>(null)
    private val _isBiometricLoading = MutableStateFlow(false)


    // Fetch sleep sessions for the last 30 days
    private val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
    private val sleepSessionsFlow = healthRepository.getSleepHistory(thirtyDaysAgo)
    
    // Fetch heart rate history for baseline and session analysis
    private val heartRateFlow = healthRepository.getHeartRateHistory(thirtyDaysAgo)

    val isRefreshing: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_IS_REFRESHING, false)

    val uiState: StateFlow<DashboardUiState> = getDashboardDataUseCase()
        .combine(sleepSessionsFlow) { data, sleepSessions -> Pair(data, sleepSessions) }
        .combine(heartRateFlow) { (data, sleepSessions), allHeartRates -> 
            Triple(data, sleepSessions, allHeartRates) 
        }
        .combine(_weeksLoaded) { (data, sleepSessions, allHeartRates), weeksLoaded ->
            Quadruple(data, sleepSessions, allHeartRates, weeksLoaded)
        }
        .combine(_isLoadingMoreSleep) { (data, sleepSessions, allHeartRates, weeksLoaded), isLoadingMore ->
            Quintuple(data, sleepSessions, allHeartRates, weeksLoaded, isLoadingMore)
        }
        .combine(_biometricReport) { (data, sleepSessions, allHeartRates, weeksLoaded, isLoadingMore), biometric ->
            Sextuple(data, sleepSessions, allHeartRates, weeksLoaded, isLoadingMore, biometric)
        }
        .combine(isRefreshing) { (data, sleepSessions, allHeartRates, weeksLoaded, isLoadingMore, biometric), refreshing ->
            val h = data.sleepMinutes / 60
            val m = data.sleepMinutes % 60
            
            // Calculate baseline RHR from last 7 days of data
            val baselineRhr = calculateBaselineRhr(allHeartRates)
            
            // Generate date sequence based on weeks loaded. Note: we no longer compute inside mapping if possible, but keep cache for sync operations
            val sleepNights = generateSleepNights(
                sleepSessions = sleepSessions,
                allHeartRates = allHeartRates,
                baselineRhr = baselineRhr,
                weeksToShow = weeksLoaded
            )
            
            // Trigger biometric computation ONLY if we have new data and it's not currently running
            // This async call updates _biometricReport, which triggers THIS combine block again
            if (allHeartRates.isNotEmpty() && (biometric == null)) {
               computeBiometrics(allHeartRates, sleepSessions, baselineRhr)
            }

            DashboardUiState(
                // Se i dati core (sleep o heart rate) ci sono, restiamo in loading finché _biometricReport non è calcolato (e refresh terminato)
                isLoading = refreshing && data.stepsHistory.isEmpty() && data.sleepHistory.isEmpty() 
                            || (!refreshing && allHeartRates.isNotEmpty() && biometric == null),

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
                    val cacheKey = "${session.id}_${session.hashCode()}_hr${hrData.size}"
                    sleepAnalysisCache.getOrPut(cacheKey) {
                        analyzeSleepQualityUseCase(session, hrData, baselineRhr)
                    }
                },
                
                sleepNights = sleepNights,
                isLoadingMoreSleep = isLoadingMore,
                biometricReport = biometric,
                isBiometricLoading = _isBiometricLoading.value
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
                
                // Clear the biometric report so it gets recalculated with fresh data
                _biometricReport.value = null
                
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
                val hrData = getHeartRatesForSession(allHeartRates, sess)
                val cacheKey = "${sess.id}_${sess.hashCode()}_hr${hrData.size}"
                sleepAnalysisCache.getOrPut(cacheKey) {
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
    
    // =========================================================================
    // Biometric Engine
    // =========================================================================
    
    /**
     * Runs the Biometric Engine pipeline on the available health data.
     * Computes TRIMP, Fitness-Fatigue, Z-Score, Dipping, Baevsky SI, and Readiness.
     *
     * Called automatically when health data updates. Runs on Dispatchers.Default
     * to avoid blocking the main thread with math-heavy computations.
     */
    private fun computeBiometrics(
        allHeartRates: List<HeartRateRec>,
        sleepSessions: List<SleepSessionRec>,
        baselineRhr: Int
    ) {
        if (allHeartRates.isEmpty() || _isBiometricLoading.value) return
        
        viewModelScope.launch {
            _isBiometricLoading.value = true
            try {
                val report = withContext(Dispatchers.Default) {
                    // Default profile (can be customized via user settings)
                    val profile = UserBiometricProfile(
                        hrMax = 190,    // Conservative default; ideally from user profile
                        hrRest = baselineRhr,
                        isMale = true   // Default; could come from User.gender
                    )
                    
                    // Build daily RHR history from the lowest nocturnal HR per day
                    val dailyRhrHistory = buildDailyRhrHistory(allHeartRates)
                    
                    // Get latest sleep analysis for dipping data
                    val latestSleep = sleepSessions.maxByOrNull { it.endTime }
                    var daytimeAvg: Double? = null
                    var nocturnalAvg: Double? = null
                    var nocturnalSamples: List<HrSample> = emptyList()
                    var sleepScore: Int? = null
                    
                    if (latestSleep != null) {
                        val hrForSession = getHeartRatesForSession(allHeartRates, latestSleep)
                        val sleepStart = latestSleep.startTime
                        val sleepEnd = latestSleep.endTime
                        
                        val daytimeHr = hrForSession.filter { 
                            it.time < sleepStart 
                        }
                        val nocturnalHr = hrForSession.filter { 
                            it.time >= sleepStart && it.time <= sleepEnd 
                        }
                        
                        if (daytimeHr.isNotEmpty()) {
                            daytimeAvg = daytimeHr.map { it.beatsPerMinute.toDouble() }.average()
                        }
                        if (nocturnalHr.isNotEmpty()) {
                            nocturnalAvg = nocturnalHr.map { it.beatsPerMinute.toDouble() }.average()
                            val baseMs = nocturnalHr.first().time.toEpochMilli()
                            nocturnalSamples = nocturnalHr.map {
                                HrSample(
                                    offsetMs = it.time.toEpochMilli() - baseMs,
                                    bpm = it.beatsPerMinute.toDouble()
                                )
                            }
                        }
                        // Get sleep quality score from cache or compute
                        val hrData = getHeartRatesForSession(allHeartRates, latestSleep)
                        val cacheKey = "${latestSleep.id}_${latestSleep.hashCode()}_hr${hrData.size}"
                        val analysis = sleepAnalysisCache[cacheKey]
                        sleepScore = analysis?.totalScore
                    }
                    
                    biometricEngineUseCase(
                        heartRateRecords = allHeartRates,
                        profile = profile,
                        sleepScore = sleepScore,
                        dailyRhrHistory = dailyRhrHistory,
                        daytimeAvgHr = daytimeAvg,
                        nocturnalAvgHr = nocturnalAvg,
                        nocturnalHrSamples = nocturnalSamples
                    )
                }
                _biometricReport.value = report
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isBiometricLoading.value = false
            }
        }
    }
    
    /**
     * Builds daily RHR values from the last 30 days of heart rate data.
     * Uses the lowest 10% of samples per day as a proxy for resting HR.
     */
    private fun buildDailyRhrHistory(heartRates: List<HeartRateRec>): List<Double> {
        val zone = ZoneId.systemDefault()
        return heartRates
            .groupBy { it.time.atZone(zone).toLocalDate() }
            .toSortedMap()
            .mapNotNull { (_, records) ->
                if (records.isEmpty()) return@mapNotNull null
                val sorted = records.sortedBy { it.beatsPerMinute }
                val lowestCount = (sorted.size * 0.1).toInt().coerceAtLeast(1)
                sorted.take(lowestCount).map { it.beatsPerMinute.toDouble() }.average()
            }
    }
}

