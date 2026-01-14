package com.ai_health.assistant.presentation

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

/**
 * Main entry point for the application.
 *
 * This class is responsible for initializing the core UI components, managing the navigation
 * state between different screens (Welcome, Permissions, and Dashboard), and handling the
 * integration with the Health Connect SDK. It serves as the primary host for the Jetpack Compose
 * UI and coordinates the initial data fetching from Health Connect.
 */
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is starting.
     *
     * This method initializes the activity's content view using Jetpack Compose, sets up
     * the [NavHost] for application routing, and checks the status of the Health Connect SDK.
     * It also handles the orchestration of permission requests and dashboard data loading.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     * being shut down then this Bundle contains the data it most recently supplied in
     * onSaveInstanceState(Bundle). Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val availability = HealthConnectClient.getSdkStatus(this)
        val healthConnectManager = HealthConnectManager(this)
        
        setContent {
            AssistantTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                
                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(
                            onGetStarted = {
                                if (availability == HealthConnectClient.SDK_UNAVAILABLE) {
                                    Toast.makeText(context, "Install Health Connect to continue", Toast.LENGTH_LONG).show()
                                    navController.navigate("dashboard")
                                } else {
                                    navController.navigate("permissions")
                                }
                            }
                        )
                    }
                    composable("permissions") {
                        val launcher = rememberLauncherForActivityResult(
                            PermissionController.createRequestPermissionResultContract()
                        ) { granted ->
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
                        var stepsCount by remember { mutableStateOf<Int?>(null) }

                        // Load ALL data when entering the dashboard
                        LaunchedEffect(Unit) {
                            Log.d("MainActivity", "Starting dashboard data loading...")
                            if (healthConnectManager.hasAllPermissions()) {
                                /*val data = healthConnectManager.readAllData()
                                if (data != null) {
                                    // Calculate total steps from the last 7 days (as configured in the manager)
                                    stepsCount = data.steps.sumOf { it.count }.toInt()
                                    Log.d("MainActivity", "Data successfully loaded into the UI!")
                                } else {
                                    Log.e("MainActivity", "Error: readAllData returned null")
                                    stepsCount = 0
                                }*/


                                //DEBUG
                                healthConnectManager.fullResetAndResync()



                                //healthConnectManager.syncAllData()
                                stepsCount = healthConnectManager.getTotalStepsFromCache().toInt()



                                Log.d("MainActivity", "Total steps in cache: $stepsCount")

                            } else {
                                Log.w("MainActivity", "Permissions not granted for Health Connect")
                                stepsCount = 0
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Recent Activity",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                if (stepsCount != null) {
                                    Text(
                                        text = "$stepsCount",
                                        color = Color(0xFF38BDF8),
                                        style = MaterialTheme.typography.displayLarge
                                    )
                                    Text(
                                        text = "steps (last 7 days)",
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                } else {
                                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
