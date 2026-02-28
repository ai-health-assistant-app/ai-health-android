package com.ai_health.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_health.core.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.ai_health.core.data.local.dao.ChatMessageDao
import com.ai_health.core.data.local.entity.ChatMessageEntity
import com.ai_health.core.domain.model.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.time.ZoneId
import com.ai_health.core.domain.repository.HealthRepository
import com.ai_health.core.domain.usecase.biometric.BiometricEngineUseCase
import com.ai_health.core.domain.usecase.sleep.AnalyzeSleepQualityUseCase

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageDao: ChatMessageDao,
    private val healthRepository: HealthRepository,
    private val biometricEngineUseCase: BiometricEngineUseCase,
    private val analyzeSleepQualityUseCase: AnalyzeSleepQualityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Cache per i dati biometrici
    private var cachedBiometricReport: BiometricReport? = null
    private var cachedSleepAnalysis: SleepQualityResult? = null

    init {
        loadHistory()
        observeHealthData()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            chatMessageDao.getAllMessages().collect { entities ->
                if (entities.isEmpty()) {
                    val initialMsg = ChatMessageEntity(role = "assistant", content = "Ciao! Sono il tuo assistente AI per la salute. Come posso aiutarti oggi?")
                    chatMessageDao.insertMessage(initialMsg)
                } else {
                    val messages = entities.map { 
                        ChatMessage(id = it.id.toString(), text = it.content, isUser = it.role == "user", timestamp = it.timestamp) 
                    }
                    _uiState.update { it.copy(messages = messages) }
                }
            }
        }
    }

    private fun observeHealthData() {
        viewModelScope.launch {
            // Aumenta a window piena per RHR analysis (simula come Dashboard)
            val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
            val sleepFlow = healthRepository.getSleepHistory(thirtyDaysAgo)
            val hrFlow = healthRepository.getHeartRateHistory(thirtyDaysAgo)

            combine(sleepFlow, hrFlow) { sleepSessions, heartRates ->
                // Manteniamo una logica semplificata e fluida senza bloccare la UI
                withContext(Dispatchers.Default) {
                    try {
                        val latestSleep = sleepSessions.maxByOrNull { it.endTime }
                        
                        // Calcolo Baseline RHR (simile alla dashboard)
                        val baselineRhr = calculateBaselineRhr(heartRates) 

                        var daytimeAvg: Double? = null
                        var nocturnalAvg: Double? = null
                        var nocturnalSamples: List<HrSample> = emptyList()

                        if (latestSleep != null) {
                            val sleepStart = latestSleep.startTime
                            val sleepEnd = latestSleep.endTime

                            val hrForSession = heartRates.filter {
                                it.time >= sleepStart.minus(16, ChronoUnit.HOURS) && it.time <= sleepEnd
                            }

                            val sleepHrSamples = heartRates.filter { it.time >= sleepStart && it.time <= sleepEnd }
                            cachedSleepAnalysis = analyzeSleepQualityUseCase(latestSleep, sleepHrSamples, baselineRhr)

                            val daytimeHr = hrForSession.filter { it.time < sleepStart }
                            val nocturnalHr = hrForSession.filter { it.time in sleepStart..sleepEnd }
                            
                            if (daytimeHr.isNotEmpty()) daytimeAvg = daytimeHr.map { it.beatsPerMinute.toDouble() }.average()
                            if (nocturnalHr.isNotEmpty()) {
                                nocturnalAvg = nocturnalHr.map { it.beatsPerMinute.toDouble() }.average()
                                val baseMs = nocturnalHr.first().time.toEpochMilli()
                                nocturnalSamples = nocturnalHr.map {
                                    HrSample(offsetMs = it.time.toEpochMilli() - baseMs, bpm = it.beatsPerMinute.toDouble())
                                }
                            }
                        }

                        // Calcolo Biometrico Avanzato per il Chat Payload
                        // Costruiremo il report se ci sono dati base HR registrati
                        if (heartRates.isNotEmpty()) {
                            val dailyRhrHistory = buildDailyRhrHistory(heartRates)

                            cachedBiometricReport = biometricEngineUseCase(
                                heartRateRecords = heartRates,
                                profile = com.ai_health.core.domain.model.UserBiometricProfile(190, baselineRhr, true),
                                sleepScore = cachedSleepAnalysis?.totalScore,
                                dailyRhrHistory = dailyRhrHistory,
                                daytimeAvgHr = daytimeAvg,
                                nocturnalAvgHr = nocturnalAvg,
                                nocturnalHrSamples = nocturnalSamples
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.collect {
                // Background compute complete, metrics are in cache variable.
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Salva il messaggio utente in locale (UI si auto-aggiorna via Flow)
        viewModelScope.launch {
            chatMessageDao.insertMessage(ChatMessageEntity(role = "user", content = text))
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 2. Prepara la History (Ultime 6 interazioni, es. 3 user e 3 assistant recenti)
            val historyEntities = chatMessageDao.getLastMessages(limit = 6).reversed()
            val messageDomains = historyEntities.map { 
                ChatMessageDomain(role = it.role, content = it.content) 
            }

            // 3. Usa le metriche reali cachate (se non ci sono ancora pronti setta null)
            val readinessDomain = cachedBiometricReport?.readiness?.score?.let { ReadinessContextDomain(it) }
            
            val sleepDomain = cachedSleepAnalysis?.let { sa ->
                SleepContextDomain(
                    score = sa.totalScore,
                    deepSleepDurationMin = sa.deepSleepDuration.toMinutes(),
                    remSleepDurationMin = sa.remSleepDuration.toMinutes(),
                    totalDurationMin = sa.metrics?.totalSleepDurationMin ?: 0
                )
            }

            val stressDomain = cachedBiometricReport?.baevskyStress?.stressIndex?.let { StressContextDomain(it) }
            val trainingLoadDomain = cachedBiometricReport?.fitnessFatigue?.let { ff ->
                TrainingLoadContextDomain(tsbForm = ff.tsb, fatigueAtl = ff.atl)
            }

            val contextDomain = ChatContextDomain(
                readiness = readinessDomain,
                sleep = sleepDomain,
                stress = stressDomain,
                trainingLoad = trainingLoadDomain
            )

            // 4. Invia la richiesta
            val request = ChatRequestDomain(context = contextDomain, messages = messageDomains)

            chatRepository.sendMessage(request)
                .onSuccess { response ->
                    chatMessageDao.insertMessage(ChatMessageEntity(role = "assistant", content = response))
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    // Mostra errore effimero
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Errore di connessione") }
                    chatMessageDao.insertMessage(ChatMessageEntity(role = "assistant", content = "Scusa, ho avuto un problema di connessione. Riprova più tardi."))
                }
        }
    }

    /** Clear all chat messages from Room and reset UI state */
    fun clearChat() {
        viewModelScope.launch {
            chatMessageDao.clearHistory()
            // The Flow from loadHistory will automatically pick up the empty state
            // and re-insert the welcome message
        }
    }

    // Metodo addMessage rimosso perché la UI è guidata dal Flow del Database Room

    private fun calculateBaselineRhr(heartRates: List<HeartRateRec>): Int {
        if (heartRates.isEmpty()) return 60
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
        val recentHr = heartRates.filter { it.time >= sevenDaysAgo }
        if (recentHr.isEmpty()) return 60
        val sorted = recentHr.sortedBy { it.beatsPerMinute }
        val lowestCount = (sorted.size * 0.1).toInt().coerceAtLeast(1)
        return sorted.take(lowestCount).map { it.beatsPerMinute }.average().toInt().coerceIn(40, 100)
    }

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
