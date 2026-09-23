package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background WorkManager worker that periodically checks study progress, targets,
 * and triggers smart notifications without draining battery or requiring cloud functions.
 */
class SmartNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SmartNotificationWorker", "Running Smart Notification periodic evaluation...")
        return try {
            val sent = SmartNotificationEngine.evaluateAndTrigger(applicationContext)
            Log.d("SmartNotificationWorker", "Periodic evaluation finished. Notification sent: $sent")
            Result.success()
        } catch (e: Exception) {
            Log.e("SmartNotificationWorker", "Failed smart notification evaluation: ${e.message}", e)
            Result.retry()
        }
    }
}
