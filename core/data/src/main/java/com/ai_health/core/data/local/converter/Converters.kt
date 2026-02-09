package com.ai_health.core.data.local.converter

import androidx.room.TypeConverter
import com.ai_health.core.data.local.entity.HeartRateSample
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Room TypeConverters per tipi custom.
 * 
 * HeartRateSample usa Kotlin Serialization per JSON offset-based:
 * [{"o":0,"v":72},{"o":1000,"v":74},...]
 */
class Converters {
    
    // --- Instant <-> Long ---
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }
    
    // --- HeartRateSample List <-> JSON String ---
    // Usato per HeartRateSessionEntity.samplesJson
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @TypeConverter
    fun fromSamplesList(samples: List<HeartRateSample>?): String? {
        return samples?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun toSamplesList(jsonString: String?): List<HeartRateSample>? {
        return jsonString?.let { json.decodeFromString(it) }
    }
}

