package com.ai_health.assistant.domain.model

data class User(val name: String,
                val surname: String,
                val email: String,
                val birthDate: String,
                val gender: String,
                val weight: Double,
                val height: Double)
