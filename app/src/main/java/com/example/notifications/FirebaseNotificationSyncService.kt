package com.example.notifications

import android.util.Log
import com.example.RankifyApplication
import com.example.data.model.NotificationHistoryEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Syncs notification history records with Firebase Firestore under:
 * `users/{uid}/notification_history/{notificationId}`.
 * Works seamlessly on the Firebase Spark (free) tier with client-side writes.
 */
object FirebaseNotificationSyncService {

    private const val TAG = "FirebaseNotificationSync"

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            RankifyApplication.appContext?.let { RankifyApplication.ensureFirebase(it) }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseFirestore instance: ${e.message}")
            null
        }
    }

    suspend fun syncNotificationToFirestore(uid: String, entity: NotificationHistoryEntity) {
        val firestore = getFirestore() ?: return
        try {
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("notification_history")
                .document(entity.notificationId)

            val data = hashMapOf<String, Any>(
                "notificationId" to entity.notificationId,
                "title" to entity.title,
                "body" to entity.body,
                "category" to entity.category,
                "personality" to entity.personality,
                "deepLink" to entity.deepLink,
                "createdTime" to Timestamp(Date(entity.createdTime)),
                "opened" to entity.opened,
                "dismissed" to entity.dismissed,
                "updatedAt" to Timestamp.now()
            )

            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "Notification ${entity.notificationId} successfully synced to Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync notification to Firestore: ${e.message}")
        }
    }

    suspend fun markNotificationOpenedInFirestore(uid: String, notificationId: String) {
        val firestore = getFirestore() ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("notification_history")
                .document(notificationId)
                .update(
                    mapOf(
                        "opened" to true,
                        "openedAt" to Timestamp.now()
                    )
                ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification opened status in Firestore: ${e.message}")
        }
    }

    suspend fun markNotificationDismissedInFirestore(uid: String, notificationId: String) {
        val firestore = getFirestore() ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("notification_history")
                .document(notificationId)
                .update(
                    mapOf(
                        "dismissed" to true,
                        "dismissedAt" to Timestamp.now()
                    )
                ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification dismissed status in Firestore: ${e.message}")
        }
    }

    suspend fun syncPreferencesToFirestore(uid: String, prefs: com.example.notifications.model.NotificationPreferences) {
        val firestore = getFirestore() ?: return
        try {
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("notification_preferences")
                .document("settings")

            val data = hashMapOf<String, Any>(
                "masterEnabled" to prefs.masterEnabled,
                "personality" to prefs.personality.id,
                "morningReminderEnabled" to prefs.morningReminderEnabled,
                "afterSchoolReminderEnabled" to prefs.afterSchoolReminderEnabled,
                "eveningReminderEnabled" to prefs.eveningReminderEnabled,
                "nightReminderEnabled" to prefs.nightReminderEnabled,
                "weakChapterReminderEnabled" to prefs.weakChapterReminderEnabled,
                "revisionReminderEnabled" to prefs.revisionReminderEnabled,
                "examReminderEnabled" to prefs.examReminderEnabled,
                "achievementReminderEnabled" to prefs.achievementReminderEnabled,
                "streakReminderEnabled" to prefs.streakReminderEnabled,
                "inactivityReminderEnabled" to prefs.inactivityReminderEnabled,
                "silentHoursStart" to prefs.silentHoursStart,
                "silentHoursEnd" to prefs.silentHoursEnd,
                "maxDailyNotifications" to prefs.maxDailyNotifications,
                "updatedAt" to Timestamp.now()
            )

            docRef.set(data, SetOptions.merge()).await()
            Log.d(TAG, "Notification preferences synced to Firestore for user $uid")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync notification preferences to Firestore: ${e.message}")
        }
    }

    suspend fun fetchPreferencesFromFirestore(uid: String): com.example.notifications.model.NotificationPreferences? {
        val firestore = getFirestore() ?: return null
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .collection("notification_preferences")
                .document("settings")
                .get()
                .await()

            if (doc.exists()) {
                val personalityId = doc.getString("personality") ?: "mixed"
                com.example.notifications.model.NotificationPreferences(
                    masterEnabled = doc.getBoolean("masterEnabled") ?: true,
                    personality = com.example.notifications.model.NotificationPersonality.fromId(personalityId),
                    morningReminderEnabled = doc.getBoolean("morningReminderEnabled") ?: true,
                    afterSchoolReminderEnabled = doc.getBoolean("afterSchoolReminderEnabled") ?: true,
                    eveningReminderEnabled = doc.getBoolean("eveningReminderEnabled") ?: true,
                    nightReminderEnabled = doc.getBoolean("nightReminderEnabled") ?: true,
                    weakChapterReminderEnabled = doc.getBoolean("weakChapterReminderEnabled") ?: true,
                    revisionReminderEnabled = doc.getBoolean("revisionReminderEnabled") ?: true,
                    examReminderEnabled = doc.getBoolean("examReminderEnabled") ?: true,
                    achievementReminderEnabled = doc.getBoolean("achievementReminderEnabled") ?: true,
                    streakReminderEnabled = doc.getBoolean("streakReminderEnabled") ?: true,
                    inactivityReminderEnabled = doc.getBoolean("inactivityReminderEnabled") ?: true,
                    silentHoursStart = doc.getString("silentHoursStart") ?: "22:15",
                    silentHoursEnd = doc.getString("silentHoursEnd") ?: "07:00",
                    maxDailyNotifications = doc.getLong("maxDailyNotifications")?.toInt() ?: 3
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch notification preferences from Firestore: ${e.message}")
            null
        }
    }

    suspend fun fetchHistoryFromFirestore(uid: String, limit: Long = 30): List<NotificationHistoryEntity> {
        val firestore = getFirestore() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("notification_history")
                .orderBy("createdTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val notifId = doc.getString("notificationId") ?: doc.id
                val title = doc.getString("title") ?: return@mapNotNull null
                val body = doc.getString("body") ?: ""
                val category = doc.getString("category") ?: "EVENING"
                val personality = doc.getString("personality") ?: "mixed"
                val deepLink = doc.getString("deepLink") ?: "rankify://home"
                val createdTime = doc.getTimestamp("createdTime")?.toDate()?.time ?: System.currentTimeMillis()
                val opened = doc.getBoolean("opened") ?: false
                val dismissed = doc.getBoolean("dismissed") ?: false

                NotificationHistoryEntity(
                    notificationId = notifId,
                    title = title,
                    body = body,
                    category = category,
                    personality = personality,
                    deepLink = deepLink,
                    createdTime = createdTime,
                    opened = opened,
                    dismissed = dismissed
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch notification history from Firestore: ${e.message}")
            emptyList()
        }
    }
}
