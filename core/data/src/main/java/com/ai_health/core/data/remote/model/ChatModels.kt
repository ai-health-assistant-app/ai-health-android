package com.ai_health.core.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("context") val context: ChatContextDto?,
    @SerializedName("messages") val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatContextDto(
    @SerializedName("readiness") val readiness: ReadinessContextDto?,
    @SerializedName("sleep") val sleep: SleepContextDto?,
    @SerializedName("stress") val stress: StressContextDto?,
    @SerializedName("training_load") val trainingLoad: TrainingLoadContextDto?
)

data class ReadinessContextDto(
    @SerializedName("score") val score: Int
)

data class SleepContextDto(
    @SerializedName("score") val score: Int,
    @SerializedName("deep_sleep_duration_min") val deepSleepDurationMin: Long,
    @SerializedName("rem_sleep_duration_min") val remSleepDurationMin: Long,
    @SerializedName("total_duration_min") val totalDurationMin: Long
)

data class StressContextDto(
    @SerializedName("baevsky_index") val baevskyIndex: Double
)

data class TrainingLoadContextDto(
    @SerializedName("tsb_form") val tsbForm: Double,
    @SerializedName("fatigue_atl") val fatigueAtl: Double
)

data class ChatResponse(
    @SerializedName("response") val response: String
)
