package com.ai_health.core.domain.usecase

import com.ai_health.core.domain.model.RawStep
import com.ai_health.core.domain.model.StepSource
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.UserActivityType
import com.ai_health.core.domain.model.ValidatedStep
import com.ai_health.core.domain.repository.ActivityLogRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ValidateStepCountUseCase @Inject constructor(
    private val activityLogRepository: ActivityLogRepository,
    private val healthRepository: com.ai_health.core.domain.repository.HealthRepository
) {

    /**
     * Validates a list of raw steps using strictly defined policies.
     * With GRANULAR [StepValidator] logging for deep debugging.
     */
    suspend operator fun invoke(rawSteps: List<RawStep>): List<ValidatedStep> {
        println("[StepValidator] ==================================================")
        println("[StepValidator] >>> START VALIDATION | Total Raw Steps: ${rawSteps.size}")
        
        if (rawSteps.isEmpty()) {
            println("[StepValidator] Input is empty. Returning empty list.")
            return emptyList()
        }

        // 0. Pre-analysis
        val phoneSteps = rawSteps.filter { it.source == StepSource.PHONE }
        val wearableSteps = rawSteps.filter { it.source == StepSource.WEARABLE }
        println("[StepValidator] Input Breakdown -> Phone: ${phoneSteps.size} | Wearable: ${wearableSteps.size}")

        // Policy 1: Trust Wearable (Lazy Sync Friendly)
        val hasWearableData = wearableSteps.isNotEmpty()

        val preliminarySteps = if (hasWearableData) {
            println("[StepValidator] [STRATEGY: TRUST WEARABLE]")
            println("[StepValidator] Wearable data detected. Discarding ${phoneSteps.size} Phone steps (Silence Assenso).")
             // Optional: Log discarded phone steps summary
            if (phoneSteps.isNotEmpty()) {
                val first = phoneSteps.first()
                val last = phoneSteps.last()
                println("[StepValidator] -> Discarded Phone Range: ${first.startTime} to ${last.endTime}")
            }
            wearableSteps
        } else {
            println("[StepValidator] [STRATEGY: FALLBACK PHONE]")
            println("[StepValidator] No Wearable data found. Using Fallback logic on ${phoneSteps.size} Phone steps.")
            validatePhoneStepsWithActivity(phoneSteps)
        }

        // Policy 3: Global Sleep Filter
        println("[StepValidator] [FILTER: SLEEP CHECK] Inspecting ${preliminarySteps.size} candidates...")
        val finalSteps = filterStepsBySleepData(preliminarySteps)

        val totalValidatedSteps = finalSteps.sumOf { it.rawCount }
        println("[StepValidator] <<< END VALIDATION | Kept: ${finalSteps.size} records | Total Volume: $totalValidatedSteps steps")
        println("[StepValidator] ==================================================")
        
        return finalSteps.map {
            ValidatedStep(
                startTime = it.startTime,
                endTime = it.endTime,
                effectiveCount = it.rawCount
            )
        }.sortedBy { it.startTime }
    }

    private suspend fun filterStepsBySleepData(steps: List<RawStep>): List<RawStep> {
        if (steps.isEmpty()) return emptyList()

        val queryTime = steps.first().startTime.minusSeconds(86400)
        
        println("[StepValidator] Querying Sleep History since $queryTime")
        val sleepSessions: List<SleepSessionRec> = try {
            healthRepository.getSleepHistory(queryTime).firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            println("[StepValidator] [ERROR] Failed to fetch sleep history: ${e.message}")
            emptyList()
        }

        if (sleepSessions.isNotEmpty()) {
            println("[StepValidator] Found ${sleepSessions.size} Sleep Sessions for today:")
            sleepSessions.forEach { println("[StepValidator]    -> Sleep Session: ${it.startTime} to ${it.endTime} (${it.source})") }
        } else {
            println("[StepValidator] No Sleep Sessions found. Skipping filter.")
            return steps
        }

        val filteredSteps = mutableListOf<RawStep>()
        var droppedSleepSteps = 0

        for (step in steps) {
            val overlappingSession: SleepSessionRec? = sleepSessions.find { session ->
                val stepStart = step.startTime.toEpochMilli()
                val stepEnd = step.endTime.toEpochMilli()
                val sleepStart = session.startTime.toEpochMilli()
                val sleepEnd = session.endTime.toEpochMilli()
                (stepStart < sleepEnd) && (sleepStart < stepEnd)
            }

            if (overlappingSession != null) {
                droppedSleepSteps++
                println("[StepValidator] [DROP-SLEEP] Step (${step.startTime} | ${step.rawCount}) overlaps Sleep (${overlappingSession.startTime}-${overlappingSession.endTime})")
            } else {
                filteredSteps.add(step)
            }
        }
        
        println("[StepValidator] Sleep Filter Result -> Kept: ${filteredSteps.size} | Dropped: $droppedSleepSteps")
        return filteredSteps
    }

    private suspend fun validatePhoneStepsWithActivity(phoneSteps: List<RawStep>): List<RawStep> {
        val verifiedSteps = mutableListOf<RawStep>()
        var droppedCount = 0

        println("[StepValidator] Validating ${phoneSteps.size} Phone Steps against Activity Log...")

        for (step in phoneSteps) {
            val midPoint = java.time.Instant.ofEpochMilli((step.startTime.toEpochMilli() + step.endTime.toEpochMilli()) / 2)
            
            // Check Activity Context
            val activity = activityLogRepository.getActivityClosestTo(midPoint, java.time.Duration.ofMinutes(2))
            
            val activityType = activity?.type?.name ?: "NULL"
            val activityConf = activity?.confidence ?: 0
            val activityTime = activity?.timestamp

            val isValid = when (activity?.type) {
                UserActivityType.WALKING,
                UserActivityType.RUNNING -> true
                else -> false
            }

            if (isValid) {
                verifiedSteps.add(step)
                // println("[StepValidator] [KEEP] Step (${step.startTime}) | Activity: $activityType ($activityConf%) @ $activityTime")
            } else {
                droppedCount++
                println("[StepValidator] [DROP-GHOST] Step (${step.startTime} | ${step.rawCount}) | Activity: $activityType ($activityConf%) @ $activityTime | Reason: Not Walking/Running")
            }
        }

        println("[StepValidator] Phone Activity Check Result -> Kept: ${verifiedSteps.size} | Dropped: $droppedCount (Ghost Steps)")
        return verifiedSteps
    }
}
