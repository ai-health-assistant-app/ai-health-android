package com.ai_health.assistant

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ai_health.core.health.HealthConnectManager
import com.ai_health.feature.dashboard.DashboardScreen
import com.ai_health.feature.dashboard.DashboardViewModel
import com.ai_health.feature.onboarding.HealthPermissionScreen
import com.ai_health.feature.onboarding.WelcomeScreen
import com.ai_health.ui.theme.AssistantTheme
import dagger.hilt.android.AndroidEntryPoint

import com.ai_health.feature.dashboard.DetailChartScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Health Connect SDK check
        val availability = HealthConnectClient.getSdkStatus(this)

        setContent {
            AssistantTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                val state by dashboardViewModel.uiState.collectAsState()

                // Decide start destination
                var startDestination by remember { mutableStateOf("welcome") }
                LaunchedEffect(Unit) {
                    val manager = HealthConnectManager(context)
                    if (manager.hasAllPermissions()) {
                        startDestination = "dashboard"
                        dashboardViewModel.refreshData()
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    composable("welcome") {
                        WelcomeScreen(onGetStarted = {
                            if (availability == HealthConnectClient.SDK_UNAVAILABLE) {
                                Toast.makeText(context, "Install Health Connect", Toast.LENGTH_LONG).show()
                            } else {
                                navController.navigate("permissions")
                            }
                        })
                    }

                    composable("permissions") {
                        val launcher = rememberLauncherForActivityResult(
                            PermissionController.createRequestPermissionResultContract()
                        ) {
                            dashboardViewModel.refreshData()
                            navController.navigate("dashboard") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                        HealthPermissionScreen(
                            onRequestPermissions = {
                                try {
                                    launcher.launch(HealthConnectManager.permissions)
                                } catch (e: Exception) {
                                    navController.navigate("dashboard")
                                }
                            },
                            onSkip = {
                                navController.navigate("dashboard") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            state = state,
                            onRefresh = { dashboardViewModel.refreshData() },
                            onMetricClick = { type -> navController.navigate("detail/$type") }
                        )
                    }



                    composable("detail/{type}") { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: ""
                        
                        val historyData = when(type) {
                            "steps" -> state.stepsHistory
                            "sleep" -> state.sleepHistory
                            "hr" -> state.heartRateHistory
                            "cal" -> state.caloriesHistory
                            "dist" -> state.distanceHistory
                            "ox" -> state.oxygenHistory
                            else -> emptyList()
                        }

                        DetailChartScreen(
                            metricType = type,
                            data = historyData,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}