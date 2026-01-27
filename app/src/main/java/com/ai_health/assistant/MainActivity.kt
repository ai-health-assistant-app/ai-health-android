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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Health Connect SDK availability check
        // Note: This check is performed but not currently used
        // Future enhancement: Show appropriate UI if SDK is unavailable
        val availability = HealthConnectClient.getSdkStatus(this)

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
}