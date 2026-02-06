package com.ai_health.core.domain.usecase.sleep

import com.ai_health.core.domain.model.HeartRateRec
import com.ai_health.core.domain.model.ScoreBreakdown
import com.ai_health.core.domain.model.SleepMetrics
import com.ai_health.core.domain.model.SleepQualityResult
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.SleepStageRec
import com.ai_health.core.domain.model.SleepStageType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * UseCase per l'analisi avanzata della qualità del sonno basata su HR e Architettura.
 * 
 * Implementa l'algoritmo di scoring multi-parametrico:
 * 1. Sleep Architecture & Continuity (40%)
 * 2. Nocturnal HR Dipping (30%)
 * 3. RHR Analysis (20%)
 * 4. Sleep Timing/Nadir (10%)
 * 
 * Basato sul documento: algoritmo_analisi_sonno.md
 */
class AnalyzeSleepQualityUseCase @Inject constructor() {

    companion object {
        
        // Pesi massimi per componente
        private const val W_ARCHITECTURE = 40.0
        private const val W_DIPPING = 30.0
        private const val W_RHR = 20.0
        private const val W_TIMING = 10.0

        // Target Fisiologici
        private const val TARGET_DEEP_PERCENT = 20.0
        private const val TARGET_REM_PERCENT = 25.0
        private const val THRESHOLD_WASO_PENALTY = 20.0 // Minuti prima di penalizzare
        private const val THRESHOLD_FRAG_PENALTY = 15.0 // Transizioni/ora prima di penalizzare

        // Costanti per Interpolazione
        private const val MAX_INTERPOLATION_GAP_MS = 60 * 60 * 1000L // 60 min
        private const val MIN_INTERPOLATION_GAP_MS = 60 * 1000L // 1 min
        private const val DAYTIME_WINDOW_HOURS = 16L // Finestra diurna precedente al sonno

        // Soglie Qualità Dati
        private const val MIN_DATA_QUALITY = 0.5       // Alta risoluzione (>= 1 sample/10 min)
        private const val LOW_RES_MAX_GAP_MIN = 31L    // Gap max per low-res (31 min = Xiaomi tipico)
        private const val LOW_RES_MIN_GAP_MIN = 15L    // Gap min per considerare low-res (sopra = low-res mode)
        
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    }
    
    /**
     * Risultato dell'analisi qualità dati.
     */
    private data class DataQualityResult(
        val score: Double,
        val isLowResolution: Boolean,
        val avgGapMinutes: Long
    )

