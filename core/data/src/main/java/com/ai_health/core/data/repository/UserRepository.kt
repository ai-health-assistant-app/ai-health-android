package com.ai_health.core.data.repository

import com.ai_health.core.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val userProfile: Flow<UserProfileEntity>
    suspend fun saveUser(user: UserProfileEntity)
    
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
    suspend fun getAuthToken(): String?
}