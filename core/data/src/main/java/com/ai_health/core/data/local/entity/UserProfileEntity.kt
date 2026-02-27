package com.ai_health.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1, // Single user supported for now
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val uid: String = "",
    val birthDate: String = "",
    val gender: String = "Male",
    val weight: Float = 0f,
    val height: Int = 0
)