package com.ai_health.core.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant
import java.util.UUID

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val source: String,
    val title: String?,
    val notes: String?,
    val startTime: Instant,
    val endTime: Instant,
    val deviceType: String?
)

@Entity(
    tableName = "sleep_stages",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sleepSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sleepSessionId")]
)
data class SleepStageEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val source: String,
    val sleepSessionId: String,
    val stage: Int,
    val startTime: Instant,
    val endTime: Instant,
    val deviceType: String?
)

data class SleepSessionWithStages(
    @Embedded
    val session: SleepSessionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "sleepSessionId"
    )
    val stages: List<SleepStageEntity>
)
