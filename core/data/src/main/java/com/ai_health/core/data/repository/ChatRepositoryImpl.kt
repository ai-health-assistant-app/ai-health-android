package com.ai_health.core.data.repository

import com.ai_health.core.data.remote.ChatApi
import com.ai_health.core.data.remote.model.ChatRequest
import com.ai_health.core.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi
) : ChatRepository {

    override suspend fun sendMessage(message: String): Result<String> {
        return try {
            val response = api.sendMessage(ChatRequest(message))
            Result.success(response.response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
