package com.ai_health.core.domain.usecase.sleep

import com.ai_health.core.domain.model.SleepQualityResult
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.SleepStageRec
import com.ai_health.core.domain.model.SleepStageType
import java.time.Duration
import javax.inject.Inject


class AnalyzeSleepQualityUseCase @Inject constructor() {

    // Costanti per gli obiettivi (configurabili in futuro)
    private val TARGET_SLEEP_MINUTES = 480 // 8 ore
    private val TARGET_DEEP_PERCENT = 20
    private val TARGET_REM_PERCENT = 25

    operator fun invoke(session: SleepSessionRec): SleepQualityResult {
        val stages = session.stages

        // 1. Aggregazione dei dati
        var deepMillis = 0L
        var remMillis = 0L
        var lightMillis = 0L
        var awakeMillis = 0L

        stages.forEach { stage ->
            val duration = stage.endTime.toEpochMilli() - stage.startTime.toEpochMilli()
            // Mappa l'intero raw (Health Connect standard) al nostro Enum
            val type = SleepStageType.fromInt(stage.stage)
            
            when (type) {
                SleepStageType.DEEP -> deepMillis += duration
                SleepStageType.REM -> remMillis += duration
                SleepStageType.LIGHT -> lightMillis += duration
                SleepStageType.AWAKE, SleepStageType.OUT_OF_BED -> awakeMillis += duration
                else -> lightMillis += duration // Fallback per UNKNOWN o SLEEPING generico
            }
        }

        val totalSleepMillis = deepMillis + remMillis + lightMillis // Escludiamo awake dal "total sleep", ma lo teniamo per l'efficienza
        val totalSessionMillis = totalSleepMillis + awakeMillis // Tempo totale a letto

        if (totalSessionMillis == 0L) return emptyResult() // Gestione edge case

        // 2. Calcolo Percentuali
        val deepPercent = ((deepMillis.toDouble() / totalSessionMillis) * 100).toInt()
        val remPercent = ((remMillis.toDouble() / totalSessionMillis) * 100).toInt()

        // 3. Calcolo Punteggio (Logica Semplificata)
        // Punteggio Durata (Max 40 punti)
        val totalMinutes = totalSleepMillis / 1000 / 60
        val durationScore = (totalMinutes.toFloat() / TARGET_SLEEP_MINUTES * 40).coerceIn(0f, 40f)

        // Punteggio Profondo (Max 30 punti)
        val deepScore = (deepPercent.toFloat() / TARGET_DEEP_PERCENT * 30).coerceIn(0f, 30f)

        // Punteggio REM (Max 20 punti)
        val remScore = (remPercent.toFloat() / TARGET_REM_PERCENT * 20).coerceIn(0f, 20f)

        // Punteggio Efficienza (Max 10 punti) - Penalità per tempo sveglio
        val awakePercent = (awakeMillis.toDouble() / totalSessionMillis) * 100
        val efficiencyScore = (10 - (awakePercent / 2)).coerceIn(0.0, 10.0)

        val finalScore = (durationScore + deepScore + remScore + efficiencyScore).toInt()

        // 4. Generazione Feedback
        val feedback = when {
            finalScore >= 85 -> "Sonno eccellente! Sei pronto per la giornata."
            finalScore >= 70 -> "Buon riposo, ma potresti migliorare la continuità."
            deepPercent < 10 -> "Sonno profondo basso. Evita caffeina la sera."
            else -> "Qualità del sonno bassa. Cerca di mantenere orari regolari."
        }

        return SleepQualityResult(
            totalScore = finalScore,
            deepSleepDuration = Duration.ofMillis(deepMillis),
            remSleepDuration = Duration.ofMillis(remMillis),
            lightSleepDuration = Duration.ofMillis(lightMillis),
            awakeDuration = Duration.ofMillis(awakeMillis),
            deepSleepPercentage = deepPercent,
            remSleepPercentage = remPercent,
            feedback = feedback
        )
    }

    private fun emptyResult(): SleepQualityResult {
        return SleepQualityResult(
            totalScore = 0,
            deepSleepDuration = Duration.ZERO,
            remSleepDuration = Duration.ZERO,
            lightSleepDuration = Duration.ZERO,
            awakeDuration = Duration.ZERO,
            deepSleepPercentage = 0,
            remSleepPercentage = 0,
            feedback = "Dati insufficienti per analizzare il sonno."
        )
    }
}