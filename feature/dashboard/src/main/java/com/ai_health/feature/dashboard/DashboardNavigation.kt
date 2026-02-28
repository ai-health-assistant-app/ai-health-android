package com.ai_health.feature.dashboard

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.dashboardGraph(
    navController: androidx.navigation.NavHostController,
    sharedTransitionScope: SharedTransitionScope, // <--- Importante
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

            DashboardScreen(
                viewModel = viewModel,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this, // 'this' è AnimatedVisibilityScope
                onMetricClick = onMetricClick,
                onChatClick = {
                    navController.navigate("chat")
                }
            )
        }

        // Chat Screen
        composable(route = "chat") {
            ChatScreen(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this, // 'this' è AnimatedVisibilityScope
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Detail chart screen (Chart logic remains same)
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

            // ... (Logica dati invariata) ...
            val historyData = when(type) {
                "steps" -> state.stepsHistory
                "sleep" -> state.sleepHistory
                "hr" -> state.heartRateHistory
                "cal" -> state.caloriesHistory
                "dist" -> state.distanceHistory
                "ox" -> state.oxygenHistory
                else -> emptyList()
            }

            if (type == "sleep") {
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
            } else if (type == "hr") {
                HeartRateDetailScreen(
                    biometricReport = state.biometricReport,
                    heartRateFormatted = state.heartRateFormatted,
                    oxygenFormatted = state.oxygenFormatted,
                    isLoading = state.isBiometricLoading,
                    onBack = onBack
                )
            } else {
                DetailChartScreen(
                    metricType = type,
                    data = historyData,
                    onBack = onBack
                )
            }
        }
    }
}