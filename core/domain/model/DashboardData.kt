package com.ai_health.assistant.domain.model

// Questo oggetto contiene solo i dati puri da mostrare nella UI.
// Lo mettiamo nel package "domain" perché è indipendente da database o sensori.
data class DashboardData(
    val steps: Int = 0,
    val sleepMinutes: Int = 0,
    val avgHeartRate: Int = 0,
    val calories: Int = 0,
    val distanceKm: Double = 0.0,
    val oxygenSaturation: Double = 0.0
)