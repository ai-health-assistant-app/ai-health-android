package com.ai_health.feature.onboarding

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Onboarding feature navigation graph extension.
 * 
 * This function encapsulates the navigation logic for the onboarding feature,
 * following the principle of feature module ownership of navigation.
 * 
 * @param onOnboardingFinished Callback invoked when onboarding is completed,
 *                             typically used to navigate to the dashboard.
 */
fun NavGraphBuilder.onboardingGraph(
    onOnboardingFinished: () -> Unit
) {
    composable(route = "onboarding") {
        // ViewModel injection happens HERE within the feature module,
        // not in MainActivity - following proper dependency injection principles
        val viewModel: OnboardingViewModel = hiltViewModel()
        
        OnboardingScreen(
            onOnboardingComplete = onOnboardingFinished
        )
    }
}
