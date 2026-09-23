package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules background periodic WorkManager tasks to evaluate and trigger notifications.
 */
object NotificationScheduler {

    private const val TAG = "NotificationScheduler"
    private const val PERIODIC_WORK_NAME = "RankifySmartNotificationPeriodicWork"
    private const val ONE_TIME_WORK_NAME = "RankifySmartNotificationImmediateWork"

    /**
     * Schedules periodic evaluation (every 15-30 minutes) using WorkManager.
     * Guaranteed battery-efficient and offline-capable.
     */
    fun schedulePeriodicWork(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            // Minimum periodic interval supported by Android WorkManager is 15 minutes
            val periodicRequest = PeriodicWorkRequestBuilder<SmartNotificationWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag("smart_notifications")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Smart notification periodic work successfully enqueued.")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic notification work: ${e.message}", e)
        }
    }

    /**
     * Enqueues an immediate one-time background check.
     */
    fun triggerImmediateCheck(context: Context) {
        try {
            val oneTimeRequest = OneTimeWorkRequestBuilder<SmartNotificationWorker>()
                .addTag("smart_notifications_immediate")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Immediate smart notification check enqueued.")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering immediate check: ${e.message}", e)
        }
    }

    /**
     * Cancels all scheduled notification workers if user disables notifications.
     */
    fun cancelAll(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            Log.d(TAG, "Smart notification periodic work cancelled.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification work: ${e.message}", e)
        }
    }
}
