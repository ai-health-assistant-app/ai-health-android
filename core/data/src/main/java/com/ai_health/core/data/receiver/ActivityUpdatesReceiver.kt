package com.ai_health.core.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ai_health.core.domain.model.UserActivityType
import com.ai_health.core.domain.repository.ActivityLogRepository
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class ActivityUpdatesReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ActivityLogRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            android.util.Log.d("ActivityUpdatesReceiver", "Received intent with result: $result")
            result?.let {
                val mostProbableActivity = it.mostProbableActivity
                val activityType = mapActivityType(mostProbableActivity.type)
                val confidence = mostProbableActivity.confidence
                
                android.util.Log.d("ActivityUpdatesReceiver", "Detected activity: $activityType with confidence: $confidence")

                // Use goAsync() to keep the receiver alive while performing invalidation/DB write
                val pendingResult = goAsync()
                
                // Launch in a scope (Global or custom) but ensure we finish pendingResult
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        android.util.Log.d("ActivityUpdatesReceiver", "Saving activity to repository...")
                        repository.logActivity(Instant.ofEpochMilli(it.time), activityType, confidence)
                        android.util.Log.d("ActivityUpdatesReceiver", "Activity saved successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("ActivityUpdatesReceiver", "Error saving activity", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else {
             android.util.Log.d("ActivityUpdatesReceiver", "Received intent but no ActivityRecognitionResult")
        }
    }
    
    private fun mapActivityType(type: Int): UserActivityType {
        return when(type) {
             DetectedActivity.IN_VEHICLE -> UserActivityType.IN_VEHICLE
             DetectedActivity.ON_BICYCLE -> UserActivityType.ON_BICYCLE
             DetectedActivity.RUNNING -> UserActivityType.RUNNING
             DetectedActivity.STILL -> UserActivityType.STILL
             DetectedActivity.WALKING -> UserActivityType.WALKING
             DetectedActivity.TILTING -> UserActivityType.TILTING
             else -> UserActivityType.UNKNOWN
        }
    }
}