    /**
     * Esegue l'analisi completa della qualità del sonno.
     * 
     * @param session Sessione di sonno con le fasi
     * @param heartRates Stream HR (idealmente da sleepStart - 16h a sleepEnd)
     * @param userBaselineRhr Media RHR degli ultimi 7 giorni dell'utente (default 60)
     */
    operator fun invoke(
        session: SleepSessionRec,
        heartRates: List<HeartRateRec> = emptyList(),
        userBaselineRhr: Int = 60
    ): SleepQualityResult {
        val stages = session.stages
        
        println("[SleepScoring] " + "========================================")
        println("[SleepScoring] " + "🌙 SLEEP SCORING ANALYSIS START")
        println("[SleepScoring] " + "Session ID: ${session.id}")
        println("[SleepScoring] " + "Stages count: ${stages.size}")
        println("[SleepScoring] " + "HR samples received: ${heartRates.size}")
        println("[SleepScoring] " + "User baseline RHR: $userBaselineRhr bpm")

        if (stages.isEmpty()) {
            println("[SleepScoring] WARN: " + "❌ No sleep stages found - returning empty result")
            return emptyResult("Dati del sonno mancanti.")
        }

        // --- FASE 1: Analisi Temporale e Architetturale ---
        val sortedStages = stages.sortedBy { it.startTime }
        val sleepOnset = sortedStages.first().startTime
        val sleepOffset = sortedStages.last().endTime
        
        println("[SleepScoring] " + "📅 Sleep window: ${timeFormatter.format(sleepOnset)} → ${timeFormatter.format(sleepOffset)}")
        
        val archMetrics = calculateArchitectureMetrics(sortedStages)
        println("[SleepScoring] " + "📊 Architecture Metrics:")
        println("[SleepScoring] " + "   Total duration: ${archMetrics.totalMinutes} min")
        println("[SleepScoring] " + "   Deep: ${archMetrics.deepPercent}% (target: ${TARGET_DEEP_PERCENT.toInt()}%)")
        println("[SleepScoring] " + "   REM: ${archMetrics.remPercent}% (target: ${TARGET_REM_PERCENT.toInt()}%)")
        println("[SleepScoring] " + "   Light: ${archMetrics.lightPercent}%")
        println("[SleepScoring] " + "   WASO: ${archMetrics.waso} min (threshold: ${THRESHOLD_WASO_PENALTY.toInt()} min)")
        println("[SleepScoring] " + "   Fragmentation: %.2f trans/h (threshold: ${THRESHOLD_FRAG_PENALTY.toInt()})".format(archMetrics.fragmentationIndex))

        // --- FASE 2: Pre-elaborazione Dati Cardiaci ---
        if (heartRates.isEmpty()) {
            println("[SleepScoring] WARN: " + "⚠️ No HR data - using architecture-only fallback")
            return resultWithArchitectureOnly(archMetrics, "Dati cardiaci non disponibili.")
        }

        // Filtra e separa i dati HR in Diurni e Notturni
        val (daytimeBpmRaw, nocturnalBpmRaw) = splitDayNightHr(heartRates, sleepOnset, sleepOffset)
        println("[SleepScoring] " + "💓 HR Data Split:")
        println("[SleepScoring] " + "   Daytime samples (16h window): ${daytimeBpmRaw.size}")
        println("[SleepScoring] " + "   Nocturnal samples (raw): ${nocturnalBpmRaw.size}")
        
        // Filtra HR durante periodi AWAKE dalla media notturna
        val nocturnalBpmFiltered = filterAwakePeriodsFromHr(nocturnalBpmRaw, sortedStages)
        println("[SleepScoring] " + "   Nocturnal after AWAKE filter: ${nocturnalBpmFiltered.size}")
        
        // Imputazione dei dati mancanti (Interpolazione Lineare)
        val nocturnalBpm = interpolateHeartRate(nocturnalBpmFiltered)
        println("[SleepScoring] " + "   Nocturnal after interpolation: ${nocturnalBpm.size}")
        
        // Calcolo affidabilità dati
        val sleepDurationMin = Duration.between(sleepOnset, sleepOffset).toMinutes()
        val dataQualityResult = calculateDataQuality(nocturnalBpmRaw, sleepOnset, sleepOffset)
        val dataQuality = dataQualityResult.score
        val avgGapMin = dataQualityResult.avgGapMinutes
        val isLowResolution = dataQualityResult.isLowResolution
        
        println("[SleepScoring] " + "   Data quality score: %.2f".format(dataQuality))
        println("[SleepScoring] " + "   Avg gap: $avgGapMin min")
        println("[SleepScoring] " + "   Low-resolution mode: $isLowResolution")

        // Verifica se abbiamo abbastanza dati per l'analisi
        // Calcola campioni attesi in base alla durata del sonno
        val hasEnoughData = when {
            // Gap < 15 min -> Alta risoluzione: usa quality score
            avgGapMin < LOW_RES_MIN_GAP_MIN -> {
                val enough = dataQuality >= MIN_DATA_QUALITY && nocturnalBpm.isNotEmpty()
                println("[SleepScoring] " + "   HIGH-RES check: quality >= 0.5? $enough")
                enough
            }
            // Gap 15-31 min -> Low-res: verifica campioni attesi
            avgGapMin <= LOW_RES_MAX_GAP_MIN -> {
                // Campioni attesi = durata sonno / gap medio (con margine 70%)
                val expectedSamples = (sleepDurationMin / avgGapMin).toInt()
                val minRequiredSamples = (expectedSamples * 0.7).toInt().coerceAtLeast(2)
                val enough = nocturnalBpmRaw.size >= minRequiredSamples
                println("[SleepScoring] " + "   LOW-RES check: samples ${nocturnalBpmRaw.size} >= $minRequiredSamples (70% of $expectedSamples expected)? $enough")
                enough
            }
            // Gap > 31 min -> Troppo sparso, fallback
            else -> {
                println("[SleepScoring] " + "   Gap troppo grande ($avgGapMin min > $LOW_RES_MAX_GAP_MIN min) -> FALLBACK")
                false
            }
        }
        
        if (!hasEnoughData) {
            println("[SleepScoring] WARN: " + "⚠️ Insufficient HR data - using architecture-only fallback")
            return resultWithArchitectureOnly(archMetrics, "Dati cardiaci insufficienti.")
        }
        
        println("[SleepScoring] " + "✅ Proceeding with " + if (isLowResolution) "LOW-RES" else "HIGH-RES" + " strategy")

        // --- FASE 3: Calcolo Metriche Cardiache Avanzate ---
        val nocturnalAvg = nocturnalBpm.map { it.beatsPerMinute }.average()
        
        // Se mancano dati diurni, usiamo una stima conservativa
        val daytimeAvg = if (daytimeBpmRaw.isNotEmpty()) {
            daytimeBpmRaw.map { it.beatsPerMinute }.average()
        } else {
            println("[SleepScoring] " + "   ⚠️ No daytime HR data - using estimate (nocturnal * 1.2)")
            nocturnalAvg * 1.2
        }
        
        println("[SleepScoring] " + "❤️ HR Averages:")
        println("[SleepScoring] " + "   Daytime avg: %.1f bpm".format(daytimeAvg))
        println("[SleepScoring] " + "   Nocturnal avg: %.1f bpm".format(nocturnalAvg))

        // Dipping Factor
        val dippingPercent = if (daytimeAvg > 0) {
            ((daytimeAvg - nocturnalAvg) / daytimeAvg) * 100.0
        } else 0.0
        
        println("[SleepScoring] " + "📉 Dipping Analysis:")
        println("[SleepScoring] " + "   Dipping: %.1f%% (optimal: 10-20%%)".format(dippingPercent))
        val dippingCategory = when {
            dippingPercent < 0 -> "❌ REVERSE DIPPER (pathological)"
            dippingPercent < 10 -> "⚠️ NON-DIPPER (suboptimal)"
            dippingPercent <= 20 -> "✅ NORMAL DIPPER (healthy)"
            else -> "🔵 EXTREME DIPPER (>20%)"
        }
        println("[SleepScoring] " + "   Category: $dippingCategory")

        // RHR e Nadir
        val minHrRec = nocturnalBpm.minByOrNull { it.beatsPerMinute }!!
        val lowestNocturnalHr = minHrRec.beatsPerMinute.toInt()

        // Calcolo posizione Nadir (offset % rispetto alla durata del sonno)
        val sleepDurationMs = Duration.between(sleepOnset, sleepOffset).toMillis()
        val nadirOffsetMs = Duration.between(sleepOnset, minHrRec.time).toMillis()
        val nadirOffsetPercent = if (sleepDurationMs > 0) {
            ((nadirOffsetMs.toDouble() / sleepDurationMs) * 100).toInt()
        } else 0
        
        println("[SleepScoring] " + "🎯 Nadir Analysis:")
        println("[SleepScoring] " + "   Lowest HR: $lowestNocturnalHr bpm at ${timeFormatter.format(minHrRec.time)}")
        println("[SleepScoring] " + "   Nadir position: $nadirOffsetPercent%% of night (optimal: 40-65%%)")
        println("[SleepScoring] " + "   Baseline RHR: $userBaselineRhr bpm, Delta: ${lowestNocturnalHr - userBaselineRhr} bpm")

        // --- FASE 4: Calcolo Punteggi (Scoring) ---
        val scoreArch = scoreArchitecture(archMetrics)
        val scoreDipping = scoreDipping(dippingPercent)
        val scoreRhr = scoreRhr(lowestNocturnalHr, userBaselineRhr)
        val scoreTiming = scoreTiming(nadirOffsetPercent)

        println("[SleepScoring] " + "🏆 SCORE BREAKDOWN:")
        println("[SleepScoring] " + "   Architecture: %.1f / ${W_ARCHITECTURE.toInt()} pts".format(scoreArch))
        println("[SleepScoring] " + "   Dipping:      %.1f / ${W_DIPPING.toInt()} pts".format(scoreDipping))
        println("[SleepScoring] " + "   RHR:          %.1f / ${W_RHR.toInt()} pts".format(scoreRhr))
        println("[SleepScoring] " + "   Timing:       %.1f / ${W_TIMING.toInt()} pts".format(scoreTiming))

        // Somma ponderata
        val totalScore = (scoreArch + scoreDipping + scoreRhr + scoreTiming)
            .roundToInt()
            .coerceIn(0, 100)
        
        println("[SleepScoring] " + "========================================")
        println("[SleepScoring] " + "⭐ FINAL SCORE: $totalScore / 100")
        println("[SleepScoring] " + "========================================")

        // --- FASE 5: Generazione Feedback ---
        val baseFeedback = generateFeedback(
            archMetrics,
            dippingPercent,
            lowestNocturnalHr,
            userBaselineRhr,
            nadirOffsetPercent
        )
        
        // Aggiungi warning low-res al feedback se applicabile
        val feedback = if (isLowResolution) {
            listOf("⚠️ Analisi basata su dati a bassa risoluzione.") + baseFeedback
        } else {
            baseFeedback
        }

        val breakdown = ScoreBreakdown(
            architectureScore = scoreArch,
            dippingScore = scoreDipping,
            rhrScore = scoreRhr,
            timingScore = scoreTiming
        )

        val metrics = SleepMetrics(
            totalSleepDurationMin = archMetrics.totalMinutes,
            wasoMinutes = archMetrics.waso,
            fragmentationIndex = archMetrics.fragmentationIndex,
            dippingPercent = dippingPercent,
            nocturnalHrAvg = nocturnalAvg.roundToInt(),
            daytimeHrAvg = daytimeAvg.roundToInt(),
            lowestNocturnalHr = lowestNocturnalHr,
            hrNadirOffsetPercent = nadirOffsetPercent,
            dataQualityScore = dataQuality,
            isLowResolution = isLowResolution
        )

        return SleepQualityResult(
            totalScore = totalScore,
            breakdown = breakdown,
            metrics = metrics,
            deepSleepDuration = Duration.ofMillis(archMetrics.deepMs),
            remSleepDuration = Duration.ofMillis(archMetrics.remMs),
            lightSleepDuration = Duration.ofMillis(archMetrics.lightMs),
            awakeDuration = Duration.ofMillis(archMetrics.awakeMs),
            deepSleepPercentage = archMetrics.deepPercent,
            remSleepPercentage = archMetrics.remPercent,
            feedback = feedback.firstOrNull() ?: "Analisi completata.",
            feedbackList = feedback,
            dataQualityWarning = isLowResolution
        )
    }

