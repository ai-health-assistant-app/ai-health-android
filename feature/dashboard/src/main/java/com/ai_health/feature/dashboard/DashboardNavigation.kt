package com.ai_health.feature.dashboard

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Dashboard feature navigation graph extension.
 * 
 * This function encapsulates the navigation logic for the dashboard feature
 * and its related detail chart screens. The DashboardViewModel is shared
 * between the dashboard and detail screens to maintain state consistency.
 * 
 * @param onMetricClick Callback invoked when a metric card is clicked,
 *                      receives the metric type (e.g., "steps", "sleep").
 * @param onBack Callback invoked when the back button is pressed in detail view.
 */
fun NavGraphBuilder.dashboardGraph(
    onMetricClick: (String) -> Unit,
    onBack: () -> Unit
) {
    // Main dashboard screen
    composable(route = "dashboard") {
        // ViewModel injection within the feature module
        val viewModel: DashboardViewModel = hiltViewModel()
        
        DashboardScreen(
            viewModel = viewModel,
            onMetricClick = onMetricClick
        )
    }
    
    // Detail chart screen
    composable(
        route = "detail/{type}",
        arguments = listOf(
            navArgument("type") { 
                type = NavType.StringType
                nullable = false
            }
        )
    ) { backStackEntry ->
        // Get the DashboardViewModel to access historical data
        // Since detail screen is part of the dashboard graph, 
        // we use hiltViewModel() which will share the instance
        val dashboardViewModel: DashboardViewModel = hiltViewModel()
        val state by dashboardViewModel.uiState.collectAsState()
        
        // Extract metric type from navigation arguments
        val type = backStackEntry.arguments?.getString("type") ?: ""
        
        // Map metric type to corresponding historical data
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
