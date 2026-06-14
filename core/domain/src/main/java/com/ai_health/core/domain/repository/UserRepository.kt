package com.ai_health.core.domain.repository

import com.ai_health.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val userProfile: Flow<User>
    suspend fun saveUser(user: User)
    
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
    suspend fun getAuthToken(): String?
}