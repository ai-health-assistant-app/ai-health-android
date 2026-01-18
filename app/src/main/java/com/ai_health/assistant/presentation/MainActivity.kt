package com.ai_health.assistant.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.ai_health.assistant.presentation.dashboard.DashboardScreen
import com.ai_health.assistant.presentation.dashboard.DashboardViewModel
import com.ai_health.assistant.presentation.onboarding.HealthPermissionScreen
import com.ai_health.assistant.presentation.onboarding.WelcomeScreen
import com.ai_health.assistant.presentation.ui.theme.AssistantTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // <--- FONDAMENTALE!
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica rapida disponibilità SDK (può restare qui per ora)
        val availability = HealthConnectClient.getSdkStatus(this)

        setContent {
            AssistantTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // Hilt inietta il ViewModel automaticamente.
                // Se ruoti lo schermo, ottieni lo stesso identico oggetto (non perdi i dati).
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                val state by dashboardViewModel.uiState.collectAsState()

                // Decidi dove iniziare
                // Nota: In un'app reale, questo check andrebbe fatto in uno SplashViewModel
                var startDestination by remember { mutableStateOf("welcome") }
                LaunchedEffect(Unit) {
                    val manager = HealthConnectManager(context) // Solo per check permessi iniziale
                    if (manager.hasAllPermissions()) {
                        startDestination = "dashboard"
                        dashboardViewModel.refreshData() // Forza refresh se abbiamo permessi
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
                            dashboardViewModel.refreshData() // Aggiorna i dati dopo aver ottenuto permessi
                            navController.navigate("dashboard") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                        HealthPermissionScreen(
                            onRequestPermissions = {
                                try { launcher.launch(HealthConnectManager.permissions) }
                                catch (e: Exception) { navController.navigate("dashboard") }
                            },
                            onSkip = {
                                navController.navigate("dashboard") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        // Passiamo lo stato e le funzioni del ViewModel alla UI
                        DashboardScreen(
                            state = state,
                            onRefresh = { dashboardViewModel.refreshData() },
                            onMetricClick = { type -> navController.navigate("detail/$type") }
                        )
                    }

                    composable("detail/{type}") { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: ""
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Dettaglio per $type (To be implemented)", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}