package com.ai_health.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" o "assistant"
    val content: String,
    val timestamp: Long = Instant.now().toEpochMilli()
)
