package com.ai_health.feature.dashboard

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

/**
 * Dashboard feature navigation graph.
 * 
 * Chat and HR/Sleep detail views are now handled inline within DashboardScreen
 * (ModalBottomSheet for chat, expandable overlay for details).
 * Only Steps/Cal/Dist chart routes remain as separate navigation destinations.
 */
fun NavGraphBuilder.dashboardGraph(
    navController: androidx.navigation.NavHostController,
    onMetricClick: (String) -> Unit,
    onBack: () -> Unit
) {
    navigation(
        route = "dashboard_graph",
        startDestination = "dashboard"
    ) {
        // Main dashboard screen
        composable(route = "dashboard") { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry("dashboard_graph") }
            val viewModel: DashboardViewModel = hiltViewModel(parentEntry)
            // ChatViewModel scoped to dashboard_graph so it persists across the session
            val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)

            DashboardScreen(
                viewModel = viewModel,
                chatViewModel = chatViewModel,
                onMetricClick = onMetricClick,
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        // Settings Screen
        composable(route = "settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Detail chart screen — only for chart-based metrics (steps, cal, dist)
        composable(
            route = "detail/{type}",
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("dashboard_graph") }
            val dashboardViewModel: DashboardViewModel = hiltViewModel(parentEntry)
            val state by dashboardViewModel.uiState.collectAsState()

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
                onBack = onBack
            )
        }
    }
}