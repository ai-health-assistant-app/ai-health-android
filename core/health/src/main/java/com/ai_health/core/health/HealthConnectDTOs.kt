package com.ai_health.core.health

/**
 * These DTOs (Data Transfer Objects) serve to decouple
 * the Health Connect manager from the local Database.
 * 
 * PRIVACY-PROOF ARCHITECTURE:
 * - deviceType field enables trusted device filtering
 * - RawHeartRate uses offset-based samples for space efficiency
 * - HealthChange sealed class supports Changes API differential sync
 */

// ============================================
// CHANGES API TYPES (Differential Sync)
// ============================================

/**
 * Rappresenta un cambiamento rilevato dalla Changes API di Health Connect.
 */
sealed class HealthChange {
    /**
     * Un record è stato inserito o aggiornato.
     */
    data class Upsert(
        val recordType: String,   // "STEPS", "HEART_RATE", "SLEEP", etc.
        val recordId: String,     // metadata.id del record
        val data: Any             // Il DTO corrispondente (RawStep, RawHeartRate, etc.)
    ) : HealthChange()
    
    /**
     * Un record è stato cancellato dall'utente in Health Connect.
     * L'app deve cancellare fisicamente questo record dal database locale.
     */
    data class Deletion(
        val recordType: String,
        val recordId: String
    ) : HealthChange()
}

/**
 * Risultato di una chiamata a getChanges().
 */
data class HealthChangesResult(
    val changes: List<HealthChange>,
    val nextToken: String,
    val hasMore: Boolean,         // Se true, ci sono altre pagine da consumare
    val tokenExpired: Boolean     // Se true, bisogna richiedere un nuovo token e fare cold-start sync
)

// ============================================
// DATA RECORD TYPES
// ============================================

data class RawStep(
    val id: String,
    val count: Long,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val deviceType: String? = null  // "WATCH", "FITNESS_BAND", "PHONE", "UNKNOWN"
)

/**
 * RawHeartRate - Ottimizzato per dati ad alta frequenza.
 * 
 * I campioni usano offsetMs relativo a startTime per risparmiare spazio.
 * Formato JSON risultante: [{"o":0,"v":72},{"o":1000,"v":74},...]
 */
data class RawHeartRate(
    val id: String,
    val samples: List<RawHeartRateSample>,  // Campioni offset-based
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val deviceType: String? = null
)

/**
 * Singolo campione HR.
 * offsetMs: millisecondi da RawHeartRate.startTime
 * bpm: battiti per minuto
 */
data class RawHeartRateSample(
    val offsetMs: Int,
    val bpm: Int
)

data class RawSleepStage(
    val stage: Int,
    val startTime: Long,
    val endTime: Long
)

data class RawSleep(
    val id: String,
    val durationMinutes: Double,
    val stage: Int, // Deprecated or for summary? Kept for compatibility
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val stages: List<RawSleepStage> = emptyList(),
    val deviceType: String? = null
)

data class RawDistance(
    val id: String,
    val distanceMeters: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val deviceType: String? = null
)

data class RawCalories(
    val id: String,
    val kilocalories: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val deviceType: String? = null
)

data class RawOxygen(
    val id: String,
    val percentage: Double, 
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val deviceType: String? = null
)

data class RawExercise(
    val id: String,
    val type: String, 
    val durationMinutes: Double,
    val title: String? = null,
    val notes: String? = null,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val deviceType: String? = null
)

data class RawBMR(
    val id: String,
    val kcalPerDay: Double,
    val startTime: Long,
    val endTime: Long,
    val sourcePackage: String,
    val deviceType: String? = null
)

