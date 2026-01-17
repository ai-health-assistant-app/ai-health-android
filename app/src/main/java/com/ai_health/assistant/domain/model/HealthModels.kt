package com.ai_health.assistant.domain.model

data class HeartRateRec(
    val beatsPerMinute: Long,
    val time: java.time.Instant
)

data class StepsRec(
    val count: Long,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val source: String
)

data class ExerciseSessionRec(
    val exerciseType: Int,      // ID del tipo di esercizio (es. RUNNING, WALKING)
    val title: String?,         // Titolo opzionale della sessione
    val notes: String?,         // Note opzionali
    val startTime: java.time.Instant,
    val endTime: java.time.Instant
)


data class OxygenSaturationRec(
    val percentage: Double,     // Es. 98.0
    val time: java.time.Instant
)


data class DistanceRec(
    val distanceMeters: Double, // Distanza in metri
    val startTime: java.time.Instant,
    val endTime: java.time.Instant
)


data class SleepStageRec(
    val stage: Int,             // Es: 1 (Light), 2 (Deep), 3 (REM), etc.
    val startTime: java.time.Instant,
    val endTime: java.time.Instant
)

data class SleepSessionRec(
    val title: String?,         // Spesso null, ma utile se l'app di origine mette un titolo
    val notes: String?,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val stages: List<SleepStageRec> = emptyList()
)

data class CaloriesRec(
    val energyKilocalories: Double,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant
)

data class BasalMetabolicRateRec(
    val energyKilocaloriesPerDay: Double,
    val time: java.time.Instant
)
