package com.ai_health.core.domain.model

import java.time.Duration

/**
 * Risultato completo dell'analisi della qualità del sonno.
 * Combina architettura del sonno con analisi emodinamica (HR).
 */
data class SleepQualityResult(
    val totalScore: Int,                      // Punteggio da 0 a 100
    val breakdown: ScoreBreakdown? = null,    // Punteggi parziali dei 4 domini
    val metrics: SleepMetrics? = null,        // Metriche fisiologiche dettagliate
    val deepSleepDuration: Duration,          // Durata sonno profondo
    val remSleepDuration: Duration,           // Durata REM
    val lightSleepDuration: Duration,         // Durata sonno leggero
    val awakeDuration: Duration,              // Tempo sveglio
    val deepSleepPercentage: Int,             // % rispetto al totale
    val remSleepPercentage: Int,              // % rispetto al totale
    val feedback: String,                     // Feedback principale
    val feedbackList: List<String> = listOf(feedback),  // Feedback multipli
    val dataQualityWarning: Boolean = false   // True se analisi basata su dati low-res
)

/**
 * Breakdown dei punteggi parziali per i 4 domini dell'algoritmo.
 * Somma = 100 punti massimi.
 */
data class ScoreBreakdown(
    val architectureScore: Double,  // Max 40 - Sleep Architecture & Continuity
    val dippingScore: Double,       // Max 30 - Nocturnal HR Dipping
    val rhrScore: Double,           // Max 20 - RHR Analysis
    val timingScore: Double         // Max 10 - Sleep Timing & Nadir
)

/**
 * Metriche fisiologiche dettagliate calcolate durante l'analisi.
 * Nullable quando i dati HR non sono disponibili.
 */
data class SleepMetrics(
    val totalSleepDurationMin: Long,
    val wasoMinutes: Long,                    // Wake After Sleep Onset
    val fragmentationIndex: Double,           // Transizioni/ora
    val dippingPercent: Double?,              // HR dipping % (null se no dati HR)
    val nocturnalHrAvg: Int?,                 // Media HR notturna
    val daytimeHrAvg: Int?,                   // Media HR diurna (16h pre-sonno)
    val lowestNocturnalHr: Int?,              // HR minima notturna (Nadir)
    val hrNadirOffsetPercent: Int?,           // Posizione nadir (0-100% della notte)
    val dataQualityScore: Double,             // Affidabilità dati HR (0.0 - 1.0)
    val isLowResolution: Boolean = false      // True se dati scarsi (es. Xiaomi 30min)
)