    // ==============================================================================
    // LOGICA DI CALCOLO METRICHE (Internal Methods)
    // ==============================================================================

    private data class ArchMetrics(
        val totalMinutes: Long,
        val deepMs: Long,
        val remMs: Long,
        val lightMs: Long,
        val awakeMs: Long,
        val deepPercent: Int,
        val remPercent: Int,
        val lightPercent: Int,
        val waso: Long,
        val fragmentationIndex: Double
    )

    private fun calculateArchitectureMetrics(stages: List<SleepStageRec>): ArchMetrics {
        var deepMs = 0L
        var remMs = 0L
        var lightMs = 0L
        var awakeMs = 0L
        var wasoMs = 0L
        var transitions = 0

        val totalDurationMs = Duration.between(stages.first().startTime, stages.last().endTime).toMillis()

        var previousStageType = SleepStageType.UNKNOWN
        stages.forEachIndexed { index, rec ->
            val duration = Duration.between(rec.startTime, rec.endTime).toMillis()
            val type = SleepStageType.fromInt(rec.stage)

            // Rilevamento Transizioni (Frammentazione)
            if (index > 0 && type != previousStageType) {
                transitions++
            }
            previousStageType = type

            when (type) {
                SleepStageType.DEEP -> deepMs += duration
                SleepStageType.REM -> remMs += duration
                SleepStageType.LIGHT, SleepStageType.SLEEPING -> lightMs += duration
                SleepStageType.AWAKE, SleepStageType.OUT_OF_BED -> {
                    awakeMs += duration
                    // WASO: Escludiamo il primo e l'ultimo blocco
                    val isFirstBlock = index == 0
                    val isLastBlock = index == stages.size - 1
                    if (!isFirstBlock && !isLastBlock) {
                        wasoMs += duration
                    }
                }
                else -> lightMs += duration
            }
        }

        val totalMinutes = max(1L, totalDurationMs / 1000 / 60)
        val validTotalMs = max(1L, deepMs + remMs + lightMs + awakeMs)

        val deepPct = ((deepMs.toDouble() / validTotalMs) * 100).toInt()
        val remPct = ((remMs.toDouble() / validTotalMs) * 100).toInt()
        val lightPct = ((lightMs.toDouble() / validTotalMs) * 100).toInt()

        val sleepHours = totalMinutes / 60.0
        val fragIndex = if (sleepHours > 0) transitions / sleepHours else 0.0

        return ArchMetrics(
            totalMinutes = totalMinutes,
            deepMs = deepMs,
            remMs = remMs,
            lightMs = lightMs,
            awakeMs = awakeMs,
            deepPercent = deepPct,
            remPercent = remPct,
            lightPercent = lightPct,
            waso = wasoMs / 1000 / 60,
            fragmentationIndex = fragIndex
        )
    }

