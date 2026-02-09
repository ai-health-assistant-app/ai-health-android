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
            // ViewModel scoped to the navigation graph
            val parentEntry = remember(entry) { navController.getBackStackEntry("dashboard_graph") }
            val viewModel: DashboardViewModel = hiltViewModel(parentEntry)
            
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
            // Get the DashboardViewModel scoped to the navigation graph
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("dashboard_graph") }
            val dashboardViewModel: DashboardViewModel = hiltViewModel(parentEntry)
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
            
            
            if (type == "sleep" && state.sleepNights.isNotEmpty()) {
                // Determine initial date from selected session
                val initialDate = state.selectedSleepSession?.let { session ->
                    session.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                }
                
                SleepDetailScreen(
                    sleepNights = state.sleepNights,
                    initialDate = initialDate,
                    isLoadingMore = state.isLoadingMoreSleep,
                    onPageChanged = { currentPage, totalPages ->
                        dashboardViewModel.onPageChanged(currentPage, totalPages)
                    },
                    onBack = onBack
                )
            } else {
                // History data for other charts
                DetailChartScreen(
                    metricType = type,
                    data = historyData,
                    onBack = onBack
                )
            }
        }
    }
}
