package com.ai_health.core.domain.usecase.biometric

import com.ai_health.core.domain.model.*
import javax.inject.Inject

/**
 * Orchestrator Use Case that runs the full Biometric Engine pipeline.
 *
 * Pipeline: HeartRateRec → HrSample → Interpolate → TRIMP → Fitness-Fatigue
 *           → Z-Score → Dipping → Baevsky → Readiness
 *
 * Handles missing/empty data gracefully by returning null for sub-results
 * that can't be computed due to insufficient data.
 */
class BiometricEngineUseCase @Inject constructor() {

    /**
     * Runs the full biometric analysis.
     *
     * @param heartRateRecords All HR records from HealthConnect, sorted by time
     * @param profile User's biometric profile
     * @param sleepScore Sleep quality score from AnalyzeSleepQualityUseCase (0-100, null if unavailable)
     * @param dailyRhrHistory RHR values for the past 30 days (chronological)
     * @param daytimeAvgHr Average daytime HR (for dipping calculation)
     * @param nocturnalAvgHr Average nocturnal HR (for dipping calculation)
     * @param nocturnalHrSamples HR samples during deep sleep (for Baevsky SI)
     * @param dailyTrimpHistory Daily TRIMP totals (chronological, for fitness-fatigue)
     * @param volatilityScore Nocturnal HR volatility score (0-100)
     */
    operator fun invoke(
        heartRateRecords: List<HeartRateRec>,
        profile: UserBiometricProfile,
        sleepScore: Int? = null,
        dailyRhrHistory: List<Double> = emptyList(),
        daytimeAvgHr: Double? = null,
        nocturnalAvgHr: Double? = null,
        nocturnalHrSamples: List<HrSample> = emptyList(),
        dailyTrimpHistory: List<Double> = emptyList(),
        volatilityScore: Double? = null
    ): BiometricReport {

        // --- Step 1: Convert HeartRateRec → HrSample and interpolate ---
        val rawSamples = convertToHrSamples(heartRateRecords)
        val interpolatedSamples = if (rawSamples.size >= 2) {
            BiometricMathUtils.interpolateCubicSpline(rawSamples)
        } else {
            rawSamples
        }

        // --- Step 2: Calculate Session TRIMP ---
        val trimpResult = if (interpolatedSamples.size >= 2) {
            BiometricMathUtils.calculateTrimp(interpolatedSamples, profile)
        } else {
            null
        }

        // --- Step 3: Fitness-Fatigue Model ---
        // Build up daily TRIMP history including today's session
        val updatedDailyTrimp = if (trimpResult != null && trimpResult.trimp > 0) {
            dailyTrimpHistory + trimpResult.trimp
        } else if (dailyTrimpHistory.isNotEmpty()) {
            dailyTrimpHistory + 0.0  // Rest day
        } else {
            emptyList()
        }

        val fitnessFatigue = if (updatedDailyTrimp.size >= 2) {
            BiometricMathUtils.calculateFitnessFatigue(updatedDailyTrimp)
        } else {
            null
        }

        // --- Step 4: Z-Score Anomaly Detection ---
        val todayRhr = dailyRhrHistory.lastOrNull()
        val rhrAnomaly = if (todayRhr != null && dailyRhrHistory.size >= 3) {
            BiometricMathUtils.calculateRhrZScore(todayRhr, dailyRhrHistory)
        } else {
            null
        }

        // --- Step 5: Sleep Dipping Ratio ---
        val sleepDipping = if (daytimeAvgHr != null && nocturnalAvgHr != null && daytimeAvgHr > 0) {
            BiometricMathUtils.calculateSleepDipping(daytimeAvgHr, nocturnalAvgHr)
        } else {
            null
        }

        // --- Step 6: Baevsky Stress Index ---
        val baevskyStress = if (nocturnalHrSamples.size >= 5) {
            BiometricMathUtils.calculateBaevskyStressIndex(nocturnalHrSamples)
        } else {
            null
        }

        // --- Step 7: Readiness Score ---
        val readiness = if (sleepScore != null) {
            val tsb = fitnessFatigue?.tsb ?: 0.0
            BiometricMathUtils.calculateReadinessScore(
                sleepScore = sleepScore,
                rhrAnomaly = rhrAnomaly,
                tsb = tsb,
                volatilityScore = volatilityScore
            )
        } else {
            null
        }

        return BiometricReport(
            trimpResult = trimpResult,
            fitnessFatigue = fitnessFatigue,
            rhrAnomaly = rhrAnomaly,
            sleepDipping = sleepDipping,
            baevskyStress = baevskyStress,
            readiness = readiness
        )
    }

    /**
     * Converts HealthConnect HeartRateRec records to HrSamples.
     * Uses the earliest timestamp as offset 0.
     */
    private fun convertToHrSamples(records: List<HeartRateRec>): List<HrSample> {
        if (records.isEmpty()) return emptyList()

        val sorted = records.sortedBy { it.time }
        val baseTime = sorted.first().time.toEpochMilli()

        return sorted.map { rec ->
            HrSample(
                offsetMs = rec.time.toEpochMilli() - baseTime,
                bpm = rec.beatsPerMinute.toDouble()
            )
        }
    }
}
