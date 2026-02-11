package com.ai_health.assistant.navigation

import android.content.Context
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
 */
@OptIn(ExperimentalSharedTransitionApi::class)
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
        // Gestore delle transizioni condivise (necessario per l'animazione della chat)
        SharedTransitionLayout(modifier = modifier) {

            NavHost(
                navController = navController,
                startDestination = if (startDestination == "dashboard") "dashboard_graph" else startDestination,
                enterTransition = {
                    fadeIn(animationSpec = tween(400)) + slideInHorizontally(animationSpec = tween(400), initialOffsetX = { it / 10 })
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(400))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(400))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(400)) + slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { it / 10 })
                }
            ) {
                // Onboarding feature navigation graph
                onboardingGraph(
                    // RIMOSSO navController qui perché la tua funzione non lo richiede
                    onOnboardingFinished = {
                        // Navigate to dashboard and clear onboarding from back stack
                        navController.navigate("dashboard_graph") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )

                // Dashboard feature navigation graph
                dashboardGraph(
                    navController = navController,
                    // Passiamo lo scope per l'animazione della pillola Chat
                    sharedTransitionScope = this@SharedTransitionLayout,
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
}