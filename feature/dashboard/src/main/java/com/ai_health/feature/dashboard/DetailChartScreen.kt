package com.ai_health.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ai_health.ui.components.AppBackground
import com.ai_health.ui.components.AppCard
import com.ai_health.ui.components.CardVariant
import com.ai_health.ui.components.SimpleBarChart
import com.ai_health.ui.components.SimpleLineChart
import com.ai_health.ui.components.ChartDataPoint
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme
import com.ai_health.ui.theme.ChartBlue
import com.ai_health.ui.theme.ChartGreen
import com.ai_health.ui.theme.ChartOrange
import com.ai_health.ui.theme.ChartPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailChartScreen(
    metricType: String,
    data: List<ChartDataPoint>,
    onBack: () -> Unit
) {
    // Map metric types to design system colors
    val color = when (metricType) {
        "steps" -> AppTheme.colors.accentBlue
        "hr" -> ChartPink
        "cal" -> ChartOrange
        "ox" -> AppTheme.colors.accentGreen
        else -> AppTheme.colors.accentPurple
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
                title = { Text(title, color = AppTheme.colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.backgroundPrimary
                )
            )
        },
        containerColor = AppTheme.colors.backgroundPrimary
    ) { padding ->
        AppBackground(
            contentPadding = false,
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.space4)
            ) {
                // Chart Section
                AppCard(
                    variant = CardVariant.NORMAL,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Box(modifier = Modifier.padding(AppDimensions.space4)) {
                        if (metricType == "hr" || metricType == "ox") {
                            SimpleLineChart(
                                data = data,
                                color = color,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SimpleBarChart(
                                data = data,
                                color = color,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
