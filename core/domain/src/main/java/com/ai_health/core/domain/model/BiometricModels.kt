package com.ai_health.core.domain.model

/**
 * Lightweight HR sample from JSON format [{"o":offsetMs,"v":bpm}].
 */
data class HrSample(
    val offsetMs: Long,
    val bpm: Double
)

/**
 * User biometric profile needed for TRIMP and other calculations.
 */
data class UserBiometricProfile(
    val hrMax: Int,
    val hrRest: Int,
    val isMale: Boolean
) {
    /**
     * Estimates HRmax using Tanaka formula: 208 - 0.7 * age
     */
    companion object {
        fun estimateHrMax(age: Int): Int = (208 - 0.7 * age).toInt()
    }
}

// =============================================================================
// Phase 2: Training Load
// =============================================================================

/**
 * Result of Banister TRIMP calculation for a single session.
 */
data class TrimpResult(
    val trimp: Double,
    val durationMinutes: Double,
    val avgHrReserveFraction: Double
)

/**
 * Fitness-Fatigue model state (Performance Manager Chart).
 * CTL = Chronic Training Load ("Fitness"), 42-day EWMA
 * ATL = Acute Training Load ("Fatigue"), 7-day EWMA
 * TSB = Training Stress Balance = CTL - ATL ("Form")
 */
data class FitnessFatigueState(
    val ctl: Double,
    val atl: Double,
    val tsb: Double,
    val ctlHistory: List<Double> = emptyList(),
    val atlHistory: List<Double> = emptyList(),
    val tsbHistory: List<Double> = emptyList()
)

// =============================================================================
// Phase 3: Autonomic Health
// =============================================================================

enum class AlertLevel { GREEN, YELLOW, RED }

/**
 * Z-Score anomaly detection result for Resting Heart Rate.
 * Used for illness/overtraining early warning.
 */
data class RhrAnomalyResult(
    val zScore: Double,
    val mean30d: Double,
    val stdDev30d: Double,
    val todayRhr: Double,
    val alertLevel: AlertLevel
)

enum class DippingProfile {
    REVERSE_DIPPER,   // < 0% — pathological
    NON_DIPPER,       // 0-10% — suboptimal
    NORMAL_DIPPER,    // 10-20% — healthy
    EXTREME_DIPPER    // > 20%
}

/**
 * Sleep Dipping Ratio result.
 * Measures the physiological HR drop during sleep vs. daytime.
 */
data class SleepDippingResult(
    val dippingPercent: Double,
    val profile: DippingProfile
)

/**
 * Baevsky Stress Index approximation from HR distribution.
 * Higher values indicate stronger sympathetic control (more stress).
 */
data class BaevskyStressResult(
    val stressIndex: Double,
    val modeRR: Double,         // Mode in seconds (most frequent RR interval)
    val modeAmplitude: Double,  // % of samples in mode bin
    val variationRange: Double  // Max - Min RR interval in seconds
)

// =============================================================================
// Phase 4: Readiness Score
// =============================================================================

enum class ReadinessLevel { GREEN, YELLOW, RED }

/**
 * Breakdown of Readiness Score components.
 * Weights: sleep=40%, rhr=30%, training=20%, volatility=10%
 */
data class ReadinessBreakdown(
    val sleepComponent: Double,     // Max 40 pts
    val rhrComponent: Double,       // Max 30 pts
    val trainingComponent: Double,  // Max 20 pts
    val volatilityComponent: Double // Max 10 pts
)

/**
 * Final Readiness Score (0-100) with semaphoric level.
 * Answers: "How hard can I push today?"
 */
data class ReadinessResult(
    val score: Int,
    val level: ReadinessLevel,
    val breakdown: ReadinessBreakdown,
    val isOverridden: Boolean = false  // True if Z-Score forced a rest day
)

// =============================================================================
// Aggregated Report
// =============================================================================

/**
 * Complete biometric analysis report combining all engine outputs.
 * Nullable fields indicate insufficient data for that metric.
 */
data class BiometricReport(
    val trimpResult: TrimpResult?,
    val fitnessFatigue: FitnessFatigueState?,
    val rhrAnomaly: RhrAnomalyResult?,
    val sleepDipping: SleepDippingResult?,
    val baevskyStress: BaevskyStressResult?,
    val readiness: ReadinessResult?
)
