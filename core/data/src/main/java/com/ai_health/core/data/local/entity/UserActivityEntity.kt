package com.ai_health.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "user_activity_log")
data class UserActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val activityType: String, // Stored as String (name of UserActivityType)
    val confidence: Int
)
