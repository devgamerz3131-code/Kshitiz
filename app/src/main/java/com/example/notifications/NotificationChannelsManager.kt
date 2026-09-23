package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Handles creation and configuration of Android Notification Channels.
 */
object NotificationChannelsManager {

    const val CHANNEL_REMINDERS = "rankify_reminders"
    const val CHANNEL_ACHIEVEMENTS = "rankify_achievements"
    const val CHANNEL_EXAMS = "rankify_exams"
    const val CHANNEL_URGENT = "rankify_urgent"

    fun setupNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Study Reminders & Sprints",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart morning, evening, night reminders, and target updates."
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Milestones & Streaks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Celebrations for streaks, chapter completions, and target badges."
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                CHANNEL_EXAMS,
                "Exam Countdowns & Planning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Target countdown warnings (30, 15, 7, 5, 3, 2, 1 days) and morning alerts."
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                CHANNEL_URGENT,
                "Study Buddy & Inactivity Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weak chapter focus suggestions and inactivity check-ins."
                enableVibration(true)
            }
        )

        notificationManager.createNotificationChannels(channels)
    }
}
