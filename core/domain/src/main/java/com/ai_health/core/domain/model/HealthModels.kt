package com.ai_health.core.domain.model

data class HeartRateRec(
    val source: String,
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
    val source: String,
    val exerciseType: String,      // ID del tipo di esercizio (es. RUNNING, WALKING)
    val title: String?,         // Titolo opzionale della sessione
    val notes: String?,         // Note opzionali
    val startTime: java.time.Instant,
    val endTime: java.time.Instant
)


data class OxygenSaturationRec(
    val source: String,
    val percentage: Double,     // Es. 98.0
    val time: java.time.Instant
)


data class DistanceRec(
    // Distanza in metri
    val source: String,
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
    val source: String,
    val title: String?,         // Spesso null, ma utile se l'app di origine mette un titolo
    val notes: String?,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val stages: List<SleepStageRec> = emptyList()
)

data class CaloriesRec(
    val source: String,
    val energyKilocalories: Double,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant
)

data class BasalMetabolicRateRec(
    val source: String,
    val energyKilocaloriesPerDay: Double,
    val time: java.time.Instant
)
