package com.ai_health.assistant.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.ai_health.assistant.data.healthconnect.HealthConnectManager
import com.ai_health.assistant.presentation.onboarding.HealthPermissionScreen
import com.ai_health.assistant.presentation.onboarding.WelcomeScreen
import com.ai_health.assistant.presentation.ui.theme.AssistantTheme
import com.ai_health.assistant.data.repository.HealthCacheEntity
import com.ai_health.assistant.data.repository.SourceStat
import com.ai_health.assistant.presentation.dashboard.DetailChartScreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val availability = HealthConnectClient.getSdkStatus(this)
        val healthConnectManager = HealthConnectManager(this)
        setContent {
            AssistantTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                var startDestination by remember { mutableStateOf<String?>(null) }

                // State shared for details (to avoid complex navigation arguments)
                val allDataMap = remember { mutableStateMapOf<String, List<HealthCacheEntity>>() }

                LaunchedEffect(Unit) {
                    if (healthConnectManager.hasAllPermissions()) {
                        startDestination = "dashboard"
                    } else {
                        startDestination = "welcome"
                    }
                }

                if (startDestination != null) {
                    NavHost(navController = navController, startDestination = startDestination!!) {
                        composable("welcome") {
                            WelcomeScreen(onGetStarted = {
                                if (availability == HealthConnectClient.SDK_UNAVAILABLE) {
                                    Toast.makeText(context, "Install Health Connect", Toast.LENGTH_LONG).show()
                                    navController.navigate("dashboard")
                                } else navController.navigate("permissions")
                            })
                        }
                        composable("permissions") {
                            val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
                                navController.navigate("dashboard") { popUpTo("welcome") { inclusive = true } }
                            }
                            HealthPermissionScreen(
                                onRequestPermissions = { try { launcher.launch(HealthConnectManager.permissions) } catch (e: Exception) { navController.navigate("dashboard") } },
                                onSkip = { navController.navigate("dashboard") { popUpTo("welcome") { inclusive = true } } }
                            )
                        }
                        composable("dashboard") {
                            val metrics = remember { mutableStateMapOf<String, Any>() }
                            var exercises by remember { mutableStateOf<List<HealthCacheEntity>>(emptyList()) }
                            var sleepStages by remember { mutableStateOf<List<HealthCacheEntity>>(emptyList()) }
                            var stepsBySource by remember { mutableStateOf<List<SourceStat>>(emptyList()) }
                            var isRefreshing by remember { mutableStateOf(false) }
                            val scope = rememberCoroutineScope()
                            val db = com.ai_health.assistant.data.repository.AppDatabase.getDatabase(context)

                            val refreshData = suspend {
                                isRefreshing = true
                                if (healthConnectManager.hasAllPermissions()) {
                                    healthConnectManager.syncAllData()
                                    metrics["steps"] = healthConnectManager.getTotalStepsFromCache()
                                    metrics["sleep"] = healthConnectManager.getTotalSleepFromCache()
                                    metrics["hr"] = healthConnectManager.getAvgHeartRateFromCache()
                                    metrics["cal"] = healthConnectManager.getTotalCaloriesFromCache()
                                    metrics["dist"] = healthConnectManager.getTotalDistanceFromCache() / 1000.0
                                    metrics["ox"] = healthConnectManager.getAvgOxygenFromCache()
                                    
                                    sleepStages = healthConnectManager.getSleepStagesFromCache()
                                    exercises = healthConnectManager.getRecentExercisesFromCache()
                                    stepsBySource = healthConnectManager.getStepsBySourceFromCache()

                                    // Filter data for today (since midnight)
                                    val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                                    // Cache data for detail screens
                                    allDataMap["steps"] = db.healthCacheDao().getRecordsByTime("STEPS", startOfToday)
                                    allDataMap["hr"] = db.healthCacheDao().getRecordsByTime("HEART_RATE", startOfToday)
                                    allDataMap["cal"] = db.healthCacheDao().getRecordsByTime("CALORIES", startOfToday)
                                    allDataMap["ox"] = db.healthCacheDao().getRecordsByTime("OXYGEN_SATURATION", startOfToday)
                                    allDataMap["sleep"] = sleepStages // Already filtered in getSleepStagesFromCache
                                }
                                isRefreshing = false
                            }

                            LaunchedEffect(Unit) { refreshData() }

                            DashboardScreen(
                                metrics = metrics,
                                stages = sleepStages,
                                exercises = exercises,
                                stepsBySource = stepsBySource,
                                isRefreshing = isRefreshing,
                                onRefresh = { scope.launch { refreshData() } },
                                onMetricClick = { type -> navController.navigate("detail/$type") }
                            )
                        }
                        composable("detail/{type}") { backStackEntry ->
                            val type = backStackEntry.arguments?.getString("type") ?: ""
                            DetailChartScreen(
                                metricType = type,
                                data = allDataMap[type] ?: emptyList(),
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    metrics: Map<String, Any>,
    stages: List<HealthCacheEntity>,
    exercises: List<HealthCacheEntity>,
    stepsBySource: List<SourceStat>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMetricClick: (String) -> Unit
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val pullToRefreshState = rememberPullToRefreshState()

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
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Health Dashboard", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Button(
                            onClick = {
                                val packageName = "com.xiaomi.wearable"
                                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Mi Fitness", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard("Passi", "${(metrics["steps"] as? Double)?.toInt() ?: 0}", "🚶", Modifier.weight(1f)) { onMetricClick("steps") }
                            val sleepMin = metrics["sleep"] as? Int ?: 0
                            MetricCard("Sonno", "${sleepMin/60}h ${sleepMin%60}m", "😴", Modifier.weight(1f)) { onMetricClick("sleep") }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard("Battito (Avg)", "${(metrics["hr"] as? Double)?.toInt() ?: 0} bpm", "❤️", Modifier.weight(1f)) { onMetricClick("hr") }
                            MetricCard("Calorie", "${(metrics["cal"] as? Double)?.toInt() ?: 0} kcal", "🔥", Modifier.weight(1f)) { onMetricClick("cal") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard("Distanza", String.format("%.2f km", metrics["dist"] as? Double ?: 0.0), "📍", Modifier.weight(1f)) { onMetricClick("dist") }
                            MetricCard("Ossigeno", String.format("%.1f %%", metrics["ox"] as? Double ?: 0.0), "🫁", Modifier.weight(1f)) { onMetricClick("ox") }
                        }
                    }
                }

                if (exercises.isNotEmpty()) {
                    item { Text("Esercizi Recenti", color = Color.White, style = MaterialTheme.typography.titleLarge) }
                    items(exercises) { ex ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(ex.metadata ?: "Allenamento", color = Color.White)
                                    Text(timeFormatter.format(Instant.ofEpochMilli(ex.startTime)), color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${(ex.endTime - ex.startTime) / 60000} min", color = Color(0xFF38BDF8))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
