package com.ai_health.core.data.repository

import com.ai_health.core.data.local.dao.ActivityLogDao
import com.ai_health.core.data.local.entity.UserActivityEntity
import com.ai_health.core.domain.model.UserActivityType
import com.ai_health.core.domain.repository.ActivityLogRepository
import java.time.Instant
import javax.inject.Inject

class ActivityLogRepositoryImpl @Inject constructor(
    private val dao: ActivityLogDao
) : ActivityLogRepository {

    override suspend fun getDominantActivity(startTime: Instant, endTime: Instant): UserActivityType {
        // Fetch all logs in the range
        val logs = dao.getActivitiesInRange(startTime, endTime)
        
        if (logs.isEmpty()) {
            return UserActivityType.UNKNOWN
        }

        // Logic to calculate dominant activity:
        // Since we only have point-in-time events with confidence, we can assume an event holds true until the next event.
        // However, given the "range" is likely small (per step duration), simple frequency or max confidence might suffice.
        // Let's implement a duration-based weighted approach assuming events are periodic status updates.
        
        // Simplified approach for this task: return the activity type that appears most frequently in the logs.
        // If the window is small (e.g. 1 min) and we get events every 10s, frequency is a good proxy for duration.
        
        val frequencyMap = logs.groupingBy { it.activityType }.eachCount()
        val dominantTypeString = frequencyMap.maxByOrNull { it.value }?.key

        return try {
            dominantTypeString?.let { UserActivityType.valueOf(it) } ?: UserActivityType.UNKNOWN
        } catch (e: IllegalArgumentException) {
            UserActivityType.UNKNOWN
        }
    }

    override suspend fun logActivity(timestamp: Instant, type: UserActivityType, confidence: Int) {
        android.util.Log.d("ActivityLogRepository", "Inserting log: $type at $timestamp (confidence: $confidence)")
        dao.insertActivityLog(
            UserActivityEntity(
                timestamp = timestamp,
                activityType = type.name,
                confidence = confidence
            )
        )
    }
}
