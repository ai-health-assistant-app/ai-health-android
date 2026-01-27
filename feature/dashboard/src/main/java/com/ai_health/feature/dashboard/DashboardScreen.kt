package com.ai_health.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onMetricClick: (String) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = state.isLoading,
                    containerColor = Color(0xFF1E293B),
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intestazione
                item {
                    Text(
                        "Health Dashboard",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // Griglia delle metriche
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        // Riga 1: Passi e Sonno
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(
                                title = "Passi",
                                value = state.stepsFormatted,
                                icon = "🚶",
                                modifier = Modifier.weight(1f)
                            ) { onMetricClick("steps") }

                            MetricCard(
                                title = "Sonno",
                                value = state.sleepTimeFormatted,
                                icon = "😴",
                                modifier = Modifier.weight(1f)
                            ) { onMetricClick("sleep") }
                        }

                        // Riga 2: Battito e Calorie
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(
                                title = "Battito",
                                value = state.heartRateFormatted,
                                icon = "❤️",
                                modifier = Modifier.weight(1f)
                            ) { onMetricClick("hr") }

                            MetricCard(
                                title = "Calorie",
                                value = state.caloriesFormatted,
                                icon = "🔥",
                                modifier = Modifier.weight(1f)
                            ) { onMetricClick("cal") }
                        }

                        // Riga 3: Distanza e Ossigeno
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(
                                title = "Distanza",
                                value = state.distanceFormatted,
                                icon = "📍",
                                modifier = Modifier.weight(1f)
                            ) { onMetricClick("dist") }

                            MetricCard(
                                title = "Ossigeno",
                                value = state.oxygenFormatted,
                                icon = "🫁",
                                modifier = Modifier.weight(1f)
                            ) { onMetricClick("ox") }
                        }
                    }
                }
            }
        }
    }
}

// Componente UI riutilizzabile
@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$icon $title", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}
