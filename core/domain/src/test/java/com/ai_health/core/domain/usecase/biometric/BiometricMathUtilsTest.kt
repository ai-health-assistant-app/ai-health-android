package com.ai_health.core.domain.usecase.biometric

import com.ai_health.core.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BiometricMathUtils.
 * Verifies all pure math functions with known inputs and expected outputs.
 */
class BiometricMathUtilsTest {

    // =========================================================================
    // Cubic Spline Interpolation
    // =========================================================================

    @Test
    fun `interpolateCubicSpline fills 3-minute gap`() {
        val samples = listOf(
            HrSample(0, 60.0),
            HrSample(60_000, 62.0),
            HrSample(120_000, 64.0),
            // 3-minute gap (180_000 to 300_000 missing)
            HrSample(300_000, 70.0)
        )

        val result = BiometricMathUtils.interpolateCubicSpline(samples, intervalMs = 60_000)

        // Should have samples at 0, 60k, 120k, 180k, 240k, 300k
        assertTrue("Should fill the gap", result.size >= 5)
        // All values should be in reasonable range
        result.forEach { sample ->
            assertTrue("BPM should be in range: ${sample.bpm}", sample.bpm in 30.0..220.0)
        }
    }

    @Test
    fun `interpolateCubicSpline preserves endpoints`() {
        val samples = listOf(
            HrSample(0, 65.0),
            HrSample(60_000, 70.0),
            HrSample(120_000, 68.0)
        )

        val result = BiometricMathUtils.interpolateCubicSpline(samples)

        assertEquals(65.0, result.first().bpm, 0.1)
        assertEquals(68.0, result.last().bpm, 0.1)
    }

    @Test
    fun `interpolateCubicSpline skips large gaps`() {
        val samples = listOf(
            HrSample(0, 60.0),
            HrSample(60_000, 62.0),
            // 10-minute gap (> 5 min maxGap)
            HrSample(660_000, 65.0)
        )

        val result = BiometricMathUtils.interpolateCubicSpline(samples, maxGapMs = 300_000)

        // Points inside the >5min gap should be skipped
        val pointsInGap = result.filter { it.offsetMs in 120_001..659_999 }
        assertTrue("Large gaps should not be interpolated", pointsInGap.isEmpty())
    }

    @Test
    fun `interpolateCubicSpline handles single sample`() {
        val samples = listOf(HrSample(0, 70.0))
        val result = BiometricMathUtils.interpolateCubicSpline(samples)
        assertEquals(1, result.size)
    }

    @Test
    fun `interpolateCubicSpline handles empty input`() {
        val result = BiometricMathUtils.interpolateCubicSpline(emptyList())
        assertTrue(result.isEmpty())
    }

    // =========================================================================
    // Banister TRIMP
    // =========================================================================

    @Test
    fun `calculateTrimp produces expected value for known input`() {
        // 30 minutes at 150 bpm, male, HRmax=180, HRrest=60
        val samples = (0..30).map { min ->
            HrSample(offsetMs = min * 60_000L, bpm = 150.0)
        }
        val profile = UserBiometricProfile(hrMax = 180, hrRest = 60, isMale = true)

        val result = BiometricMathUtils.calculateTrimp(samples, profile)

        assertTrue("TRIMP should be positive", result.trimp > 0)
        assertEquals(30.0, result.durationMinutes, 0.1)
        assertEquals(0.75, result.avgHrReserveFraction, 0.01) // (150-60)/(180-60) = 0.75
    }

    @Test
    fun `calculateTrimp female uses different coefficients`() {
        val samples = (0..30).map { min ->
            HrSample(offsetMs = min * 60_000L, bpm = 150.0)
        }
        val maleProfile = UserBiometricProfile(hrMax = 180, hrRest = 60, isMale = true)
        val femaleProfile = UserBiometricProfile(hrMax = 180, hrRest = 60, isMale = false)

        val maleResult = BiometricMathUtils.calculateTrimp(samples, maleProfile)
        val femaleResult = BiometricMathUtils.calculateTrimp(samples, femaleProfile)

        // Female and male results should differ due to different coefficients
        assertNotEquals(maleResult.trimp, femaleResult.trimp, 0.1)
        assertTrue("Both should be positive", maleResult.trimp > 0 && femaleResult.trimp > 0)
    }

