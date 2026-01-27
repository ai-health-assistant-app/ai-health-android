package com.ai_health.assistant.navigation

/**
 * Type-safe navigation routes for the application.
 * 
 * This sealed class provides compile-time safety for navigation,
 * preventing typos and enabling better refactoring support.
 */
sealed class Screen(val route: String) {
    /**
     * Onboarding flow route - shown on first app launch
     */
    data object Onboarding : Screen("onboarding")
    
    /**
     * Main dashboard route - shows health metrics overview
     */
    data object Dashboard : Screen("dashboard")
    
    /**
     * Detail chart route - shows historical data for a specific metric
     * 
     * @param type The metric type (e.g., "steps", "sleep", "hr", "cal", "dist", "ox")
     */
    data class Detail(val type: String) : Screen("detail/{type}") {
        /**
         * Creates a navigation route with the actual metric type value
         */
        fun createRoute(type: String) = "detail/$type"
        
        companion object {
            /**
             * Base route pattern for navigation graph definition
             */
            const val ROUTE_PATTERN = "detail/{type}"
        }
    }
}
