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
            // If there are no activity logs in the requested time range, it's impossible to determine a dominant activity.
            // A potential future improvement could be to query for the last known activity before this time range
            // and assume it continued, but for now, UNKNOWN is the correct response.
            return UserActivityType.UNKNOWN
        }

        // Logic to calculate dominant activity based on DURATION, not frequency.
        // This correctly handles cases where a short-lived but frequent activity (like 'STILL' every 10s)
        // would otherwise dominate a longer, less frequent activity (like 'WALKING' for a full minute).
        val durationMap = mutableMapOf<String, Long>()
        val sortedLogs = logs.sortedBy { it.timestamp }

        // We assume an activity log entry (e.g., 'WALKING' at 10:00:00) defines the user's state
        // until a new entry arrives (e.g., 'STILL' at 10:01:00).
        
        // Calculate the duration for each activity segment between consecutive logs.
        for (i in 0 until sortedLogs.size - 1) {
            val currentLog = sortedLogs[i]
            val nextLog = sortedLogs[i + 1]
            
            // Duration is the time from the current event until the next one starts.
            val duration = nextLog.timestamp.toEpochMilli() - currentLog.timestamp.toEpochMilli()
            durationMap[currentLog.activityType] = (durationMap[currentLog.activityType] ?: 0L) + duration
        }

        // The last activity in the list is assumed to continue until the end of the requested time window.
        val lastLog = sortedLogs.last()
        val lastSegmentDuration = endTime.toEpochMilli() - lastLog.timestamp.toEpochMilli()
        if (lastSegmentDuration > 0) {
            durationMap[lastLog.activityType] = (durationMap[lastLog.activityType] ?: 0L) + lastSegmentDuration
        }
        
        // Note: This logic doesn't account for the time between 'startTime' and the timestamp of the first log.
        // This is a minor inaccuracy that can be addressed by fetching the state before the window.
        
        val dominantTypeString = durationMap.maxByOrNull { it.value }?.key

        return try {
            dominantTypeString?.let { UserActivityType.valueOf(it) } ?: UserActivityType.UNKNOWN
        } catch (e: IllegalArgumentException) {
            UserActivityType.UNKNOWN
        }
    }

    override suspend fun getActivities(startTime: Instant, endTime: Instant): List<com.ai_health.core.domain.model.UserActivity> {
        val entities = dao.getActivitiesInRange(startTime, endTime)
        return entities.map { entity ->
            com.ai_health.core.domain.model.UserActivity(
                type = UserActivityType.valueOf(entity.activityType),
                confidence = entity.confidence,
                timestamp = entity.timestamp
            )
        }
    }

    override suspend fun getActivityClosestTo(timestamp: Instant, tolerance: java.time.Duration): com.ai_health.core.domain.model.UserActivity? {
        // We fetch a small window around the timestamp.
        // Optimization: We could add a specific DAO query for "closest", but for now, range query is safer.
        val startWindow = timestamp.minus(tolerance)
        val endWindow = timestamp.plus(tolerance)
        
        val candidates = dao.getActivitiesInRange(startWindow, endWindow)
        
        if (candidates.isEmpty()) return null

        // Find the one strictly closest in time
        val closestEntity = candidates.minByOrNull { 
            java.time.Duration.between(it.timestamp, timestamp).abs().toMillis()
        }

        return closestEntity?.let { entity ->
             com.ai_health.core.domain.model.UserActivity(
                type = UserActivityType.valueOf(entity.activityType),
                confidence = entity.confidence,
                timestamp = entity.timestamp
            )
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
