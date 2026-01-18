package com.ai_health.core.data.normalization

import com.ai_health.core.health.RawStep
import com.ai_health.core.data.local.HealthCacheEntity
import javax.inject.Inject

class HealthConnectNormalizer @Inject constructor() {

    fun normalizeSteps(records: List<RawStep>): List<HealthCacheEntity> {
        val groupedRecords = mutableMapOf<Long, HealthCacheEntity>()

        for (record in records) {
            val packageName = record.sourcePackage
            val count = record.count

            // Rule 1: Discard watch records with less than 10 steps
            val isGoogleFit = packageName == "com.google.android.apps.fitness"
            if (!isGoogleFit && count < 10) {
                continue
            }

            val startTime = record.startTime
            val existing = groupedRecords[startTime]

            val newValue = count.toDouble()
            
            // Rule 2: Take max value when conflicts exist in same minute (startTime)
            if (existing == null || newValue > existing.value) {
                groupedRecords[startTime] = HealthCacheEntity(
                    type = "STEPS",
                    value = newValue,
                    startTime = startTime,
                    endTime = record.endTime,
                    sourceApp = if (existing != null && existing.sourceApp != packageName) "MERGED" else packageName,
                    metadata = null
                )
            } else {
                 if (existing.sourceApp != packageName) {
                     groupedRecords[startTime] = existing.copy(sourceApp = "MERGED")
                 }
            }
        }

        return groupedRecords.values.toList()
    }
}