    // ==============================================================================
    // LOGICA DI SCORING (Funzioni di Normalizzazione)
    // ==============================================================================

    private fun scoreArchitecture(m: ArchMetrics): Double {
        // Deep Sleep Score (Max 15 pt): Saturazione lineare fino al target
        val deepScore = 15.0 * (m.deepPercent.toDouble() / TARGET_DEEP_PERCENT).coerceAtMost(1.0)

        // REM Sleep Score (Max 15 pt)
        val remScore = 15.0 * (m.remPercent.toDouble() / TARGET_REM_PERCENT).coerceAtMost(1.0)

        // Base Continuity (10 pt) con penalità
        var continuityScore = 10.0

        // Penalità WASO: -0.5 punti per ogni minuto sopra soglia
        val wasoExcess = max(0L, m.waso - THRESHOLD_WASO_PENALTY.toLong())
        continuityScore -= (wasoExcess * 0.5)

        // Penalità Frammentazione: -1 punto per ogni transizione sopra soglia
        val fragExcess = max(0.0, m.fragmentationIndex - THRESHOLD_FRAG_PENALTY)
        continuityScore -= (fragExcess * 1.0)

        return (deepScore + remScore + continuityScore).coerceIn(0.0, W_ARCHITECTURE)
    }

    private fun scoreDipping(dipPercent: Double): Double {
        // Piecewise Linear Function (0-30 punti)
        return when {
            dipPercent < 0 -> 0.0 // Reverse Dipping (Grave)
            dipPercent < 10 -> {
                // Non-Dipper: Crescita lineare da 0 a 20 punti
                20.0 * (dipPercent / 10.0)
            }
            dipPercent <= 20 -> {
                // Normal Dipper: Crescita da 20 a 30 punti
                20.0 + 10.0 * ((dipPercent - 10.0) / 10.0)
            }
            else -> 30.0 // Extreme Dipper (>20%): Saturazione
        }
    }

