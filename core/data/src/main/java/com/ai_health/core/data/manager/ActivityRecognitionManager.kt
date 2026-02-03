package com.ai_health.core.data.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ai_health.core.data.receiver.ActivityUpdatesReceiver
import com.google.android.gms.location.ActivityRecognition
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val client = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityUpdatesReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission") // Caller must ensure permission
    fun requestUpdates(intervalMillis: Long = 30000L) {
        android.util.Log.d("ServiceStartup", "Manager: requestUpdates called")
        val intent = Intent(context, com.ai_health.core.data.service.ActivityRecognitionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.util.Log.d("ServiceStartup", "Manager: starting Foreground Service")
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    @SuppressLint("MissingPermission")
    fun removeUpdates() {
        val intent = Intent(context, com.ai_health.core.data.service.ActivityRecognitionService::class.java)
        context.stopService(intent)
        stopListening()
    }

    @SuppressLint("MissingPermission")
    fun listenToUpdates(intervalMillis: Long = 30000L) {
         client.requestActivityUpdates(intervalMillis, pendingIntent)
            .addOnSuccessListener {
                android.util.Log.d("ActivityManager", "Updates registered successfully")
            }
            .addOnFailureListener {
                android.util.Log.e("ActivityManager", "Failed to register updates", it)
            }
    }

    @SuppressLint("MissingPermission")
    fun stopListening() {
        client.removeActivityUpdates(pendingIntent)
    }
}
