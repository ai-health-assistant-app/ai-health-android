package com.ai_health.assistant.data.healthconnect

import android.util.Log
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.metadata.Device
import com.ai_health.assistant.data.repository.HealthCacheEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthConnectNormalizer {

    private val tagDebug = "NormalizerDebug"
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun normalizeSteps(records: List<StepsRecord>): List<HealthCacheEntity> {
        if (records.isEmpty()) return emptyList()
        val timeline = records.groupBy { it.startTime.toEpochMilli() / 60000 }
        val finalEntities = mutableListOf<HealthCacheEntity>()

        timeline.keys.sorted().forEach { minute ->
            val minuteRecords = timeline[minute] ?: return@forEach
            val watchRecords = minuteRecords.filter { 
                it.metadata.device?.type == Device.TYPE_WATCH || 
                it.metadata.device?.type == Device.TYPE_FITNESS_BAND ||
                it.metadata.dataOrigin.packageName.contains("xiaomi", ignoreCase = true) ||
                it.metadata.dataOrigin.packageName.contains("samsung.health", ignoreCase = true)
            }
            val phoneRecords = minuteRecords.filter { 
                it.metadata.device?.type == Device.TYPE_PHONE || 
                it.metadata.dataOrigin.packageName.contains("apps.fitness", ignoreCase = true) || 
                (it.metadata.device == null && !watchRecords.contains(it))
            }
            
            val watchCount = watchRecords.sumOf { it.count }.toDouble()
            val phoneCount = phoneRecords.sumOf { it.count }.toDouble()

            val finalValue: Double
            val sourceLabel: String

            when {
                watchCount > 0 && phoneCount > 0 -> {
                    finalValue = maxOf(watchCount, phoneCount)
                    sourceLabel = "MERGED"
                }
                watchCount > 0 -> {
                    if (watchCount < 10) { 
                        finalValue = 0.0 
                        sourceLabel = "DISCARDED"
                    } else {
                        finalValue = watchCount
                        sourceLabel = "WEARABLE"
                    }
                }
                phoneCount > 0 -> {
                    finalValue = phoneCount
                    sourceLabel = "PHONE"
                }
                else -> {
                    finalValue = minuteRecords.sumOf { it.count }.toDouble()
                    sourceLabel = "OTHER"
                }
            }

            if (finalValue > 0) {
                finalEntities.add(HealthCacheEntity(
                    type = "STEPS",
                    value = finalValue,
                    startTime = minute * 60000,
                    endTime = (minute + 1) * 60000,
                    sourceApp = sourceLabel
                ))
            }
        }
        return finalEntities
    }

    fun normalizeDistance(records: List<DistanceRecord>): List<HealthCacheEntity> {
        if (records.isEmpty()) return emptyList()
        val timeline = records.groupBy { it.startTime.toEpochMilli() / 60000 }
        return timeline.map { (minute, minuteRecords) ->
            HealthCacheEntity(
                type = "DISTANCE",
                value = minuteRecords.maxOf { it.distance.inMeters },
                startTime = minute * 60000,
                endTime = (minute + 1) * 60000,
                sourceApp = minuteRecords.first().metadata.dataOrigin.packageName
            )
        }
    }

    fun normalizeHeartRate(records: List<HeartRateRecord>): List<HealthCacheEntity> {
        if (records.isEmpty()) return emptyList()
        val allSamples = records.flatMap { record -> record.samples }
        val timeline = allSamples.groupBy { it.time.toEpochMilli() / 60000 }
        return timeline.map { (minute, minuteSamples) ->
            HealthCacheEntity(
                type = "HEART_RATE",
                value = minuteSamples.map { it.beatsPerMinute }.average(),
                startTime = minute * 60000,
                endTime = (minute + 1) * 60000,
                sourceApp = "HEART_RATE"
            )
        }
    }

    fun normalizeOxygen(records: List<OxygenSaturationRecord>): List<HealthCacheEntity> {
        if (records.isEmpty()) return emptyList()
        val timeline = records.groupBy { it.time.toEpochMilli() / 300000 }
        return timeline.map { (period, periodRecords) ->
            HealthCacheEntity(
                type = "OXYGEN_SATURATION",
                value = periodRecords.map { it.percentage.value }.average(),
                startTime = period * 300000,
                endTime = (period + 1) * 300000,
                sourceApp = periodRecords.first().metadata.dataOrigin.packageName
            )
        }
    }

    fun normalizeSleep(records: List<SleepSessionRecord>): List<HealthCacheEntity> {
        if (records.isEmpty()) return emptyList()
        val sortedSessions = records.sortedByDescending { it.endTime.toEpochMilli() - it.startTime.toEpochMilli() }
        val finalSessions = mutableListOf<SleepSessionRecord>()
        for (session in sortedSessions) {
            if (finalSessions.none { session.startTime.isBefore(it.endTime) && session.endTime.isAfter(it.startTime) }) {
                finalSessions.add(session)
            }
        }
        return finalSessions.map { session ->
            HealthCacheEntity(
                type = "SLEEP",
                value = (session.endTime.toEpochMilli() - session.startTime.toEpochMilli()).toDouble() / 60000.0,
                startTime = session.startTime.toEpochMilli(),
                endTime = session.endTime.toEpochMilli(),
                sourceApp = session.metadata.dataOrigin.packageName
            )
        }
    }

    fun normalizeCalories(records: List<TotalCaloriesBurnedRecord>): List<HealthCacheEntity> {
        if (records.isEmpty()) return emptyList()
        val timeline = records.groupBy { it.startTime.toEpochMilli() / 900000 }
        return timeline.map { (time, recs) ->
            HealthCacheEntity(
                type = "CALORIES",
                value = recs.maxOf { it.energy.inKilocalories },
                startTime = time * 900000,
                endTime = (time + 1) * 900000,
                sourceApp = recs.first().metadata.dataOrigin.packageName
            )
        }
    }

    fun normalizeExercise(records: List<ExerciseSessionRecord>): List<HealthCacheEntity> {
        return records.map {
            HealthCacheEntity(
                type = "EXERCISE",
                value = it.exerciseType.toDouble(),
                startTime = it.startTime.toEpochMilli(),
                endTime = it.endTime.toEpochMilli(),
                sourceApp = it.metadata.dataOrigin.packageName,
                metadata = it.title
            )
        }
    }

    fun normalizeBMR(records: List<BasalMetabolicRateRecord>): List<HealthCacheEntity> {
        return records.map {
            HealthCacheEntity(
                type = "BMR",
                value = it.basalMetabolicRate.inKilocaloriesPerDay,
                startTime = it.time.toEpochMilli(),
                endTime = it.time.toEpochMilli(),
                sourceApp = it.metadata.dataOrigin.packageName
            )
        }
    }
}
