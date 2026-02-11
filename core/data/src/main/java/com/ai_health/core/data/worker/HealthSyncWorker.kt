package com.ai_health.core.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ai_health.core.domain.repository.HealthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthRepository: HealthRepository
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "HealthSyncWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting health sync worker")
        return try {
            // syncHealthData handles Cold Start vs Incremental Sync internally
            healthRepository.syncHealthData()
            Log.d(TAG, "Health sync worker finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Health sync worker failed", e)
            if (runAttemptCount < 3) {
                 Result.retry()
            } else {
                 Result.failure()
            }
        }
    }
}
