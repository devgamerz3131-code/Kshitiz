package com.example.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.db.RankifyDatabase
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirebaseFirestoreService
import com.example.data.model.NotificationHistoryEntity
import com.example.notifications.model.SmartNotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles building and displaying system notifications with appropriate PendingIntents,
 * deep links, Room local history, and Firebase Firestore synchronization.
 */
object NotificationDispatcher {

    private const val TAG = "NotificationDispatcher"
    const val EXTRA_DEEP_LINK = "extra_rankify_deep_link"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun showNotification(context: Context, payload: SmartNotificationPayload) {
        // Check POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping notification display.")
                return
            }
        }

        NotificationChannelsManager.setupNotificationChannels(context)

        // Tap PendingIntent -> opens MainActivity directly with Deep Link
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DEEP_LINK, payload.deepLink)
            putExtra(EXTRA_NOTIFICATION_ID, payload.notificationId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            payload.notificationIntId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss PendingIntent -> NotificationActionReceiver
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISSED
            putExtra(EXTRA_NOTIFICATION_ID, payload.notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            payload.notificationIntId + 10000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action button PendingIntent (e.g., "Start Sprint" or "Open")
        val actionIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DEEP_LINK, payload.deepLink)
            putExtra(EXTRA_NOTIFICATION_ID, payload.notificationId)
        }
        val actionPendingIntent = PendingIntent.getActivity(
            context,
            payload.notificationIntId + 20000,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Open & Study", actionPendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(payload.notificationIntId, builder.build())

            // Update preferences tracking
            val prefsManager = NotificationPreferencesManager.getInstance(context)
            prefsManager.incrementDailySentCount()
            val templateKeyToRecord = if (payload.templateKey.isNotBlank()) payload.templateKey else payload.notificationId
            prefsManager.recordTemplateSent(templateKeyToRecord)
            prefsManager.recordNotificationContent(payload.title)

            // In-app debug log
            NotificationDebugLogger.logDispatch(
                title = payload.title,
                category = payload.category.title,
                personality = payload.personality.displayName
            )

            // Save to Room DB & Sync to Firestore
            saveAndSyncHistory(context, payload)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while notifying: ${e.message}")
            NotificationDebugLogger.logError("Permission Denied", "SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification: ${e.message}", e)
            NotificationDebugLogger.logError("Dispatch Failed", e.message ?: "Unknown error")
        }
    }

    /**
     * Cancels all notifications currently posted in the system tray.
     */
    fun cancelAllNotifications(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
            NotificationDebugLogger.logInfo("Cancel All Notifications", "Cleared all active status bar alerts.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel all notifications: ${e.message}")
        }
    }

    /**
     * Returns count of currently active notifications in the status bar (Android M / API 23+).
     */
    fun getActiveNotificationsCount(context: Context): Int {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.activeNotifications?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun saveAndSyncHistory(context: Context, payload: SmartNotificationPayload) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = RankifyDatabase.getDatabase(context)
                val entity = NotificationHistoryEntity(
                    notificationId = payload.notificationId,
                    title = payload.title,
                    body = payload.body,
                    category = payload.category.title,
                    personality = payload.personality.displayName,
                    deepLink = payload.deepLink,
                    createdTime = System.currentTimeMillis(),
                    opened = false,
                    dismissed = false,
                    isSynced = false
                )
                db.rankifyDao().insertNotificationHistory(entity)

                // Sync to Firebase Firestore: users/{uid}/notification_history/{notificationId}
                val uid = FirebaseAuthService.currentUser?.uid
                if (!uid.isNullOrBlank()) {
                    FirebaseNotificationSyncService.syncNotificationToFirestore(uid, entity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification history: ${e.message}", e)
            }
        }
    }
}
