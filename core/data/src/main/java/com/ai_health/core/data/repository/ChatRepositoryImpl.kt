package com.ai_health.core.data.repository

import com.ai_health.core.data.remote.ChatApi
import com.ai_health.core.data.remote.model.*
import com.ai_health.core.domain.repository.ChatRepository
import com.ai_health.core.domain.model.ChatRequestDomain
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi
) : ChatRepository {

    override suspend fun sendMessage(requestDomain: ChatRequestDomain): Result<String> {
        return try {
            val contextDto = requestDomain.context?.let { ctx ->
                ChatContextDto(
                    readiness = ctx.readiness?.let { ReadinessContextDto(it.score) },
                    sleep = ctx.sleep?.let { s -> 
                        SleepContextDto(s.score, s.deepSleepDurationMin, s.remSleepDurationMin, s.totalDurationMin) 
                    },
                    stress = ctx.stress?.let { StressContextDto(it.baevskyIndex) },
                    trainingLoad = ctx.trainingLoad?.let { tl -> 
                        TrainingLoadContextDto(tl.tsbForm, tl.fatigueAtl) 
                    }
                )
            }
            
            val messageDtos = requestDomain.messages.map { 
                ChatMessageDto(it.role, it.content) 
            }
            
            val request = ChatRequest(context = contextDto, messages = messageDtos)
            val response = api.sendMessage(request)
            Result.success(response.response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
