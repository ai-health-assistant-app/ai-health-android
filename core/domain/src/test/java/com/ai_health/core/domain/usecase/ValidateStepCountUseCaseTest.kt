package com.ai_health.core.domain.usecase

import com.ai_health.core.domain.model.RawStep
import com.ai_health.core.domain.model.StepSource
import com.ai_health.core.domain.model.UserActivity
import com.ai_health.core.domain.model.UserActivityType
import com.ai_health.core.domain.repository.ActivityLogRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ValidateStepCountUseCaseTest {

    private lateinit var activityLogRepository: ActivityLogRepository
    private lateinit var healthRepository: com.ai_health.core.domain.repository.HealthRepository
    private lateinit var useCase: ValidateStepCountUseCase

    @Before
    fun setup() {
        activityLogRepository = mockk()
        healthRepository = mockk()
        useCase = ValidateStepCountUseCase(activityLogRepository, healthRepository)

        // Default: No sleep history unless specified
        coEvery { healthRepository.getSleepHistory(any()) } returns kotlinx.coroutines.flow.flowOf(emptyList())
    }

    // --- POLICY 1: TRUST WEARABLE (Silence Assenso) ---

    @Test
    fun `when wearable steps exist, ignore ALL phone steps and return ONLY wearable steps`() = runTest {
        // Given
        val now = Instant.now()
        val phoneStep = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 100)
        val wearableStep = RawStep(now, now.plusSeconds(60), StepSource.WEARABLE, 200)

        // Even if activity says WALKING
        val walkingActivity = UserActivity(UserActivityType.WALKING, 100, now)
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns walkingActivity

        // When
        val result = useCase(listOf(phoneStep, wearableStep))

        // Then
        assertEquals("Should return exactly 1 step (wearable)", 1, result.size)
        assertEquals("Should match wearable count", 200, result[0].effectiveCount)
    }

    @Test
    fun `when wearable steps exist, ignore phone steps even if they don't overlap`() = runTest {
        // Given
        val now = Instant.now()
        // Phone step at 10:00
        val phoneStep = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 50)
        // Wearable step at 12:00
        val wearableStep = RawStep(now.plusSeconds(7200), now.plusSeconds(7260), StepSource.WEARABLE, 300)

        // When
        val result = useCase(listOf(phoneStep, wearableStep))

        // Then
        assertEquals("Should return only the wearable step", 1, result.size)
        assertEquals(300, result[0].effectiveCount)
    }

    // --- POLICY 2: FALLBACK TO PHONE (Activity Validation) ---

    @Test
    fun `fallback - keep phone step if activity is WALKING`() = runTest {
        // Given (No Wearable Steps)
        val now = Instant.now()
        val phoneStep = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 100)
        
        val activity = UserActivity(UserActivityType.WALKING, 100, now)
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns activity

        // When
        val result = useCase(listOf(phoneStep))

        // Then
        assertEquals(1, result.size)
        assertEquals(100, result[0].effectiveCount)
    }

    @Test
    fun `fallback - discard phone step if activity is STILL`() = runTest {
        // Given
        val now = Instant.now()
        val phoneStep = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 100)
        
        val activity = UserActivity(UserActivityType.STILL, 100, now)
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns activity

        // When
        val result = useCase(listOf(phoneStep))

        // Then
        assertTrue("Should discard steps during STILL", result.isEmpty())
    }

    // --- LAZY SYNC SCENARIO ---

    @Test
    fun `lazy sync scenario - switch from filtered phone to full wearable`() = runTest {
        val now = Instant.now()
        
        // --- Phase 1: Morning, Phone Only ---
        val phoneStep1 = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 15)
        val stillActivity = UserActivity(UserActivityType.STILL, 100, now)
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns stillActivity

        val result1 = useCase(listOf(phoneStep1))
        assertTrue("Phase 1: Should have 0 steps (Filtered out 15 phone steps due to STILL)", result1.isEmpty())

        // --- Phase 2: Sync Happens ---
        val wearableStep = RawStep(now, now.plusSeconds(3600), StepSource.WEARABLE, 2000)
        
        val result2 = useCase(listOf(phoneStep1, wearableStep))
        
        assertEquals("Phase 2: Should have 1 step (Wearable)", 1, result2.size)
        assertEquals(2000, result2[0].effectiveCount)
    }

    // --- POLICY 3: SLEEP FILTER ---

    @Test
    fun `sleep filter - remove step overlapping with sleep session`() = runTest {
        // Given
        val now = Instant.now()
        val sleepStart = now
        val sleepEnd = now.plusSeconds(3600) // 1 Hour Sleep
        
        // Step INSIDE sleep session (e.g. at +30m)
        // Even if it's from Wearable (Ground Truth usually) - if Sleep says Sleep, we discard step.
        val wearableStep = RawStep(now.plusSeconds(1800), now.plusSeconds(1860), StepSource.WEARABLE, 50)
        
        // Mock Sleep Data
        val sleepSession = com.ai_health.core.domain.model.SleepSessionRec("WEARABLE", null, null, sleepStart, sleepEnd)
        coEvery { healthRepository.getSleepHistory(any()) } returns kotlinx.coroutines.flow.flowOf(listOf(sleepSession))

        // When
        val result = useCase(listOf(wearableStep))

        // Then
        assertTrue("Should discard step during sleep session", result.isEmpty())
    }

    @Test
    fun `sleep filter - keep step outside sleep session`() = runTest {
        // Given
        val now = Instant.now()
        val sleepStart = now
        val sleepEnd = now.plusSeconds(3600)
        
        // Step AFTER sleep session (e.g. +2h)
        val wearableStep = RawStep(now.plusSeconds(7200), now.plusSeconds(7260), StepSource.WEARABLE, 50)
        
        val sleepSession = com.ai_health.core.domain.model.SleepSessionRec("WEARABLE", null, null, sleepStart, sleepEnd)
        coEvery { healthRepository.getSleepHistory(any()) } returns kotlinx.coroutines.flow.flowOf(listOf(sleepSession))

        // When
        val result = useCase(listOf(wearableStep))

        // Then
        assertEquals(1, result.size)
    }
}
