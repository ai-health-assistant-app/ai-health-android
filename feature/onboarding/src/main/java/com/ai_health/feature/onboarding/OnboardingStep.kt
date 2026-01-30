package com.ai_health.feature.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a step in the onboarding wizard
 */
sealed class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissionType: PermissionType
) {
    data object HealthConnect : OnboardingStep(
        title = "Health Connect",
        description = "Connect to Health Connect to sync your health data from various sources for comprehensive tracking.",
        icon = Icons.Default.FitnessCenter,
        permissionType = PermissionType.HealthConnectPermission
    )

    data object UsageStats : OnboardingStep(
        title = "Usage Statistics",
        description = "Grant usage access to help us understand your app usage patterns and provide better insights.",
        icon = Icons.Default.PhoneAndroid,
        permissionType = PermissionType.UsageStatsPermission
    )

    data object Notifications : OnboardingStep(
        title = "Notifications",
        description = "Enable notifications to receive timely health reminders and important updates.",
        icon = Icons.Default.Notifications,
        permissionType = PermissionType.RuntimePermission(android.Manifest.permission.POST_NOTIFICATIONS)
    )

    data object ActivityRecognition : OnboardingStep(
        title = "Activity Recognition",
        description = "Allow the app to detect when you are driving or stationary to improve step accuracy.",
        icon = Icons.Default.FitnessCenter, // Reuse icon or find better one if available
        permissionType = PermissionType.RuntimePermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
    )

    companion object {
        fun getAllSteps(): List<OnboardingStep> = listOf(
            HealthConnect,
            UsageStats,
            Notifications,
            ActivityRecognition
        )
    }
}

/**
 * Types of permissions requested during onboarding
 */
sealed class PermissionType {
    data class RuntimePermission(val permission: String) : PermissionType()
    data object HealthConnectPermission : PermissionType()
    data object UsageStatsPermission : PermissionType()
}

/**
 * Status of a permission
 */
sealed class PermissionStatus {
    data object NotRequested : PermissionStatus()
    data object Granted : PermissionStatus()
    data object Denied : PermissionStatus()
    data object PermanentlyDenied : PermissionStatus()
}
