package com.ai_health.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import com.ai_health.assistant.navigation.AppNavHost
import com.ai_health.core.data.sync.HealthSyncScheduler
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
    lateinit var syncScheduler: HealthSyncScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [IMPORTANTE] Controllo immediato dell'Intent
        checkHealthConnectRationale(intent)

        HealthConnectClient.getSdkStatus(this)

        // Health Connect SDK availability check
        // Note: This check is performed but not currently used
        // Future enhancement: Show appropriate UI if SDK is unavailable
        val availability = HealthConnectClient.getSdkStatus(this)
        
        // NOTE: Sync is now triggered from OnboardingViewModel/DashboardViewModel
        // AFTER Health Connect permissions are confirmed

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

    // [IMPORTANTE] Gestione onNewIntent se l'activity è già aperta
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        checkHealthConnectRationale(intent)
    }

    // [IMPORTANTE] Logica di visualizzazione Rationale
    private fun checkHealthConnectRationale(intent: android.content.Intent?) {
        val rationaleAction = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
        val systemRationaleAction = "android.health.connect.action.SHOW_PERMISSIONS_RATIONALE"

        if (intent?.action == rationaleAction || intent?.action == systemRationaleAction) {
            // Mostriamo un dialogo di sistema semplice
            android.app.AlertDialog.Builder(this)
                .setTitle("Dati Salute e Privacy")
                .setMessage("Spiegazione di come l'app utilizza i dati Health Connect...") // Personalizza il messaggio
                .setPositiveButton("Ho capito") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }
}