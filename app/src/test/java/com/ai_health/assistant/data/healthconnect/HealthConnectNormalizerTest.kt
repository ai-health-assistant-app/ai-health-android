package com.ai_health.assistant.data.healthconnect

import android.util.Log
import com.ai_health.core.health.RawStep
import com.ai_health.core.data.normalization.HealthConnectNormalizer
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class HealthConnectNormalizerTest {

    private val normalizer = HealthConnectNormalizer()

    @Before
    fun setUp() {
        // Mocking android.util.Log per permettere i test sulla JVM
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `normalizeSteps - returns empty list for empty input`() {
        val result = normalizer.normalizeSteps(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `normalizeSteps - accepts phone records normally`() {
        val startTime = Instant.ofEpochMilli(60000) // 1 minuto
        val records = listOf(
            createRawStep(100, startTime, "com.google.android.apps.fitness")
        )

        val result = normalizer.normalizeSteps(records)

        assertEquals(1, result.size)
        assertEquals(100.0, result[0].value, 0.1)
        assertEquals("com.google.android.apps.fitness", result[0].sourceApp)
    }

    @Test
    fun `normalizeSteps - discards watch records with less than 10 steps`() {
        val startTime = Instant.ofEpochMilli(60000)
        val records = listOf(
            createRawStep(5, startTime, "com.xiaomi.hm.health")
        )

        val result = normalizer.normalizeSteps(records)

        assertTrue("Dovrebbe scartare passi < 10 da sorgente watch", result.isEmpty())
    }

    @Test
    fun `normalizeSteps - accepts watch records with 10 or more steps`() {
        val startTime = Instant.ofEpochMilli(60000)
        val records = listOf(
            createRawStep(15, startTime, "com.xiaomi.hm.health")
        )

        val result = normalizer.normalizeSteps(records)

        assertEquals(1, result.size)
        assertEquals(15.0, result[0].value, 0.1)
    }

    @Test
    fun `normalizeSteps - takes max value when both phone and watch records exist in same minute`() {
        val startTime = Instant.ofEpochMilli(60000)
        val records = listOf(
            createRawStep(50, startTime, "com.google.android.apps.fitness"),
            createRawStep(80, startTime, "com.xiaomi.wearable")
        )

        val result = normalizer.normalizeSteps(records)

        assertEquals(1, result.size)
        assertEquals(80.0, result[0].value, 0.1)
        assertEquals("MERGED", result[0].sourceApp)
    }

    @Test
    fun `normalizeSteps - groups by minute and processes separately`() {
        val t1 = Instant.ofEpochMilli(60000)  // Minuto 1
        val t2 = Instant.ofEpochMilli(120000) // Minuto 2
        
        val records = listOf(
            createRawStep(100, t1, "com.google.android.apps.fitness"),
            createRawStep(5, t2, "com.xiaomi.hm.health") // Dovrebbe essere scartato
        )

        val result = normalizer.normalizeSteps(records)

        assertEquals(1, result.size)
        assertEquals(60000L, result[0].startTime)
        assertEquals(100.0, result[0].value, 0.1)
    }

    private fun createRawStep(count: Long, startTime: Instant, packageName: String): RawStep {
        return RawStep(
            count = count,
            startTime = startTime.toEpochMilli(),
            endTime = startTime.plusSeconds(30).toEpochMilli(),
            sourcePackage = packageName
        )
    }
}
