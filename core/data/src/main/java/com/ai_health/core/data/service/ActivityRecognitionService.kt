package com.ai_health.core.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ai_health.core.data.manager.ActivityRecognitionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActivityRecognitionService : Service() {

    @Inject
    lateinit var activityRecognitionManager: ActivityRecognitionManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("ServiceStartup", "Service: onStartCommand called")
        try {
            startForegroundService()
            // Delegate the actual registration to the manager
            activityRecognitionManager.listenToUpdates()
        } catch (e: Exception) {
            android.util.Log.e("ServiceStartup", "Service crash", e)
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
         android.util.Log.d("ServiceStartup", "Service: onCreate")
    }

    override fun onDestroy() {
        android.util.Log.d("ServiceStartup", "Service: onDestroy called - cleaning up")
        
        // Operation Scalpel: Ensure we stop foreground state and remove notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        // Ensure updates are removed (Leak Plug)
        activityRecognitionManager.stopListening()
        
        super.onDestroy()
    }

    private fun startForegroundService() {
        val channelId = "activity_recognition_channel"
        val channelName = "Activity Recognition"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AI Health Assistant")
            .setContentText("Monitoring your activity to optimize your life!")
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        startForeground(1001, notification)
    }
}
