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
    private lateinit var useCase: ValidateStepCountUseCase

    @Before
    fun setup() {
        activityLogRepository = mockk()
        useCase = ValidateStepCountUseCase(activityLogRepository)
    }

    @Test
    fun `test phone step removed when matching high confidence STILL`() = runTest {
        // Given
        val now = Instant.now()
        val phoneStep = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 100)
        
        val stillActivity = UserActivity(UserActivityType.STILL, 95, now.plusSeconds(30))
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns stillActivity

        // When
        val result = useCase(listOf(phoneStep))

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test phone step removed when matching TILTING`() = runTest {
        // Given
        val now = Instant.now()
        val phoneStep = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 100)
        
        val tiltingActivity = UserActivity(UserActivityType.TILTING, 100, now.plusSeconds(30))
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns tiltingActivity

        // When
        val result = useCase(listOf(phoneStep))

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test phone step kept when matching low confidence STILL`() = runTest {
        // Given
        val now = Instant.now()
        val phoneStep = RawStep(now, now.plusSeconds(60), StepSource.PHONE, 100)
        
        val stillActivity = UserActivity(UserActivityType.STILL, 50, now.plusSeconds(30))
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns stillActivity

        // When
        val result = useCase(listOf(phoneStep))

        // Then
        assertEquals(1, result.size)
        assertEquals(100, result[0].effectiveCount)
    }
    
    @Test
    fun `test phone step removed when overlapping with wearable`() = runTest {
        // Given
        val now = Instant.now()
        // Phone step: 10:00 - 10:05
        val phoneStep = RawStep(now, now.plusSeconds(300), StepSource.PHONE, 50)
        
        // Wearable step: 10:02 - 10:04 (Overlaps!)
        val wearableStep = RawStep(now.plusSeconds(120), now.plusSeconds(240), StepSource.WEARABLE, 20)

        // Mock no activity conflict for phone step to pass phase 1
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns null

        // When
        val result = useCase(listOf(phoneStep, wearableStep))

        // Then
        assertEquals(1, result.size)
        assertEquals(20, result[0].effectiveCount) // Only wearable step remains
    }

    @Test
    fun `test both steps kept when no overlap`() = runTest {
        // Given
        val now = Instant.now()
        // Phone step: 10:00 - 10:05
        val phoneStep = RawStep(now, now.plusSeconds(300), StepSource.PHONE, 50)
        
        // Wearable step: 10:10 - 10:15 (No Overlap)
        val wearableStep = RawStep(now.plusSeconds(600), now.plusSeconds(900), StepSource.WEARABLE, 20)

        // Mock no activity conflict
        coEvery { activityLogRepository.getActivityClosestTo(any(), any()) } returns null

        // When
        val result = useCase(listOf(phoneStep, wearableStep))

        // Then
        assertEquals(2, result.size)
    }
}
