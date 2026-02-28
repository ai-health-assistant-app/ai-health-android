package com.ai_health.core.domain.model

data class ChatRequestDomain(
    val context: ChatContextDomain?,
    val messages: List<ChatMessageDomain>
)

data class ChatMessageDomain(
    val role: String,
    val content: String
)

data class ChatContextDomain(
    val readiness: ReadinessContextDomain?,
    val sleep: SleepContextDomain?,
    val stress: StressContextDomain?,
    val trainingLoad: TrainingLoadContextDomain?
)

data class ReadinessContextDomain(val score: Int)

data class SleepContextDomain(
    val score: Int,
    val deepSleepDurationMin: Long,
    val remSleepDurationMin: Long,
    val totalDurationMin: Long
)

data class StressContextDomain(val baevskyIndex: Double)

data class TrainingLoadContextDomain(
    val tsbForm: Double,
    val fatigueAtl: Double
)
