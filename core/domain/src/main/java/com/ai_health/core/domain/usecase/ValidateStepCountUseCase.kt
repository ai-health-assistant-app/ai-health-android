package com.ai_health.core.domain.usecase

import com.ai_health.core.domain.model.RawStep
import com.ai_health.core.domain.model.StepSource
import com.ai_health.core.domain.model.UserActivityType
import com.ai_health.core.domain.model.ValidatedStep
import com.ai_health.core.domain.repository.ActivityLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

class ValidateStepCountUseCase @Inject constructor(
    private val activityLogRepository: ActivityLogRepository
) {

    /**
     * Validates a list of raw steps by:
     * 1. Deduplicating overlapping steps (Wearable > Phone).
     * 2. filtering out steps that occurred during invalid activities (STILL, TILTING).
     */
    suspend operator fun invoke(rawSteps: List<RawStep>): List<ValidatedStep> {
        if (rawSteps.isEmpty()) return emptyList()

        // Phase 1: Deduplication
        val totalRaw = rawSteps.sumOf { it.rawCount }
        println("StepValidator: [START] Processing ${rawSteps.size} raw records. Total Raw Steps: $totalRaw")

        val deduplicatedSteps = deduplicateSteps(rawSteps)
        val totalDedup = deduplicatedSteps.sumOf { it.rawCount }
        println("StepValidator: [PHASE 1] After Deduplication. Dedup Steps: $totalDedup (Diff: ${totalDedup - totalRaw})")

        // Phase 2: Ghost Step Validation
        val result = validateGhostSteps(deduplicatedSteps)
        val totalFinal = result.sumOf { it.effectiveCount }
        println("StepValidator: [PHASE 2] After Ghost Check. Final Effective Steps: $totalFinal (Diff: ${totalFinal - totalDedup})")
        println("StepValidator: [COMPLETE] Total Removed: ${totalRaw - totalFinal}")
        
        return result
    }

    private fun deduplicateSteps(steps: List<RawStep>): List<RawStep> {
        val sortedSteps = steps.sortedBy { it.startTime }
        val mergedSteps = mutableListOf<RawStep>()
        
        val wearableSteps = sortedSteps.filter { it.source == StepSource.WEARABLE }
        val phoneSteps = sortedSteps.filter { it.source == StepSource.PHONE }

        // Wearable steps are always accepted in deduplication phase
        wearableSteps.forEach { 
             println("StepValidator: [DEDUP] Processing step at ${it.startTime} (Source: ${it.source}, Count: ${it.rawCount}) -> ACCEPTED (Wearable priority)")
        }
        mergedSteps.addAll(wearableSteps)

        // Process Phone steps
        for (phoneStep in phoneSteps) {
            val conflictingWearable = wearableSteps.firstOrNull { wearableStep ->
                 stepsOverlap(phoneStep, wearableStep)
            }
            
            if (conflictingWearable != null) {
                println("StepValidator: [DEDUP] Processing step at ${phoneStep.startTime} (Source: ${phoneStep.source}, Count: ${phoneStep.rawCount}) -> DISCARDED (Overlaps with WEARABLE at ${conflictingWearable.startTime})")
            } else {
                println("StepValidator: [DEDUP] Processing step at ${phoneStep.startTime} (Source: ${phoneStep.source}, Count: ${phoneStep.rawCount}) -> ACCEPTED (No overlap)")
                mergedSteps.add(phoneStep)
            }
        }
        
        return mergedSteps.sortedBy { it.startTime }
    }

    private suspend fun validateGhostSteps(steps: List<RawStep>): List<ValidatedStep> {
        val validatedSteps = mutableListOf<ValidatedStep>()
        
        for (step in steps) {
            val dominantActivity = activityLogRepository.getDominantActivity(step.startTime, step.endTime)
            
            val isValid = when (dominantActivity) {
                UserActivityType.STILL, UserActivityType.TILTING -> false
                else -> true
            }

            if (!isValid) {
                 println("StepValidator: [GHOST] Validating step at ${step.startTime} (Source: ${step.source}, Count: ${step.rawCount}) -> GHOST (Activity: $dominantActivity) -> Counted as 0")
            } else {
                 println("StepValidator: [GHOST] Validating step at ${step.startTime} (Source: ${step.source}, Count: ${step.rawCount}) -> VALID (Activity: $dominantActivity) -> Counted as ${step.rawCount}")
            }

            val effectiveCount = if (isValid) step.rawCount else 0L
            
            validatedSteps.add(
                ValidatedStep(
                    startTime = step.startTime,
                    endTime = step.endTime,
                    effectiveCount = effectiveCount
                )
            )
        }
        
        return validatedSteps
    }

    private fun stepsOverlap(s1: RawStep, s2: RawStep): Boolean {
        // Overlap logic: StartA < EndB && StartB < EndA
        return s1.startTime.isBefore(s2.endTime) && s2.startTime.isBefore(s1.endTime)
    }
}