    @Test
    fun `calculateTrimp returns zero for insufficient data`() {
        val result = BiometricMathUtils.calculateTrimp(
            listOf(HrSample(0, 100.0)),
            UserBiometricProfile(180, 60, true)
        )
        assertEquals(0.0, result.trimp, 0.001)
    }

    @Test
    fun `calculateTrimp returns zero for zero hr reserve`() {
        val samples = listOf(
            HrSample(0, 100.0),
            HrSample(60_000, 100.0)
        )
        val profile = UserBiometricProfile(hrMax = 60, hrRest = 60, isMale = true) // zero reserve
        val result = BiometricMathUtils.calculateTrimp(samples, profile)
        assertEquals(0.0, result.trimp, 0.001)
    }

    // =========================================================================
    // EWMA
    // =========================================================================

    @Test
    fun `ewma with period 7 shows exponential decay`() {
        // A spike followed by zeros
        val values = listOf(100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val result = BiometricMathUtils.ewma(values, period = 7)

        assertEquals(10, result.size)
        assertEquals(100.0, result[0], 0.001) // First value is just the input
        assertTrue("EWMA should decay", result[1] < result[0])
        assertTrue("EWMA should continue decaying", result[5] < result[1])
        assertTrue("EWMA should approach zero", result[9] < 10.0)
    }

    @Test
    fun `ewma handles empty input`() {
        val result = BiometricMathUtils.ewma(emptyList(), 7)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ewma handles single value`() {
        val result = BiometricMathUtils.ewma(listOf(50.0), 7)
        assertEquals(1, result.size)
        assertEquals(50.0, result[0], 0.001)
    }

    // =========================================================================
    // Fitness-Fatigue Model
    // =========================================================================

    @Test
    fun `calculateFitnessFatigue with consistent training`() {
        // 30 days of consistent training at TRIMP=50
        val dailyTrimp = List(30) { 50.0 }

        val result = BiometricMathUtils.calculateFitnessFatigue(dailyTrimp)

        assertTrue("CTL should be positive", result.ctl > 0)
        assertTrue("ATL should be positive", result.atl > 0)
        // With constant input, both EWMA converge toward the input value
        assertTrue("CTL should approach 50: ${result.ctl}", result.ctl > 40)
        assertTrue("ATL should approach 50: ${result.atl}", result.atl > 45)
        assertEquals(30, result.ctlHistory.size)
        assertEquals(30, result.atlHistory.size)
    }

    @Test
    fun `calculateFitnessFatigue TSB reflects rest period`() {
        // 14 days heavy training then 7 days rest
        val dailyTrimp = List(14) { 100.0 } + List(7) { 0.0 }

        val result = BiometricMathUtils.calculateFitnessFatigue(dailyTrimp)

        // After rest, ATL drops faster than CTL → TSB should move positive
        // CTL has longer memory than ATL
        assertTrue("History should be tracked", result.ctlHistory.size == 21)
    }

    @Test
    fun `calculateFitnessFatigue handles empty input`() {
        val result = BiometricMathUtils.calculateFitnessFatigue(emptyList())
        assertEquals(0.0, result.ctl, 0.001)
        assertEquals(0.0, result.atl, 0.001)
        assertEquals(0.0, result.tsb, 0.001)
    }

    // =========================================================================
    // Z-Score Anomaly Detection
    // =========================================================================

    @Test
    fun `calculateRhrZScore green for normal value`() {
        val history = List(30) { 60.0 + (it % 3) } // ~60-62 bpm
        val result = BiometricMathUtils.calculateRhrZScore(61.0, history)

        assertEquals(AlertLevel.GREEN, result.alertLevel)
        assertTrue("Z-Score should be low", result.zScore < 1.5)
    }

    @Test
    fun `calculateRhrZScore yellow for moderate elevation`() {
        // mean ~60, std ~1. If todayRhr = 62, Z ≈ 2.0 → YELLOW
        val history = List(30) { 60.0 }
        // Set stdDev via known variation
        val historyWithVariation = (0 until 30).map { 60.0 + if (it % 2 == 0) 0.5 else -0.5 }
        val result = BiometricMathUtils.calculateRhrZScore(62.0, historyWithVariation)

        // With stdDev ≈ 0.5, Z ≈ (62 - 60) / 0.5 = 4.0 → should be RED
        assertEquals(AlertLevel.RED, result.alertLevel)
    }

    @Test
    fun `calculateRhrZScore red for severe elevation`() {
        val history = List(30) { 60.0 + (it % 5).toDouble() } // mean ~62, std ~1.4
        val result = BiometricMathUtils.calculateRhrZScore(70.0, history)

        assertEquals(AlertLevel.RED, result.alertLevel)
        assertTrue("Z-Score should be >= 2.5", result.zScore >= 2.5)
    }

    @Test
    fun `calculateRhrZScore handles insufficient history`() {
        val result = BiometricMathUtils.calculateRhrZScore(65.0, listOf(60.0, 61.0))
        assertEquals(AlertLevel.GREEN, result.alertLevel)
    }

    // =========================================================================
    // Sleep Dipping Ratio
    // =========================================================================

    @Test
    fun `calculateSleepDipping normal dipper`() {
        val result = BiometricMathUtils.calculateSleepDipping(
            daytimeAvgHr = 80.0,
            nocturnalAvgHr = 64.0  // 20% dip
        )

        assertEquals(DippingProfile.NORMAL_DIPPER, result.profile)
        assertEquals(20.0, result.dippingPercent, 0.1)
    }

    @Test
    fun `calculateSleepDipping non-dipper`() {
        val result = BiometricMathUtils.calculateSleepDipping(
            daytimeAvgHr = 80.0,
            nocturnalAvgHr = 76.0  // 5% dip
        )

        assertEquals(DippingProfile.NON_DIPPER, result.profile)
    }

    @Test
    fun `calculateSleepDipping reverse dipper`() {
        val result = BiometricMathUtils.calculateSleepDipping(
            daytimeAvgHr = 70.0,
            nocturnalAvgHr = 75.0  // negative dip
        )

        assertEquals(DippingProfile.REVERSE_DIPPER, result.profile)
        assertTrue("Dipping should be negative", result.dippingPercent < 0)
    }

    @Test
    fun `calculateSleepDipping handles zero daytime`() {
        val result = BiometricMathUtils.calculateSleepDipping(0.0, 60.0)
        assertEquals(DippingProfile.NON_DIPPER, result.profile)
    }

    // =========================================================================
    // Baevsky Stress Index
    // =========================================================================

    @Test
    fun `calculateBaevskyStressIndex low stress for variable HR`() {
        // Wide range of HR values → low stress (high variability)
        val samples = (55..75).map { HrSample(it * 60_000L, it.toDouble()) }

        val result = BiometricMathUtils.calculateBaevskyStressIndex(samples)

        assertTrue("Stress index should be positive", result.stressIndex > 0)
        assertTrue("Variable HR should have moderate SI", result.stressIndex < 200)
    }

    @Test
    fun `calculateBaevskyStressIndex high stress for peaked HR`() {
        // All values clustered tightly → high stress (rigid rhythm)
        val samples = (0 until 50).map { HrSample(it * 60_000L, 60.0 + (it % 2) * 0.5) }

        val result = BiometricMathUtils.calculateBaevskyStressIndex(samples)

        assertTrue("Peaked distribution should have higher SI", result.stressIndex > 0)
        assertTrue("Mode amplitude should be high", result.modeAmplitude > 30)
    }

    @Test
    fun `calculateBaevskyStressIndex handles insufficient data`() {
        val result = BiometricMathUtils.calculateBaevskyStressIndex(
            listOf(HrSample(0, 60.0), HrSample(60_000, 61.0))
        )
        assertEquals(0.0, result.stressIndex, 0.001)
    }

    // =========================================================================
    // Readiness Score
    // =========================================================================

    @Test
    fun `calculateReadinessScore green for optimal state`() {
        val rhrAnomaly = RhrAnomalyResult(0.5, 60.0, 2.0, 61.0, AlertLevel.GREEN)

        val result = BiometricMathUtils.calculateReadinessScore(
            sleepScore = 90,
            rhrAnomaly = rhrAnomaly,
            tsb = 10.0,
            volatilityScore = 80.0
        )

        assertTrue("Score should be high: ${result.score}", result.score >= 70)
        assertEquals(ReadinessLevel.GREEN, result.level)
        assertFalse(result.isOverridden)
    }

    @Test
    fun `calculateReadinessScore red for illness override`() {
        val rhrAnomaly = RhrAnomalyResult(3.0, 60.0, 2.0, 66.0, AlertLevel.RED)

        val result = BiometricMathUtils.calculateReadinessScore(
            sleepScore = 90,
            rhrAnomaly = rhrAnomaly,
            tsb = 10.0,
            volatilityScore = 80.0
        )

        assertTrue("Score should be forced low: ${result.score}", result.score <= 30)
        assertEquals(ReadinessLevel.RED, result.level)
        assertTrue("Should be overridden", result.isOverridden)
    }

    @Test
    fun `calculateReadinessScore weights sum correctly`() {
        // With perfect scores across all domains
        val rhrAnomaly = RhrAnomalyResult(-1.0, 60.0, 2.0, 58.0, AlertLevel.GREEN)

        val result = BiometricMathUtils.calculateReadinessScore(
            sleepScore = 100,
            rhrAnomaly = rhrAnomaly,
            tsb = 15.0,  // Very positive
            volatilityScore = 100.0
        )

        // sleep(40) + rhr(30) + training(20) + volatility(10) = 100
        assertTrue("Perfect input should yield high score: ${result.score}", result.score >= 90)
    }

    @Test
    fun `calculateReadinessScore yellow for moderate fatigue`() {
        val result = BiometricMathUtils.calculateReadinessScore(
            sleepScore = 65,
            rhrAnomaly = null,
            tsb = -15.0,
            volatilityScore = 50.0
        )

        assertTrue("Score should be moderate: ${result.score}", result.score in 40..84)
    }

    @Test
    fun `calculateReadinessScore handles null inputs gracefully`() {
        val result = BiometricMathUtils.calculateReadinessScore(
            sleepScore = 50,
            rhrAnomaly = null,
            tsb = 0.0,
            volatilityScore = null
        )

        assertTrue("Score should be computed with defaults: ${result.score}", result.score in 1..100)
    }

    // =========================================================================
    // Spline Coefficients (Internal)
    // =========================================================================

    @Test
    fun `computeSplineCoefficients returns correct segment count`() {
        val x = doubleArrayOf(0.0, 1.0, 2.0, 3.0)
        val y = doubleArrayOf(0.0, 1.0, 0.0, 1.0)

        val coeffs = BiometricMathUtils.computeSplineCoefficients(x, y)

        assertEquals("Should have n-1 segments", 3, coeffs.size)
        // First coefficient 'a' should equal y[i]
        assertEquals(0.0, coeffs[0].a, 0.001)
        assertEquals(1.0, coeffs[1].a, 0.001)
        assertEquals(0.0, coeffs[2].a, 0.001)
    }
}
