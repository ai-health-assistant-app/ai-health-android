package com.ai_health.core.domain.model

data class HeartRateRec(
    val id: String,
    val beatsPerMinute: Long,
    val time: java.time.Instant,
    val source: String
)

data class StepsRec(
    val id: String,
    val count: Long,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val source: String
)

data class ExerciseSessionRec(
    val id: String,
    val title: String?,         // Titolo opzionale della sessione
    val exerciseType: String,      // ID del tipo di esercizio (es. RUNNING, WALKING)
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val notes: String?,         // Note opzionali
    val source: String
)


data class OxygenSaturationRec(
    val id: String,
    val percentage: Double,     // Es. 98.0
    val time: java.time.Instant,
    val source: String
)


data class DistanceRec(
    val id: String,
    val distanceMeters: Double, // Distanza in metri
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val source: String
)


data class SleepStageRec(
    val id: String,
    val stage: Int,             // Es: 1 (Light), 2 (Deep), 3 (REM), etc.
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val source: String
)

data class SleepSessionRec(
    val id: String,
    val title: String?,         // Spesso null, ma utile se l'app di origine mette un titolo
    val notes: String?,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val stages: List<SleepStageRec> = emptyList(),
    val source: String
)

data class CaloriesRec(
    val id: String,
    val energyKilocalories: Double,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val source: String
)

data class BasalMetabolicRateRec(
    val id: String,
    val energyKilocaloriesPerDay: Double,
    val time: java.time.Instant,
    val source: String
)