    private fun scoreRhr(nightMin: Int, baseline: Int): Double {
        // Se minHR <= baseline -> 20 punti
        val delta = nightMin - baseline
        if (delta <= 0) return W_RHR

        // Penalità: -2 punti per ogni bpm sopra media
        val penalty = delta * 2.0
        return (W_RHR - penalty).coerceAtLeast(0.0)
    }

    private fun scoreTiming(nadirPercent: Int): Double {
        // Curva per posizione del nadir (0-10 punti)
        return when (nadirPercent) {
            in 40..65 -> 10.0 // Ottimo: Metà notte (Hammock ideale)
            in 30..39, in 66..75 -> 7.0 // Buono
            in 20..29, in 76..85 -> 4.0 // Sufficiente
            else -> 2.0 // Scarso: Crash iniziale o slope discendente
        }
    }

    // ==============================================================================
    // HELPER & DATA PROCESSING
    // ==============================================================================

    private fun splitDayNightHr(
        fullList: List<HeartRateRec>,
        sleepStart: Instant,
        sleepEnd: Instant
    ): Pair<List<HeartRateRec>, List<HeartRateRec>> {
        val nocturnal = ArrayList<HeartRateRec>()
        val daytime = ArrayList<HeartRateRec>()

        val dayStartWindow = sleepStart.minusSeconds(DAYTIME_WINDOW_HOURS * 60 * 60)

        fullList.forEach { rec ->
            when {
                rec.time >= sleepStart && rec.time <= sleepEnd -> nocturnal.add(rec)
                rec.time >= dayStartWindow && rec.time < sleepStart -> daytime.add(rec)
            }
        }
        return Pair(daytime, nocturnal)
    }

