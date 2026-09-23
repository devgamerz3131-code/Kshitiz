package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.RankifyDatabase
import com.example.data.firebase.FirebaseAuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles notification actions such as dismissal and interaction reporting.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotifActionReceiver"
        const val ACTION_DISMISSED = "com.example.rankify.NOTIFICATION_DISMISSED"
        const val ACTION_OPENED = "com.example.rankify.NOTIFICATION_OPENED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra(NotificationDispatcher.EXTRA_NOTIFICATION_ID) ?: return
        val action = intent.action

        Log.d(TAG, "Notification action received: $action for ID: $notificationId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DISMISSED -> SmartNotificationEngine.recordNotificationInteraction(context, notificationId, isOpened = false)
                    ACTION_OPENED -> SmartNotificationEngine.recordNotificationInteraction(context, notificationId, isOpened = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification action: ${e.message}", e)
            }
        }
    }
}
