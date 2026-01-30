package com.ai_health.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import com.ai_health.assistant.navigation.AppNavHost
import com.ai_health.ui.theme.AssistantTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for the AI Health Assistant application.
 * 
 * This Activity has been refactored to follow proper architecture patterns:
 * - No direct ViewModel instantiation
 * - No navigation logic (delegated to AppNavHost)
 * - Minimal responsibility: theme setup and SDK availability check
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var activityRecognitionManager: com.ai_health.core.data.manager.ActivityRecognitionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Health Connect SDK availability check
        // Note: This check is performed but not currently used
        // Future enhancement: Show appropriate UI if SDK is unavailable
        HealthConnectClient.getSdkStatus(this)

        // Start Activity Recognition if permission is granted
        startActivityRecognitionIfPossible()

        setContent {
            AssistantTheme {
                // All navigation logic is now handled by AppNavHost
                // Feature modules own their respective navigation graphs
                AppNavHost(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun startActivityRecognitionIfPossible() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                activityRecognitionManager.requestUpdates()
            }
        } else {
            // Pre-Q (API 29), permission is not runtime, so just request
            activityRecognitionManager.requestUpdates()
        }
    }
}