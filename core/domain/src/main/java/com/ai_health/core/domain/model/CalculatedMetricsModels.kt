package com.ai_health.core.domain.model

import java.time.Duration

data class SleepQualityResult(
    val totalScore: Int,             // Punteggio da 0 a 100
    val deepSleepDuration: Duration, // Durata sonno profondo
    val remSleepDuration: Duration,  // Durata REM
    val lightSleepDuration: Duration,// Durata sonno leggero
    val awakeDuration: Duration,     // Tempo sveglio
    val deepSleepPercentage: Int,    // % rispetto al totale
    val remSleepPercentage: Int,     // % rispetto al totale
    val feedback: String             // Es: "Ottimo recupero fisico!"
)