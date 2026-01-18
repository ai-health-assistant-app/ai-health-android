package com.ai_health.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Represents a cached health data entry in the local database.
 */
@Entity(
    tableName = "health_cache",
    indices = [Index(value = ["type", "startTime", "endTime", "sourceApp"], unique = true)])
data class HealthCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String,
    val value: Double,
    val startTime: Long,
    val endTime: Long,
    val sourceApp: String,
    val metadata: String? = null
)
