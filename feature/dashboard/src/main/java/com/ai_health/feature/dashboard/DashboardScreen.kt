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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.ai_health.ui.util.SourceAppLauncher


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel, // Using VM directly as per typical Hilt setup in NavGraph, or passing state. Previous code passed state. VM is usually passed to Screen or ScreenRoot. 
    // Previous code: `fun DashboardScreen(state: DashboardUiState, ...)`
    // I should provide a Root composable or update this one to take ViewModel if I want to "use collectAsStateWithLifecycle".
    // Usually:
    // @Composable fun DashboardRoute(viewModel: DashboardViewModel = hiltViewModel()) { ... }
    // @Composable fun DashboardScreen(state: DashboardUiState, ...) { ... }
    // User instruction: "Aggiorna il Composable per osservare lo stato reattivo... Usa collectAsStateWithLifecycle() (o collectAsState) per leggere viewModel.uiState."
    // This implies passing VM or Flow to the Screen.
    // I will modify `DashboardScreen` to accept ViewModel OR I need to see where it is called. 
    // I don't see the navigation graph.
    // I will add a `DashboardRoute` or update the signature if appropriate, but keeping `state` argument separates UI from VM.
    // However, the prompt specifically says "Aggiorna il Composable per osservare... viewModel.uiState".
    // I will assume `DashboardScreen` is the entry point or I should make it one.
    // Given the previous signature `state: DashboardUiState`, I'll overload it or change it. 
    // I'll change it to take `viewModel` as implied by the instruction to use `collectAsStateWithLifecycle`.
    
    // Wait, if I change the signature, I break call sites.
    // I'll provide a `DashboardRoute` wrapper inside the file if needed, or just change `DashboardScreen` signature if I assume it's the top level.
    // I'll change the signature to `viewModel: DashboardViewModel` as users often map 1:1 screen files.
    onMetricClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // We can expose onRefresh from VM
    val onRefresh = { viewModel.refreshData() }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                         Text(
                            text = state.lastSyncTimeFormatted.ifEmpty { "Nessun dato" },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )

                        val appName = remember(state.sourcePackage) {
                            SourceAppLauncher.getAppName(context, state.sourcePackage)
                        }

                        OutlinedButton(
                            onClick = { SourceAppLauncher.launchApp(context, state.sourcePackage) },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF38BDF8),
                                containerColor = Color(0xFF1E293B) // Matching the card bg or transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync via $appName")
                        }
                    }
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

// Componente UI riutilizzabile (Unchanged)
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
