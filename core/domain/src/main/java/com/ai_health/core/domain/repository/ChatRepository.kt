package com.ai_health.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(message: String): Result<String>
}
