package com.ai_health.core.data.remote

import com.ai_health.core.data.remote.model.ChatRequest
import com.ai_health.core.data.remote.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {
    @POST("chat") // Assumed endpoint, to be verified
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse
}
