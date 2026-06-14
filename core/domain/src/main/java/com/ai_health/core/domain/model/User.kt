package com.ai_health.core.domain.model

data class User(
    val id: Int = 1,
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
