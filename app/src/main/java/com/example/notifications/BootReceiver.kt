package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Reschedules WorkManager periodic notification analysis when device boots or app package is updated.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("RankifyBootReceiver", "Boot or package replaced received. Scheduling smart notification worker.")
            NotificationScheduler.schedulePeriodicWork(context)
        }
    }
}
