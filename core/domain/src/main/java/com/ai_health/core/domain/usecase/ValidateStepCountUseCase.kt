package com.ai_health.core.domain.usecase

import com.ai_health.core.domain.model.RawStep
import com.ai_health.core.domain.model.StepSource
import com.ai_health.core.domain.model.UserActivity
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
     * 1. Separating steps by Source (Phone vs Wearable).
     * 2. Cleaning Phone steps using aggressive Ghost Step detection.
     * 3. Merging: Discarding Phone steps that overlap with ANY Wearable data.
     */
    suspend operator fun invoke(rawSteps: List<RawStep>): List<ValidatedStep> {
        println("[StepValidator] START Analysis - Raw Steps: ${rawSteps.size}")
        if (rawSteps.isEmpty()) return emptyList()

        // 1. Separate Sources
        val phoneSteps = rawSteps.filter { it.source == StepSource.PHONE }
        val wearableSteps = rawSteps.filter { it.source == StepSource.WEARABLE }
        println("[StepValidator] Sources -> Phone: ${phoneSteps.size}, Wearable: ${wearableSteps.size}")

        // 2. Clean Phone Steps (Ghost Step Logic)
        val cleanedPhoneSteps = cleanPhoneSteps(phoneSteps)
        println("[StepValidator] Phone Cleaning -> Removed ${phoneSteps.size - cleanedPhoneSteps.size} ghost steps. Remaining: ${cleanedPhoneSteps.size}")

        // 3. Merge Logic (Temporal Prioritization)
        // Wearable steps are "Ground Truth" for the time periods they cover.
        // Any phone step overlapping with a Wearable interval is discarded.
        val mergedSteps = mergeSteps(cleanedPhoneSteps, wearableSteps)
        println("[StepValidator] Merging -> Total Final Steps: ${mergedSteps.size}")

        return mergedSteps.map { 
             ValidatedStep(
                startTime = it.startTime,
                endTime = it.endTime,
                effectiveCount = it.rawCount
            )
        }.sortedBy { it.startTime }
    }

    private suspend fun cleanPhoneSteps(steps: List<RawStep>): List<RawStep> {
        val validSteps = mutableListOf<RawStep>()

        for (step in steps) {
            val midPoint = java.time.Instant.ofEpochMilli((step.startTime.toEpochMilli() + step.endTime.toEpochMilli()) / 2)
            
            // Check for activity within +/- 2 minute (widened window)
            // If the app was killed, we might have gaps.
            val closestActivity = activityLogRepository.getActivityClosestTo(midPoint, java.time.Duration.ofMinutes(2))

            if (closestActivity != null) {
                val isGhost = when {
                    // TILTING: Always discard
                    closestActivity.type == UserActivityType.TILTING -> {
                         println("[StepValidator] Ghost Step Detected! (TILTING) at ${step.startTime}")
                         true
                    }
                    
                    // STILL > 60%: Discard (Lowered threshold from 70%)
                    closestActivity.type == UserActivityType.STILL && closestActivity.confidence > 60 -> {
                        println("[StepValidator] Ghost Step Detected! (STILL ${closestActivity.confidence}%) at ${step.startTime}")
                        true
                    }
                    
                    // UNKNOWN & Low Count: Discard
                    closestActivity.type == UserActivityType.UNKNOWN && step.rawCount < 10 -> {
                        println("[StepValidator] Ghost Step Detected! (UNKNOWN + Low Count) at ${step.startTime}")
                        true
                    }
                    
                    else -> false
                }

                if (!isGhost) {
                    validSteps.add(step)
                }
            } else {
                // NO ACTIVITY CONTEXT RULES
                // Default to KEEPING the step if we have no evidence it's a ghost step.
                // Previously this discarded data (Silent Data Killer).
                validSteps.add(step) 
                
                if (step.rawCount > 100) {
                     // Log suspicious high counts in void, but do not delete.
                     println("[StepValidator] [WARNING] High Step Count (${step.rawCount}) without Context at ${step.startTime}")
                }
            }
        }
        return validSteps
    }

    private fun mergeSteps(phoneSteps: List<RawStep>, wearableSteps: List<RawStep>): List<RawStep> {
        val result = mutableListOf<RawStep>()
        
        // 1. Prioritize Wearable: Always keep them
        result.addAll(wearableSteps)

        // 2. Filter Phone Steps
        // Optimization: If wearable covers the whole timeline, we can skip all phone checks?
        // But steps are discrete.
        
        for (phoneStep in phoneSteps) {
            // Check if THIS phone step overlaps with ANY wearable step
            // Enhanced Overlap: Add 1 second buffer to timestamps to catch near-misses
            val overlapsWithWearable = wearableSteps.any { wearableStep ->
                stepsOverlap(phoneStep, wearableStep)
            }

            if (!overlapsWithWearable) {
                result.add(phoneStep)
            } else {
                // println("[StepValidator] Merge Conflict! Discarding Phone Step at ${phoneStep.startTime} due to Wearable overlap.")
            }
        }
        
        return result
    }

    private fun stepsOverlap(s1: RawStep, s2: RawStep): Boolean {
        // Strict timestamp comparison matches exactly the intervals.
        // If s1 (10:00-10:01) and s2 (10:00-10:01) -> Overlap.
        // If s1 (10:00-10:01) and s2 (10:01-10:02) -> No Overlap (EndA == StartB)
        
        // We add a small buffer (e.g. 1 sec) to handle clock skew or alignment issues.
        // s1 Start < s2 End + Buffer && s2 Start < s1 End + Buffer
        
        val buffer = 1000L // 1 second
        val s1Start = s1.startTime.toEpochMilli()
        val s1End = s1.endTime.toEpochMilli()
        val s2Start = s2.startTime.toEpochMilli()
        val s2End = s2.endTime.toEpochMilli()

        return (s1Start < (s2End + buffer)) && (s2Start < (s1End + buffer))
    }
}
