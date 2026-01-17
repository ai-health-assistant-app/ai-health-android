package com.ai_health.assistant.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ai_health.assistant.data.repository.HealthCacheEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailChartScreen(
    metricType: String,
    data: List<HealthCacheEntity>,
    onBack: () -> Unit
) {
    val color = when (metricType) {
        "steps" -> Color(0xFF38BDF8)
        "hr" -> Color(0xFFF87171)
        "cal" -> Color(0xFFFB923C)
        "ox" -> Color(0xFF2DD4BF)
        else -> Color(0xFF818CF8)
    }

    val title = when (metricType) {
        "steps" -> "Dettaglio Passi"
        "hr" -> "Battito Cardiaco"
        "cal" -> "Calorie Bruciate"
        "ox" -> "Ossigenazione"
        "sleep" -> "Analisi Sonno"
        else -> "Dettaglio Metrica"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Chart Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().height(300.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (metricType == "hr" || metricType == "ox") {
                        SimpleLineChart(data = data, color = color, modifier = Modifier.fillMaxSize())
                    } else {
                        SimpleBarChart(data = data, color = color, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
