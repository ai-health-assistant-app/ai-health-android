package com.ai_health.core.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthSyncScheduler - Schedula la sincronizzazione foreground-first.
 * 
 * PRIVACY-PROOF FOREGROUND-FIRST STRATEGY:
 * - Attiva la sync quando l'app viene aperta (OneTimeWorkRequest)
 * - Non richiede READ_HEALTH_DATA_IN_BACKGROUND
 * - L'utente sa che l'app legge i dati solo quando è aperta
 * - Semplicità per approvazione Play Store
 */
@Singleton
class HealthSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    companion object {
        private const val TAG = "HealthSyncScheduler"
        private const val UNIQUE_WORK_NAME = "health_foreground_sync"
    }
    
    /**
     * Schedula una sync one-time.
     * Chiamato all'apertura dell'app (es. da Application.onCreate o da ViewModel).
     * 
     * ExistingWorkPolicy.KEEP: Se una sync è già in corso, non ne avvia un'altra.
     */
    fun scheduleForegroundSync() {
        Log.d(TAG, "Scheduling foreground health sync")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)  // No network needed, tutto locale
            .build()
        
        val request = OneTimeWorkRequestBuilder<HealthSyncWorker>()
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
        
        Log.d(TAG, "Foreground sync work enqueued")
    }
    
    /**
     * Forza una nuova sync anche se una è già schedulata.
     * Usato per il pulsante "Sincronizza" manuale nell'UI.
     */
    fun forceSync() {
        Log.d(TAG, "Forcing health sync (replacing existing)")
        
        val request = OneTimeWorkRequestBuilder<HealthSyncWorker>().build()
        
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
    
    /**
     * Cancella tutti i lavori di sync pending.
     * Usato per "Elimina e Disconnetti".
     */
    fun cancelAllSync() {
        Log.d(TAG, "Cancelling all sync work")
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
