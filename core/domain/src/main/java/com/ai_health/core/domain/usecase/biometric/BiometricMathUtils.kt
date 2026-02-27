package com.ai_health.core.domain.usecase.biometric

import com.ai_health.core.domain.model.*
import kotlin.math.*

/**
 * Pure mathematical functions for the Biometric Engine.
 *
 * All functions are stateless and side-effect free, making them
 * fully unit-testable without Android dependencies.
 *
 * References: algoritmo_analisi_battiti.md (Chapters 2, 4, 6, Appendix)
 */
object BiometricMathUtils {

    // =========================================================================
    // Phase 1: Pre-processing — Cubic Spline Interpolation
    // =========================================================================

    /**
     * Natural Cubic Spline interpolation to fill gaps in HR time series.
     *
     * Reconstructs a smooth, physiologically plausible HR curve from
     * irregularly-sampled data. Only fills gaps ≤ [maxGapMs] (default 5 min).
     *
     * @param samples Raw HR samples sorted by offsetMs
     * @param intervalMs Target output interval (default 60000ms = 1 min)
     * @param maxGapMs Maximum gap to interpolate (default 300000ms = 5 min)
     * @return Uniformly-spaced HR samples
     */
    fun interpolateCubicSpline(
        samples: List<HrSample>,
        intervalMs: Long = 60_000L,
        maxGapMs: Long = 300_000L
    ): List<HrSample> {
        if (samples.size < 2) return samples.toList()

        val sorted = samples.sortedBy { it.offsetMs }
        val n = sorted.size
        val x = DoubleArray(n) { sorted[it].offsetMs.toDouble() }
        val y = DoubleArray(n) { sorted[it].bpm }

        // Compute natural cubic spline coefficients
        val coefficients = computeSplineCoefficients(x, y)

        // Generate uniformly-spaced output
        val result = mutableListOf<HrSample>()
        val startMs = sorted.first().offsetMs
        val endMs = sorted.last().offsetMs
        var t = startMs

        while (t <= endMs) {
            val td = t.toDouble()

            // Find which segment this t falls into
            val segIdx = findSegmentIndex(x, td)

            // Check if we're in a gap too large to interpolate
            val gapSize = (x[segIdx + 1] - x[segIdx]).toLong()
            if (gapSize > maxGapMs) {
                // Skip points inside large gaps
                t += intervalMs
                continue
            }

            val (a, b, c, d) = coefficients[segIdx]
            val dx = td - x[segIdx]
            val interpolatedBpm = a + b * dx + c * dx * dx + d * dx * dx * dx

            result.add(HrSample(offsetMs = t, bpm = interpolatedBpm.coerceIn(30.0, 220.0)))
            t += intervalMs
        }

        return result
    }

    /**
     * Computes natural cubic spline coefficients for each segment.
     * Returns list of (a, b, c, d) quadruples for the polynomial:
     *   S_i(x) = a_i + b_i*(x-x_i) + c_i*(x-x_i)^2 + d_i*(x-x_i)^3
     */
    internal fun computeSplineCoefficients(
        x: DoubleArray,
        y: DoubleArray
    ): List<SplineCoefficients> {
        val n = x.size - 1  // number of segments
        if (n < 1) return emptyList()

        val h = DoubleArray(n) { x[it + 1] - x[it] }
        val alpha = DoubleArray(n + 1)

        for (i in 1 until n) {
            alpha[i] = (3.0 / h[i]) * (y[i + 1] - y[i]) -
                       (3.0 / h[i - 1]) * (y[i] - y[i - 1])
        }

        // Tridiagonal system solver
        val l = DoubleArray(n + 1)
        val mu = DoubleArray(n + 1)
        val z = DoubleArray(n + 1)
        val c = DoubleArray(n + 1)

        l[0] = 1.0
        mu[0] = 0.0
        z[0] = 0.0

        for (i in 1 until n) {
            l[i] = 2.0 * (x[i + 1] - x[i - 1]) - h[i - 1] * mu[i - 1]
            mu[i] = h[i] / l[i]
            z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i]
        }

        l[n] = 1.0
        z[n] = 0.0
        c[n] = 0.0

        val b = DoubleArray(n)
        val d = DoubleArray(n)

        for (j in n - 1 downTo 0) {
            c[j] = z[j] - mu[j] * c[j + 1]
            b[j] = (y[j + 1] - y[j]) / h[j] - h[j] * (c[j + 1] + 2.0 * c[j]) / 3.0
            d[j] = (c[j + 1] - c[j]) / (3.0 * h[j])
        }

