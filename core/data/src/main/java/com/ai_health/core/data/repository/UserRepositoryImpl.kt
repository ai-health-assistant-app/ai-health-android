package com.ai_health.core.data.repository

import com.ai_health.core.data.local.dao.UserDao
import com.ai_health.core.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override val userProfile: Flow<UserProfileEntity> = userDao.getUserProfile().map { 
        it ?: UserProfileEntity() 
    }

    override suspend fun saveUser(user: UserProfileEntity) {
        userDao.insertUser(user)
    }
}