    /**
     * Filtra i sample HR che cadono durante periodi AWAKE.
     * L'obiettivo è ottenere il "vero" carico cardiaco del sonno.
     */
    private fun filterAwakePeriodsFromHr(
        hrList: List<HeartRateRec>,
        stages: List<SleepStageRec>
    ): List<HeartRateRec> {
        return hrList.filter { hr ->
            val correspondingStage = stages.find { stage ->
                hr.time >= stage.startTime && hr.time <= stage.endTime
            }
            // Mantieni solo se non è AWAKE o OUT_OF_BED
            val stageType = correspondingStage?.let { SleepStageType.fromInt(it.stage) }
            stageType != SleepStageType.AWAKE && stageType != SleepStageType.OUT_OF_BED
        }
    }

    private fun interpolateHeartRate(raw: List<HeartRateRec>): List<HeartRateRec> {
        if (raw.isEmpty()) return emptyList()

        val sorted = raw.sortedBy { it.time }
        val result = ArrayList<HeartRateRec>()

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            result.add(current)

            val timeDiff = Duration.between(current.time, next.time).toMillis()

            // Solo interpolazione per gap > 1 min E gap < 60 min
            if (timeDiff > MIN_INTERPOLATION_GAP_MS && timeDiff < MAX_INTERPOLATION_GAP_MS) {
                val gapMinutes = (timeDiff / 60_000L).toInt()
                val bpmDiff = next.beatsPerMinute - current.beatsPerMinute
                val bpmStep = bpmDiff.toDouble() / gapMinutes
                val timeStep = timeDiff / gapMinutes

                for (step in 1 until gapMinutes) {
                    val newTime = current.time.plusMillis(timeStep * step)
                    val newBpm = (current.beatsPerMinute + (bpmStep * step)).toLong()
                    result.add(HeartRateRec(
                        id = "interpolated_${current.id}_$step",
                        beatsPerMinute = newBpm,
                        time = newTime,
                        source = "interpolated"
                    ))
                }
            }
            // Gap >= 60 min: non interpolare (lasciamo il buco)
        }
        result.add(sorted.last())
        return result
    }

    private fun calculateDataQuality(raw: List<HeartRateRec>, start: Instant, end: Instant): DataQualityResult {
        if (raw.isEmpty()) return DataQualityResult(0.0, false, 0)

        val durationMinutes = Duration.between(start, end).toMinutes()
        if (durationMinutes == 0L) return DataQualityResult(0.0, false, 0)

        // Calcola gap medio tra campioni
        val sorted = raw.sortedBy { it.time }
        val gaps = mutableListOf<Long>()
        for (i in 0 until sorted.size - 1) {
            val gapMs = Duration.between(sorted[i].time, sorted[i + 1].time).toMillis()
            gaps.add(gapMs / 60_000L) // convert to minutes
        }
        val avgGapMinutes = if (gaps.isNotEmpty()) gaps.average().toLong() else durationMinutes
        
        // Determina se siamo in modalità low-resolution
        // Low-res: gap medio tra 15 e 45 minuti (es. Xiaomi ogni 30 min)
        val isLowResolution = avgGapMinutes in LOW_RES_MIN_GAP_MIN..LOW_RES_MAX_GAP_MIN
        
        // Score basato su densità (1 sample/10 min = score 1.0)
        val density = raw.size.toDouble() / durationMinutes
        val score = (density / 0.1).coerceAtMost(1.0)
        
        return DataQualityResult(score, isLowResolution, avgGapMinutes)
    }

    private fun generateFeedback(
        arch: ArchMetrics,
        dip: Double,
        minHr: Int,
        baseline: Int,
        nadir: Int
    ): List<String> {
        val msgs = ArrayList<String>()

        // Feedback Cardiaco (Priorità alta)
        when {
            dip < 5.0 -> msgs.add("Cuore sotto stress: Il tuo battito non è rallentato stanotte (Dipping < 5%). Hai consumato alcol o pasti pesanti?")
            minHr > baseline + 5 -> msgs.add("Recupero incompleto: La frequenza a riposo è alta (+${minHr - baseline} bpm). Potresti essere in fase iniziale di malattia o sovrallenamento.")
            nadir > 75 -> msgs.add("Metabolismo notturno: Il cuore ha raggiunto il minimo solo al risveglio. Evita di mangiare tardi la sera.")
            else -> msgs.add("Ottimo recupero cardiovascolare: Il tuo cuore ha riposato efficacemente.")
        }

        // Feedback Architetturale
        if (arch.waso > 45) {
            msgs.add("Sonno frammentato: Sei rimasto sveglio per ${arch.waso} minuti. Cerca di mantenere la stanza buia e silenziosa.")
        }
        if (arch.deepPercent < 10) {
            msgs.add("Poco sonno profondo: Prova a regolarizzare gli orari o abbassare la temperatura della stanza.")
        }

        return msgs
    }

    private fun emptyResult(msg: String): SleepQualityResult {
        return SleepQualityResult(
            totalScore = 0,
            breakdown = null,
            metrics = null,
            deepSleepDuration = Duration.ZERO,
            remSleepDuration = Duration.ZERO,
            lightSleepDuration = Duration.ZERO,
            awakeDuration = Duration.ZERO,
            deepSleepPercentage = 0,
            remSleepPercentage = 0,
            feedback = msg,
            feedbackList = listOf(msg)
        )
    }

    private fun resultWithArchitectureOnly(arch: ArchMetrics, warning: String): SleepQualityResult {
        val archScore = scoreArchitecture(arch)
        // Proiettiamo 40 punti su 100
        val projectedScore = (archScore * 2.5).roundToInt().coerceIn(0, 100)
        
        println("[SleepScoring] " + "📊 ARCHITECTURE-ONLY FALLBACK:")
        println("[SleepScoring] " + "   Arch score: %.1f / ${W_ARCHITECTURE.toInt()} → Projected: $projectedScore / 100".format(archScore))
        println("[SleepScoring] " + "   Reason: $warning")

        val metrics = SleepMetrics(
            totalSleepDurationMin = arch.totalMinutes,
            wasoMinutes = arch.waso,
            fragmentationIndex = arch.fragmentationIndex,
            dippingPercent = null,
            nocturnalHrAvg = null,
            daytimeHrAvg = null,
            lowestNocturnalHr = null,
            hrNadirOffsetPercent = null,
            dataQualityScore = 0.0
        )

        val breakdown = ScoreBreakdown(
            architectureScore = archScore,
            dippingScore = 0.0,
            rhrScore = 0.0,
            timingScore = 0.0
        )

        return SleepQualityResult(
            totalScore = projectedScore,
            breakdown = breakdown,
            metrics = metrics,
            deepSleepDuration = Duration.ofMillis(arch.deepMs),
            remSleepDuration = Duration.ofMillis(arch.remMs),
            lightSleepDuration = Duration.ofMillis(arch.lightMs),
            awakeDuration = Duration.ofMillis(arch.awakeMs),
            deepSleepPercentage = arch.deepPercent,
            remSleepPercentage = arch.remPercent,
            feedback = warning,
            feedbackList = listOf(warning, "Punteggio basato solo sulle fasi del sonno.")
        )
    }
}