        return (0 until n).map { i ->
            SplineCoefficients(a = y[i], b = b[i], c = c[i], d = d[i])
        }
    }

    data class SplineCoefficients(val a: Double, val b: Double, val c: Double, val d: Double)

    private fun findSegmentIndex(x: DoubleArray, t: Double): Int {
        var lo = 0
        var hi = x.size - 2
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (x[mid] <= t) lo = mid else hi = mid - 1
        }
        return lo.coerceIn(0, x.size - 2)
    }

    // =========================================================================
    // Phase 2: Training Load — Banister TRIMP
    // =========================================================================

    /**
     * Calculates the Banister exponential TRIMP for a session.
     *
     * Formula (§2.1.1):
     *   ΔHRr = (HR - HRrest) / (HRmax - HRrest)
     *   Male:   y = 0.64 * e^(1.92 * ΔHRr)
     *   Female: y = 0.86 * e^(1.67 * ΔHRr)
     *   TRIMP = Σ (Δt_minutes * ΔHRr * y)
     *
     * @param samples Interpolated HR samples with uniform spacing
     * @param profile User's biometric profile (hrMax, hrRest, sex)
     * @return TrimpResult with session TRIMP and metadata
     */
    fun calculateTrimp(
        samples: List<HrSample>,
        profile: UserBiometricProfile
    ): TrimpResult {
        if (samples.size < 2) return TrimpResult(0.0, 0.0, 0.0)

        val hrReserve = (profile.hrMax - profile.hrRest).toDouble()
        if (hrReserve <= 0) return TrimpResult(0.0, 0.0, 0.0)

        val sorted = samples.sortedBy { it.offsetMs }
        var totalTrimp = 0.0
        var sumHrr = 0.0
        var count = 0

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]

            val deltaMinutes = (next.offsetMs - current.offsetMs) / 60_000.0
            if (deltaMinutes <= 0 || deltaMinutes > 5.0) continue // skip invalid/large gaps

            val hrr = ((current.bpm - profile.hrRest) / hrReserve).coerceIn(0.0, 1.0)

            val y = if (profile.isMale) {
                0.64 * exp(1.92 * hrr)
            } else {
                0.86 * exp(1.67 * hrr)
            }

            totalTrimp += deltaMinutes * hrr * y
            sumHrr += hrr
            count++
        }

        val totalDuration = (sorted.last().offsetMs - sorted.first().offsetMs) / 60_000.0
        val avgHrr = if (count > 0) sumHrr / count else 0.0

        return TrimpResult(
            trimp = totalTrimp,
            durationMinutes = totalDuration,
            avgHrReserveFraction = avgHrr
        )
    }

    // =========================================================================
    // Phase 2: Fitness-Fatigue Model (Performance Manager Chart)
    // =========================================================================

    /**
     * Generic Exponential Weighted Moving Average.
     *   α = 2 / (period + 1)
     *   EWMA_n = α * x_n + (1 - α) * EWMA_{n-1}
     *
     * @param values Daily values (e.g., daily TRIMP)
     * @param period Smoothing period in days
     * @return List of EWMA values, same length as input
     */
    fun ewma(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()

        val alpha = 2.0 / (period + 1)
        val result = mutableListOf<Double>()
        var current = values.first()
        result.add(current)

        for (i in 1 until values.size) {
            current = alpha * values[i] + (1 - alpha) * current
            result.add(current)
        }
        return result
    }

    /**
     * Computes the Fitness-Fatigue model from daily TRIMP values.
     *
     * CTL ("Fitness") = EWMA with period 42 days
     * ATL ("Fatigue") = EWMA with period 7 days
     * TSB ("Form")    = CTL - ATL
     *
     * TSB interpretation (§2.3):
     *   > +5   → Fresh, ready for performance
     *   -10..−30 → Productive training load
     *   < −30  → High risk of overtraining
     *
     * @param dailyTrimp List of daily TRIMP totals (chronological)
     * @param ctlPeriod CTL smoothing period (default 42 days)
     * @param atlPeriod ATL smoothing period (default 7 days)
     */
    fun calculateFitnessFatigue(
        dailyTrimp: List<Double>,
        ctlPeriod: Int = 42,
        atlPeriod: Int = 7
    ): FitnessFatigueState {
        if (dailyTrimp.isEmpty()) {
            return FitnessFatigueState(0.0, 0.0, 0.0)
        }

        val ctlValues = ewma(dailyTrimp, ctlPeriod)
        val atlValues = ewma(dailyTrimp, atlPeriod)
        val tsbValues = ctlValues.zip(atlValues) { c, a -> c - a }

        return FitnessFatigueState(
            ctl = ctlValues.last(),
            atl = atlValues.last(),
            tsb = tsbValues.last(),
            ctlHistory = ctlValues,
            atlHistory = atlValues,
            tsbHistory = tsbValues
        )
    }

    // =========================================================================
    // Phase 3: Autonomic Health — Z-Score for RHR Anomaly Detection
    // =========================================================================

    /**
     * Calculates Z-Score for today's RHR against a 30-day baseline.
     *
     * Algorithm (§4.2.1):
     *   Z = (RHR_today - μ_30d) / σ_30d
     *
     * Alert thresholds:
     *   Z < 1.5       → GREEN (Normal)
     *   1.5 ≤ Z < 2.5 → YELLOW (Attention: stress, alcohol, infection onset)
     *   Z ≥ 2.5       → RED (Alarm: high probability of illness)
     */
    fun calculateRhrZScore(
        todayRhr: Double,
        rhrHistory30d: List<Double>
    ): RhrAnomalyResult {
        if (rhrHistory30d.size < 3) {
            return RhrAnomalyResult(
                zScore = 0.0,
                mean30d = todayRhr,
                stdDev30d = 0.0,
                todayRhr = todayRhr,
                alertLevel = AlertLevel.GREEN
            )
        }

        val mean = rhrHistory30d.average()
        val variance = rhrHistory30d.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance).coerceAtLeast(0.5) // avoid division by zero

        val zScore = (todayRhr - mean) / stdDev

        val alertLevel = when {
            zScore < 1.5 -> AlertLevel.GREEN
            zScore < 2.5 -> AlertLevel.YELLOW
            else -> AlertLevel.RED
        }

        return RhrAnomalyResult(
            zScore = zScore,
            mean30d = mean,
            stdDev30d = stdDev,
            todayRhr = todayRhr,
            alertLevel = alertLevel
        )
    }

    // =========================================================================
    // Phase 3: Autonomic Health — Sleep Dipping Ratio
    // =========================================================================

    /**
     * Calculates the nocturnal HR "dipping" ratio.
     *
     * Formula (§4.3):
     *   Dip% = ((HR_daytime - HR_nocturnal) / HR_daytime) * 100
     *
     * Profiles:
     *   < 0%    → REVERSE_DIPPER (pathological)
     *   0-10%   → NON_DIPPER (suboptimal, cardiovascular risk)
     *   10-20%  → NORMAL_DIPPER (healthy)
     *   > 20%   → EXTREME_DIPPER
     */
    fun calculateSleepDipping(
        daytimeAvgHr: Double,
        nocturnalAvgHr: Double
    ): SleepDippingResult {
        if (daytimeAvgHr <= 0) {
            return SleepDippingResult(0.0, DippingProfile.NON_DIPPER)
        }

        val dip = ((daytimeAvgHr - nocturnalAvgHr) / daytimeAvgHr) * 100.0

        val profile = when {
            dip < 0.0 -> DippingProfile.REVERSE_DIPPER
            dip < 10.0 -> DippingProfile.NON_DIPPER
            dip <= 20.0 -> DippingProfile.NORMAL_DIPPER
            else -> DippingProfile.EXTREME_DIPPER
        }

        return SleepDippingResult(
            dippingPercent = dip,
            profile = profile
        )
    }

    // =========================================================================
    // Phase 3: Autonomic Health — Baevsky Stress Index Approximation
    // =========================================================================

    /**
     * Approximates the Baevsky Stress Index from HR samples.
     *
     * Adapted from §4.1.2 for minute-level HR data:
     *   1. Convert BPM → RR intervals (RR = 60/BPM seconds)
     *   2. Build histogram with [binWidth] bins
     *   3. Mo  = Mode (most frequent RR bin center)
     *   4. AMo = Amplitude of Mode (% of samples in Mode bin)
     *   5. MxDMn = Variation Range (max RR - min RR)
     *   6. SI = AMo / (2 * Mo * MxDMn)
     *
     * @param hrSamples HR samples from a rest period (e.g., deep sleep)
     * @param binWidth Histogram bin width in seconds (default 0.04s ≈ 2-3 bpm)
     */
    fun calculateBaevskyStressIndex(
        hrSamples: List<HrSample>,
        binWidth: Double = 0.04
    ): BaevskyStressResult {
        if (hrSamples.size < 5) {
            return BaevskyStressResult(0.0, 0.0, 0.0, 0.0)
        }

        // Convert BPM to RR intervals in seconds
        val rrIntervals = hrSamples
            .map { 60.0 / it.bpm }
            .filter { it.isFinite() && it > 0 }

        if (rrIntervals.size < 5) {
            return BaevskyStressResult(0.0, 0.0, 0.0, 0.0)
        }

        val minRR = rrIntervals.min()
        val maxRR = rrIntervals.max()
        val variationRange = maxRR - minRR

        if (variationRange <= 0.001) {
            // Perfectly uniform — extremely high stress (rigid rhythm)
            return BaevskyStressResult(
                stressIndex = 999.0,
                modeRR = minRR,
                modeAmplitude = 100.0,
                variationRange = variationRange
            )
        }

        // Build histogram
        val binStart = (minRR / binWidth).toInt() * binWidth
        val histogram = mutableMapOf<Int, Int>()
        rrIntervals.forEach { rr ->
            val binIdx = ((rr - binStart) / binWidth).toInt()
            histogram[binIdx] = (histogram[binIdx] ?: 0) + 1
        }

        // Find mode bin
        val modeBin = histogram.maxByOrNull { it.value }!!
        val modeRR = binStart + (modeBin.key + 0.5) * binWidth
        val modeAmplitude = (modeBin.value.toDouble() / rrIntervals.size) * 100.0

        // Baevsky SI formula
        val stressIndex = modeAmplitude / (2.0 * modeRR * variationRange)

        return BaevskyStressResult(
            stressIndex = stressIndex,
            modeRR = modeRR,
            modeAmplitude = modeAmplitude,
            variationRange = variationRange
        )
    }

    // =========================================================================
    // Phase 4: Readiness Score Synthesis
    // =========================================================================

    /**
     * Computes an overall Readiness Score (0-100) by combining multiple metrics.
     *
     * Weighted formula (§6):
     *   Score = sleep*0.4 + rhr*0.3 + training*0.2 + volatility*0.1
     *
     * Override rule: if Z-Score ≥ 2.5, force score ≤ 30 ("Rest Day")
     *
     * Semaphoric levels:
     *   85-100 → GREEN  (High intensity allowed)
     *   60-84  → YELLOW (Maintenance / active recovery)
     *   0-59   → RED    (Rest necessary)
     *
     * @param sleepScore Sleep quality score (0-100) from AnalyzeSleepQualityUseCase
     * @param rhrAnomaly RHR anomaly result (null = assume normal)
     * @param tsb Current Training Stress Balance
     * @param volatilityScore Nocturnal HR volatility score (0-100, null = assume 70)
     */
    fun calculateReadinessScore(
        sleepScore: Int,
        rhrAnomaly: RhrAnomalyResult?,
        tsb: Double,
        volatilityScore: Double?
    ): ReadinessResult {
        // --- Sleep Component (40%) ---
        val sleepComponent = (sleepScore / 100.0) * 40.0

        // --- RHR Component (30%) ---
        // Inverse Z-Score: low Z = high score, high Z = low score
        val rhrNormalized = if (rhrAnomaly != null) {
            when {
                rhrAnomaly.zScore <= 0 -> 100.0    // Below baseline is great
                rhrAnomaly.zScore < 1.5 -> 100.0 - (rhrAnomaly.zScore / 1.5) * 20.0 // 100→80
                rhrAnomaly.zScore < 2.5 -> 80.0 - ((rhrAnomaly.zScore - 1.5) / 1.0) * 40.0 // 80→40
                else -> max(0.0, 40.0 - (rhrAnomaly.zScore - 2.5) * 20.0) // 40→0
            }
        } else {
            70.0 // Default if no data
        }
        val rhrComponent = (rhrNormalized / 100.0) * 30.0

        // --- Training Component (20%) ---
        // TSB mapping: TSB > +5 = fresh (100), TSB -10..-30 = loaded (40-60), TSB < -30 = danger (0-20)
        val trainingNormalized = when {
            tsb >= 10.0 -> 100.0
            tsb >= 5.0 -> 90.0
            tsb >= 0.0 -> 75.0
            tsb >= -10.0 -> 60.0
            tsb >= -20.0 -> 45.0
            tsb >= -30.0 -> 25.0
            else -> 10.0  // Deep negative — overtraining risk
        }
        val trainingComponent = (trainingNormalized / 100.0) * 20.0

        // --- Volatility Component (10%) ---
        val volScore = volatilityScore ?: 70.0
        val volatilityComponent = (volScore / 100.0) * 10.0

        // --- Total ---
        var totalScore = (sleepComponent + rhrComponent + trainingComponent + volatilityComponent)
            .roundToInt()
            .coerceIn(0, 100)

        // --- Override: illness detection ---
        val isOverridden = rhrAnomaly != null && rhrAnomaly.zScore >= 2.5
        if (isOverridden) {
            totalScore = totalScore.coerceAtMost(30)
        }

        val level = when {
            totalScore >= 85 -> ReadinessLevel.GREEN
            totalScore >= 60 -> ReadinessLevel.YELLOW
            else -> ReadinessLevel.RED
        }

        return ReadinessResult(
            score = totalScore,
            level = level,
            breakdown = ReadinessBreakdown(
                sleepComponent = sleepComponent,
                rhrComponent = rhrComponent,
                trainingComponent = trainingComponent,
                volatilityComponent = volatilityComponent
            ),
            isOverridden = isOverridden
        )
    }
}
