package com.ai_health.core.domain.repository

import com.ai_health.core.domain.model.UserActivity
import com.ai_health.core.domain.model.UserActivityType
import java.time.Instant

interface ActivityLogRepository {
    /**
     * Retrieves the dominant activity during the specified time range.
     * "Dominant" is defined as the activity that occupies the most time within the window.
     *
     * @param startTime The start of the time range.
     * @param endTime The end of the time range.
     * @return The UserActivityType representing the dominant activity, or UNKNOWN if no data.
     */
    @Deprecated("Use getActivities instead")
    suspend fun getDominantActivity(startTime: Instant, endTime: Instant): UserActivityType

    suspend fun getActivities(startTime: Instant, endTime: Instant): List<UserActivity>

    suspend fun getActivityClosestTo(timestamp: Instant, tolerance: java.time.Duration): UserActivity?


    /**
     * Logs a new user activity event.
     *
     * @param timestamp The time the activity was detected.
     * @param type The type of activity.
     * @param confidence The confidence level of the detection (0-100).
     */
    suspend fun logActivity(timestamp: Instant, type: UserActivityType, confidence: Int)
}
