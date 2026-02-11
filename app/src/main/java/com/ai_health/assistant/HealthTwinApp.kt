package com.ai_health.assistant

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class con Hilt e WorkManager configuration.
 * 
 * IMPORTANTE: Per usare @HiltWorker, l'Application deve implementare
 * Configuration.Provider e usare HiltWorkerFactory.
 */
@HiltAndroidApp
class HealthTwinApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}
