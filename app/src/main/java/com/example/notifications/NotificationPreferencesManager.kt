package com.example.notifications

import android.content.Context
import android.content.SharedPreferences
import com.example.notifications.model.NotificationFrequency
import com.example.notifications.model.NotificationPersonality
import com.example.notifications.model.NotificationPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages persistence for Ranki Notification Settings, daily limits,
 * active study/test/video state flags, and 14-day anti-repetition tracking.
 */
class NotificationPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "rankify_notification_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_PERSONALITY = "personality"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_SILENT_START = "silent_start"
        private const val KEY_SILENT_END = "silent_end"
        private const val KEY_MAX_DAILY = "max_daily"

        // Category Keys
        private const val KEY_MORNING_ENABLED = "morning_enabled"
        private const val KEY_AFTER_SCHOOL_ENABLED = "after_school_enabled"
        private const val KEY_LUNCH_BREAK_ENABLED = "lunch_break_enabled"
        private const val KEY_EVENING_ENABLED = "evening_enabled"
        private const val KEY_NIGHT_ENABLED = "night_enabled"
        private const val KEY_WEAK_CHAPTER_ENABLED = "weak_chapter_enabled"
        private const val KEY_REVISION_ENABLED = "revision_enabled"
        private const val KEY_EXAM_ENABLED = "exam_enabled"
        private const val KEY_TEST_ENABLED = "test_enabled"
        private const val KEY_TARGET_ENABLED = "target_enabled"
        private const val KEY_STREAK_ENABLED = "streak_enabled"
        private const val KEY_COMEBACK_ENABLED = "comeback_enabled"
        private const val KEY_ACHIEVEMENT_ENABLED = "achievement_enabled"
        private const val KEY_CONGRATULATIONS_ENABLED = "congratulations_enabled"
        private const val KEY_TIPS_TRICKS_ENABLED = "tips_tricks_enabled"
        private const val KEY_FORMULA_OF_DAY_ENABLED = "formula_of_day_enabled"
        private const val KEY_QUESTION_OF_DAY_ENABLED = "question_of_day_enabled"
        private const val KEY_CHALLENGES_ENABLED = "challenges_enabled"
        private const val KEY_WELLNESS_ENABLED = "wellness_enabled"
        private const val KEY_SOUND_NOTES_ENABLED = "sound_notes_enabled"
        private const val KEY_COMMUNITY_ENABLED = "community_enabled"

        // Tracking Keys
        private const val KEY_LAST_SENT_DATE = "last_sent_date"
        private const val KEY_DAILY_SENT_COUNT = "daily_sent_count"
        private const val KEY_LAST_NOTIFICATION_TIMESTAMP = "last_notification_timestamp"
        private const val KEY_LAST_APP_OPEN_TIMESTAMP = "last_app_open_timestamp"
        private const val KEY_CONSECUTIVE_DISMISSALS = "consecutive_dismissals"
        private const val KEY_TOTAL_SENT = "total_sent_all_time"
        private const val KEY_TOTAL_OPENED = "total_opened_all_time"
        private const val KEY_TOTAL_DISMISSED = "total_dismissed_all_time"

        // Activity Suppression Flags
        private const val KEY_IS_STUDY_SESSION_ACTIVE = "is_study_session_active"
        private const val KEY_IS_TEST_ACTIVE = "is_test_active"
        private const val KEY_IS_VIDEO_PLAYING = "is_video_playing"

        // 14-day anti-repetition tracking JSON
        private const val KEY_SENT_TEMPLATES_JSON = "sent_templates_json"
        private const val COOLDOWN_14_DAYS_MS = 14L * 24 * 60 * 60 * 1000L

        @Volatile
        private var INSTANCE: NotificationPreferencesManager? = null

        fun getInstance(context: Context): NotificationPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getPreferences(): NotificationPreferences {
        val personalityId = prefs.getString(KEY_PERSONALITY, NotificationPersonality.MIXED.id) ?: NotificationPersonality.MIXED.id
        val frequencyId = prefs.getString(KEY_FREQUENCY, NotificationFrequency.NORMAL.id) ?: NotificationFrequency.NORMAL.id
        val frequency = NotificationFrequency.fromId(frequencyId)
        val defaultMaxDaily = frequency.maxDaily

        return NotificationPreferences(
            masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true),
            personality = NotificationPersonality.fromId(personalityId),
            frequency = frequency,
            silentHoursStart = prefs.getString(KEY_SILENT_START, "22:15") ?: "22:15",
            silentHoursEnd = prefs.getString(KEY_SILENT_END, "07:00") ?: "07:00",
            maxDailyNotifications = prefs.getInt(KEY_MAX_DAILY, defaultMaxDaily),

            morningReminderEnabled = prefs.getBoolean(KEY_MORNING_ENABLED, true),
            afterSchoolReminderEnabled = prefs.getBoolean(KEY_AFTER_SCHOOL_ENABLED, true),
            lunchBreakReminderEnabled = prefs.getBoolean(KEY_LUNCH_BREAK_ENABLED, true),
            eveningReminderEnabled = prefs.getBoolean(KEY_EVENING_ENABLED, true),
            nightReminderEnabled = prefs.getBoolean(KEY_NIGHT_ENABLED, true),
            weakChapterReminderEnabled = prefs.getBoolean(KEY_WEAK_CHAPTER_ENABLED, true),
            revisionReminderEnabled = prefs.getBoolean(KEY_REVISION_ENABLED, true),
            examReminderEnabled = prefs.getBoolean(KEY_EXAM_ENABLED, true),
            testReminderEnabled = prefs.getBoolean(KEY_TEST_ENABLED, true),
            targetReminderEnabled = prefs.getBoolean(KEY_TARGET_ENABLED, true),
            streakReminderEnabled = prefs.getBoolean(KEY_STREAK_ENABLED, true),
            comebackReminderEnabled = prefs.getBoolean(KEY_COMEBACK_ENABLED, true),
            achievementReminderEnabled = prefs.getBoolean(KEY_ACHIEVEMENT_ENABLED, true),
            congratulationsEnabled = prefs.getBoolean(KEY_CONGRATULATIONS_ENABLED, true),
            tipsAndTricksEnabled = prefs.getBoolean(KEY_TIPS_TRICKS_ENABLED, true),
            formulaOfDayEnabled = prefs.getBoolean(KEY_FORMULA_OF_DAY_ENABLED, true),
            questionOfDayEnabled = prefs.getBoolean(KEY_QUESTION_OF_DAY_ENABLED, true),
            challengesEnabled = prefs.getBoolean(KEY_CHALLENGES_ENABLED, true),
            wellnessRemindersEnabled = prefs.getBoolean(KEY_WELLNESS_ENABLED, true),
            soundAndNotesEnabled = prefs.getBoolean(KEY_SOUND_NOTES_ENABLED, true),
            communityEnabled = prefs.getBoolean(KEY_COMMUNITY_ENABLED, true)
        )
    }

    fun savePreferences(p: NotificationPreferences) {
        prefs.edit()
            .putBoolean(KEY_MASTER_ENABLED, p.masterEnabled)
            .putString(KEY_PERSONALITY, p.personality.id)
            .putString(KEY_FREQUENCY, p.frequency.id)
            .putString(KEY_SILENT_START, p.silentHoursStart)
            .putString(KEY_SILENT_END, p.silentHoursEnd)
            .putInt(KEY_MAX_DAILY, p.maxDailyNotifications)

            .putBoolean(KEY_MORNING_ENABLED, p.morningReminderEnabled)
            .putBoolean(KEY_AFTER_SCHOOL_ENABLED, p.afterSchoolReminderEnabled)
            .putBoolean(KEY_LUNCH_BREAK_ENABLED, p.lunchBreakReminderEnabled)
            .putBoolean(KEY_EVENING_ENABLED, p.eveningReminderEnabled)
            .putBoolean(KEY_NIGHT_ENABLED, p.nightReminderEnabled)
            .putBoolean(KEY_WEAK_CHAPTER_ENABLED, p.weakChapterReminderEnabled)
            .putBoolean(KEY_REVISION_ENABLED, p.revisionReminderEnabled)
            .putBoolean(KEY_EXAM_ENABLED, p.examReminderEnabled)
            .putBoolean(KEY_TEST_ENABLED, p.testReminderEnabled)
            .putBoolean(KEY_TARGET_ENABLED, p.targetReminderEnabled)
            .putBoolean(KEY_STREAK_ENABLED, p.streakReminderEnabled)
            .putBoolean(KEY_COMEBACK_ENABLED, p.comebackReminderEnabled)
            .putBoolean(KEY_ACHIEVEMENT_ENABLED, p.achievementReminderEnabled)
            .putBoolean(KEY_CONGRATULATIONS_ENABLED, p.congratulationsEnabled)
            .putBoolean(KEY_TIPS_TRICKS_ENABLED, p.tipsAndTricksEnabled)
            .putBoolean(KEY_FORMULA_OF_DAY_ENABLED, p.formulaOfDayEnabled)
            .putBoolean(KEY_QUESTION_OF_DAY_ENABLED, p.questionOfDayEnabled)
            .putBoolean(KEY_CHALLENGES_ENABLED, p.challengesEnabled)
            .putBoolean(KEY_WELLNESS_ENABLED, p.wellnessRemindersEnabled)
            .putBoolean(KEY_SOUND_NOTES_ENABLED, p.soundAndNotesEnabled)
            .putBoolean(KEY_COMMUNITY_ENABLED, p.communityEnabled)
            .apply()
    }

    fun setPersonality(personality: NotificationPersonality) {
        prefs.edit().putString(KEY_PERSONALITY, personality.id).apply()
    }

    fun setFrequency(frequency: NotificationFrequency) {
        prefs.edit()
            .putString(KEY_FREQUENCY, frequency.id)
            .putInt(KEY_MAX_DAILY, frequency.maxDaily)
            .apply()
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
    }

    // --- Active Study Session & Test & Video Protection ---
    fun setStudySessionActive(isActive: Boolean) {
        prefs.edit().putBoolean(KEY_IS_STUDY_SESSION_ACTIVE, isActive).apply()
    }

    fun isStudySessionActive(): Boolean {
        return prefs.getBoolean(KEY_IS_STUDY_SESSION_ACTIVE, false)
    }

    fun setTestActive(isActive: Boolean) {
        prefs.edit().putBoolean(KEY_IS_TEST_ACTIVE, isActive).apply()
    }

    fun isTestActive(): Boolean {
        return prefs.getBoolean(KEY_IS_TEST_ACTIVE, false)
    }

    fun setVideoPlaying(isPlaying: Boolean) {
        prefs.edit().putBoolean(KEY_IS_VIDEO_PLAYING, isPlaying).apply()
    }

    fun isVideoPlaying(): Boolean {
        return prefs.getBoolean(KEY_IS_VIDEO_PLAYING, false)
    }

    fun isStudentInDoNotDisturbMode(): Boolean {
        return isStudySessionActive() || isTestActive() || isVideoPlaying()
    }

    // --- App Open Tracking (For inactivity detection: 2, 5, 10 days) ---
    fun recordAppOpened() {
        prefs.edit()
            .putLong(KEY_LAST_APP_OPEN_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_CONSECUTIVE_DISMISSALS, 0)
            .apply()
    }

    fun getLastAppOpenTimestamp(): Long {
        val stored = prefs.getLong(KEY_LAST_APP_OPEN_TIMESTAMP, 0L)
        return if (stored == 0L) System.currentTimeMillis() else stored
    }

    // --- Daily Notification Limits & Smart Pacing ---
    fun getDailySentCount(): Int {
        val todayStr = getTodayDateKey()
        val lastSentDate = prefs.getString(KEY_LAST_SENT_DATE, "")
        return if (todayStr == lastSentDate) {
            prefs.getInt(KEY_DAILY_SENT_COUNT, 0)
        } else {
            0
        }
    }

    /**
     * Determines if a notification can be sent today, considering:
     * - Configured maximum daily limit.
     * - Adaptive dampening (reduces frequency if user consecutively ignores notifications).
     */
    fun canSendNotificationToday(configuredMaxDaily: Int = 4): Boolean {
        val dismissals = prefs.getInt(KEY_CONSECUTIVE_DISMISSALS, 0)
        // Automatically throttle if student repeatedly ignores notifications (adaptive AI rule)
        val effectiveMax = if (dismissals >= 4) {
            (configuredMaxDaily - 2).coerceAtLeast(1)
        } else if (dismissals >= 2) {
            (configuredMaxDaily - 1).coerceAtLeast(2)
        } else {
            configuredMaxDaily
        }
        return getDailySentCount() < effectiveMax
    }

    fun incrementDailySentCount() {
        val todayStr = getTodayDateKey()
        val lastSentDate = prefs.getString(KEY_LAST_SENT_DATE, "")
        val currentCount = if (todayStr == lastSentDate) {
            prefs.getInt(KEY_DAILY_SENT_COUNT, 0)
        } else {
            0
        }
        val totalSent = prefs.getInt(KEY_TOTAL_SENT, 0) + 1
        prefs.edit()
            .putString(KEY_LAST_SENT_DATE, todayStr)
            .putInt(KEY_DAILY_SENT_COUNT, currentCount + 1)
            .putLong(KEY_LAST_NOTIFICATION_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_TOTAL_SENT, totalSent)
            .apply()
    }

    fun recordNotificationOpened() {
        val opened = prefs.getInt(KEY_TOTAL_OPENED, 0) + 1
        prefs.edit()
            .putInt(KEY_TOTAL_OPENED, opened)
            .putInt(KEY_CONSECUTIVE_DISMISSALS, 0)
            .apply()
    }

    fun recordNotificationDismissed() {
        val dismissed = prefs.getInt(KEY_TOTAL_DISMISSED, 0) + 1
        val consecutive = prefs.getInt(KEY_CONSECUTIVE_DISMISSALS, 0) + 1
        prefs.edit()
            .putInt(KEY_TOTAL_DISMISSED, dismissed)
            .putInt(KEY_CONSECUTIVE_DISMISSALS, consecutive)
            .apply()
    }

    fun getEngagementStatistics(): Triple<Int, Int, Int> {
        val sent = prefs.getInt(KEY_TOTAL_SENT, 0)
        val opened = prefs.getInt(KEY_TOTAL_OPENED, 0)
        val dismissed = prefs.getInt(KEY_TOTAL_DISMISSED, 0)
        return Triple(sent, opened, dismissed)
    }

    fun getLastNotificationTimestamp(): Long {
        return prefs.getLong(KEY_LAST_NOTIFICATION_TIMESTAMP, 0L)
    }

    // --- 14-day Anti-Repetition Tracking (Never repeat text within 14 days) ---
    fun wasTemplateSentInPast14Days(templateKey: String): Boolean {
        cleanupOldTemplates()
        val jsonStr = prefs.getString(KEY_SENT_TEMPLATES_JSON, "{}") ?: "{}"
        return try {
            val json = JSONObject(jsonStr)
            if (!json.has(templateKey)) return false
            val sentTime = json.getLong(templateKey)
            val fourteenDaysAgo = System.currentTimeMillis() - COOLDOWN_14_DAYS_MS
            sentTime >= fourteenDaysAgo
        } catch (e: Exception) {
            false
        }
    }

    // Backwards-compatible alias
    fun wasTemplateSentInPast7Days(templateKey: String): Boolean {
        return wasTemplateSentInPast14Days(templateKey)
    }

    fun recordTemplateSent(templateKey: String) {
        val jsonStr = prefs.getString(KEY_SENT_TEMPLATES_JSON, "{}") ?: "{}"
        try {
            val json = JSONObject(jsonStr)
            json.put(templateKey, System.currentTimeMillis())
            prefs.edit().putString(KEY_SENT_TEMPLATES_JSON, json.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun cleanupOldTemplates() {
        val jsonStr = prefs.getString(KEY_SENT_TEMPLATES_JSON, "{}") ?: "{}"
        try {
            val json = JSONObject(jsonStr)
            val fourteenDaysAgo = System.currentTimeMillis() - COOLDOWN_14_DAYS_MS
            val keysToRemove = mutableListOf<String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (json.getLong(k) < fourteenDaysAgo) {
                    keysToRemove.add(k)
                }
            }
            if (keysToRemove.isNotEmpty()) {
                keysToRemove.forEach { json.remove(it) }
                prefs.edit().putString(KEY_SENT_TEMPLATES_JSON, json.toString()).apply()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // --- Duplicate prevention: track recent notification titles ---
    fun wasExactTitleSentRecently(title: String): Boolean {
        val titlesJson = prefs.getString("recent_sent_titles", "[]") ?: "[]"
        return try {
            val array = org.json.JSONArray(titlesJson)
            for (i in 0 until array.length()) {
                if (array.getString(i).equals(title.trim(), ignoreCase = true)) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun recordNotificationContent(title: String) {
        val titlesJson = prefs.getString("recent_sent_titles", "[]") ?: "[]"
        try {
            val array = org.json.JSONArray(titlesJson)
            val updated = org.json.JSONArray()
            updated.put(title.trim())
            for (i in 0 until array.length().coerceAtMost(30)) {
                val item = array.getString(i)
                if (!item.equals(title.trim(), ignoreCase = true)) {
                    updated.put(item)
                }
            }
            prefs.edit().putString("recent_sent_titles", updated.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun canSendWithCooldown(nowMs: Long = System.currentTimeMillis()): Boolean {
        val lastSent = getLastNotificationTimestamp()
        if (lastSent <= 0L) return true
        return (nowMs - lastSent) >= NotificationAdaptiveFrequencyEngine.MIN_SPACING_INTERVAL_MS
    }

    fun resetNotificationHistory() {
        prefs.edit()
            .remove(KEY_DAILY_SENT_COUNT)
            .remove(KEY_LAST_SENT_DATE)
            .remove(KEY_LAST_NOTIFICATION_TIMESTAMP)
            .remove(KEY_SENT_TEMPLATES_JSON)
            .remove("recent_sent_titles")
            .remove(KEY_CONSECUTIVE_DISMISSALS)
            .apply()
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
