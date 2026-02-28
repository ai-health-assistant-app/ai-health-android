package com.ai_health.core.domain.repository

import kotlinx.coroutines.flow.Flow

import com.ai_health.core.domain.model.ChatRequestDomain

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequestDomain): Result<String>
}
