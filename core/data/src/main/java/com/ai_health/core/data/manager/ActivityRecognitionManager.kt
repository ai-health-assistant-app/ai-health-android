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

    // Operation Scalpel: Increased default to 3 minutes to reduce CPU/Battery drain
    @SuppressLint("MissingPermission") // Caller must ensure permission
    fun requestUpdates(intervalMillis: Long = 180000L) {
        android.util.Log.d("ServiceStartup", "Manager: requestUpdates called with interval: $intervalMillis")
        
        // Fix: Removed Service invocation for simple updates. 
        // We only need to start the service once. Re-registering updates can be done directly on the client.
        // However, if the intent of this method was to START the service, we should keep it but update logic.
        // Assuming this method is called to CHANGE the interval:
        
        // Fix: Ensure Service is started to show Notification (Foreground)
        // This was missing after refactor, causing "Missing Notification" issue.
        startService()
        
        client.removeActivityUpdates(pendingIntent)
        client.requestActivityUpdates(intervalMillis, pendingIntent)
            .addOnSuccessListener {
                android.util.Log.d("ActivityManager", "Updates registered successfully with interval: $intervalMillis")
            }
            .addOnFailureListener {
                android.util.Log.e("ActivityManager", "Failed to register updates", it)
            }
    }

    // Helper to start the service initially
    fun startService() {
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
    fun listenToUpdates(intervalMillis: Long = 180000L) {
         client.requestActivityUpdates(intervalMillis, pendingIntent)
            .addOnSuccessListener {
                android.util.Log.d("ActivityManager", "Updates registered successfully")
            }
            .addOnFailureListener {
                android.util.Log.e("ActivityManager", "Failed to register updates", it)
            }
    }

    @SuppressLint("MissingPermission")
    private var consecutiveStillCount = 0
    private var currentInterval = 180000L

    @SuppressLint("MissingPermission")
    fun handleActivityUpdate(type: Int, confidence: Int) {
        android.util.Log.d("ActivityManager", "Handling update: type=$type, conf=$confidence")
        
        if (type == com.google.android.gms.location.DetectedActivity.STILL && confidence == 100) {
            consecutiveStillCount++
            android.util.Log.d("ActivityManager", "Consecutive STILL count: $consecutiveStillCount")
            
            if (consecutiveStillCount >= 2 && currentInterval != 600000L) {
                android.util.Log.d("ActivityManager", "Smart Sleep: Enter Deep Sleep (10 mins)")
                currentInterval = 600000L // 10 minutes
                requestUpdates(currentInterval)
            }
        } else if (type != com.google.android.gms.location.DetectedActivity.STILL) {
            consecutiveStillCount = 0
            if (currentInterval != 180000L) {
                android.util.Log.d("ActivityManager", "Smart Sleep: Wake Up -> Active Mode (3 mins)")
                currentInterval = 180000L
                requestUpdates(currentInterval)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopListening() {
        client.removeActivityUpdates(pendingIntent)
    }
}
