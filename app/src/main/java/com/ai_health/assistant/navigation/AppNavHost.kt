package com.ai_health.assistant.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.ai_health.feature.dashboard.dashboardGraph
import com.ai_health.feature.onboarding.onboardingGraph

/**
 * Main navigation orchestrator for the application.
 * 
 * This composable manages the entire app navigation flow, including:
 * - Determining the initial destination based on onboarding status
 * - Coordinating navigation between feature modules
 * - Handling navigation callbacks from feature graphs
 * 
 * @param modifier Modifier to apply to the NavHost
 * @param navController Optional NavController (defaults to a new instance)
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    
    // Determine start destination based on onboarding completion status
    var startDestination by remember { mutableStateOf("onboarding") }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val onboardingPrefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        val isOnboardingComplete = onboardingPrefs.getBoolean("onboarding_completed", false)
        
        startDestination = if (isOnboardingComplete) "dashboard" else "onboarding"
        isLoading = false
    }
    
    // Only show navigation once we've determined the start destination
    if (!isLoading) {
        NavHost(
            navController = navController,
            startDestination = if (startDestination == "dashboard") "dashboard_graph" else startDestination,
            modifier = modifier
        ) {
            // Onboarding feature navigation graph
            onboardingGraph(
                onOnboardingFinished = {
                    // Navigate to dashboard and clear onboarding from back stack
                    navController.navigate("dashboard_graph") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
            
            // Dashboard feature navigation graph (includes detail screens)
            dashboardGraph(
                navController = navController,
                onMetricClick = { type ->
                    navController.navigate("detail/$type")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
