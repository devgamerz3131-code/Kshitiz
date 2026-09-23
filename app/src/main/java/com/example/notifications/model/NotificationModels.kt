package com.example.notifications.model

/**
 * 5 Supported notification personalities for Rankify's AI mascot "Ranki".
 */
enum class NotificationPersonality(
    val id: String,
    val displayName: String,
    val emoji: String,
    val subtitle: String
) {
    FUNNY("FUNNY", "Funny", "😂", "Playful, light-hearted jokes & relatable student life"),
    SAVAGE("SAVAGE", "Savage", "😈", "Friendly reality-check roasts. Never toxic, strictly motivating"),
    MOTIVATION("MOTIVATION", "Motivational", "💪", "Direct, powerful, and inspiring reminders to conquer dreams"),
    AI_MENTOR("AI_MENTOR", "AI Mentor", "🧠", "Smart syllabus insights, memory hacks & exam strategies"),
    MIXED("MIXED", "Mixed", "🎲", "Intelligently rotates all personalities based on time & context");

    companion object {
        fun fromId(id: String): NotificationPersonality {
            return entries.find {
                it.id.equals(id, ignoreCase = true) ||
                it.displayName.equals(id, ignoreCase = true) ||
                it.name.equals(id, ignoreCase = true)
            } ?: MIXED
        }
    }
}

/**
 * Frequency level of daily smart notifications.
 */
enum class NotificationFrequency(
    val id: String,
    val displayName: String,
    val description: String,
    val maxDaily: Int
) {
    MINIMAL("MINIMAL", "Minimal", "1–2 daily • Essential alerts only", 2),
    NORMAL("NORMAL", "Normal", "3–4 daily • Balanced study buddy pace", 4),
    ACTIVE("ACTIVE", "Active", "5–6 daily • Dynamic day-long engagement", 6),
    HARDCORE("HARDCORE", "Hardcore", "7–8 daily • High-intensity exam mode", 8);

    companion object {
        fun fromId(id: String): NotificationFrequency {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: NORMAL
        }
    }
}

/**
 * Exhaustive categories of notifications produced by Ranki.
 */
enum class NotificationCategory(
    val id: String,
    val title: String,
    val channelId: String,
    val defaultEnabled: Boolean = true
) {
    MORNING("MORNING", "Good Morning", "rankify_reminders"),
    AFTER_SCHOOL("AFTER_SCHOOL", "After School", "rankify_reminders"),
    LUNCH_BREAK("LUNCH_BREAK", "Lunch Break", "rankify_reminders"),
    EVENING("EVENING", "Evening Study", "rankify_reminders"),
    NIGHT("NIGHT", "Night Study", "rankify_reminders"),
    REVISION("REVISION", "Revision Reminder", "rankify_reminders"),
    WEAK_CHAPTER("WEAK_CHAPTER", "Weak Chapter Reminder", "rankify_urgent"),
    EXAM_COUNTDOWN("EXAM_COUNTDOWN", "Exam Countdown", "rankify_exams"),
    TEST_REMINDER("TEST_REMINDER", "Test Reminder", "rankify_exams"),
    TARGET("TARGET", "Daily Target Reminder", "rankify_reminders"),
    STREAK("STREAK", "Streak Reminder", "rankify_achievements"),
    COMEBACK("COMEBACK", "Comeback Reminder", "rankify_urgent"),
    ACHIEVEMENT("ACHIEVEMENT", "Milestone Achievement", "rankify_achievements"),
    CONGRATULATIONS("CONGRATULATIONS", "Congratulations", "rankify_achievements"),
    MOTIVATION("MOTIVATION", "Pure Motivation", "rankify_reminders"),
    FUNNY_ROAST("FUNNY_ROAST", "Ranki Roast", "rankify_reminders"),
    AI_TIPS("AI_TIPS", "AI Study Tips", "rankify_reminders"),
    QUICK_FACTS("QUICK_FACTS", "Quick Science Fact", "rankify_reminders"),
    FORMULA_OF_DAY("FORMULA_OF_DAY", "Formula of the Day", "rankify_reminders"),
    QUESTION_OF_DAY("QUESTION_OF_DAY", "Question of the Day", "rankify_reminders"),
    MEMORY_TRICK("MEMORY_TRICK", "Memory Trick", "rankify_reminders"),
    TODAYS_CHALLENGE("TODAYS_CHALLENGE", "Today's Challenge", "rankify_reminders"),
    WEEKEND_CHALLENGE("WEEKEND_CHALLENGE", "Weekend Challenge", "rankify_reminders"),
    SUNDAY_PLANNING("SUNDAY_PLANNING", "Sunday Planning", "rankify_reminders"),
    PROGRESS_REMINDER("PROGRESS_REMINDER", "Syllabus Progress", "rankify_reminders"),
    UPCOMING_EXAM_ALERT("UPCOMING_EXAM_ALERT", "Upcoming Exam Alert", "rankify_exams"),
    WATER_REMINDER("WATER_REMINDER", "Hydration Reminder", "rankify_reminders"),
    SLEEP_REMINDER("SLEEP_REMINDER", "Sleep & Rest Reminder", "rankify_reminders"),
    BREAK_REMINDER("BREAK_REMINDER", "Study Break Reminder", "rankify_reminders"),
    POMODORO_REMINDER("POMODORO_REMINDER", "Pomodoro Sprint", "rankify_reminders"),
    FOCUS_REMINDER("FOCUS_REMINDER", "Deep Focus Alert", "rankify_reminders"),
    NEW_NOTES("NEW_NOTES", "Study Notes Alert", "rankify_reminders"),
    NEW_SONGS("NEW_SONGS", "Binaural Beats Unlock", "rankify_reminders"),
    COMMUNITY_ACTIVITY("COMMUNITY_ACTIVITY", "Community & Leaderboard", "rankify_reminders"),
    AI_MENTOR("AI_MENTOR", "AI Mentor Guidance", "rankify_reminders"),
    TARGET_COMPLETED("TARGET_COMPLETED", "Target Conquered", "rankify_achievements"),
    START_STUDYING("START_STUDYING", "Start Studying Reminder", "rankify_reminders"),
    SCHOOL_LEAVING("SCHOOL_LEAVING", "School Leaving Reminder", "rankify_reminders"),
    SESSION_APPRECIATION("SESSION_APPRECIATION", "Study Session Appreciation", "rankify_achievements"),
    RANKIFY_EXCLUSIVE("RANKIFY_EXCLUSIVE", "Rankify Exclusive", "rankify_reminders"),
    TARGET_PROGRESS("TARGET_PROGRESS", "Target Progress Check-in", "rankify_reminders");

    companion object {
        fun fromId(id: String): NotificationCategory {
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: EVENING
        }
    }
}

/**
 * Deep link destination keys for notification taps.
 */
object NotificationDeepLinks {
    const val WEAK_CHAPTER = "weak_chapter"
    const val TODAY_TARGET = "today_target"
    const val REVISION = "revision"
    const val EXAM_PLANNER = "exam_planner"
    const val SYLLABUS_TRACKER = "syllabus_tracker"
    const val AI_TUTOR = "ai_tutor"
    const val STUDY_SESSION = "study_session"
    const val PRACTICE = "practice"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val STUDY_DOCK = "study_dock"
    const val FLASHCARDS = "flashcards"
    const val FORMULAS = "formulas"
    const val LEADERBOARD = "leaderboard"
    const val STREAK = "streak"
    const val EXCLUSIVE = "exclusive"
    const val MUSIC_VAULT = "music_vault"
    const val COMMUNITY = "community"
    const val NOTIFICATION_TESTING = "notification_testing"
}

/**
 * Comprehensive user-configurable preferences for Ranki notifications.
 */
data class NotificationPreferences(
    val masterEnabled: Boolean = true,
    val personality: NotificationPersonality = NotificationPersonality.MIXED,
    val frequency: NotificationFrequency = NotificationFrequency.NORMAL,
    val silentHoursStart: String = "22:15", // 10:15 PM
    val silentHoursEnd: String = "07:00",   // 7:00 AM
    val maxDailyNotifications: Int = 4,

    // Granular Category Toggles
    val morningReminderEnabled: Boolean = true,
    val afterSchoolReminderEnabled: Boolean = true,
    val lunchBreakReminderEnabled: Boolean = true,
    val eveningReminderEnabled: Boolean = true,
    val nightReminderEnabled: Boolean = true,
    val weakChapterReminderEnabled: Boolean = true,
    val revisionReminderEnabled: Boolean = true,
    val examReminderEnabled: Boolean = true,
    val testReminderEnabled: Boolean = true,
    val targetReminderEnabled: Boolean = true,
    val streakReminderEnabled: Boolean = true,
    val comebackReminderEnabled: Boolean = true,
    val inactivityReminderEnabled: Boolean = true,
    val achievementReminderEnabled: Boolean = true,
    val congratulationsEnabled: Boolean = true,
    val tipsAndTricksEnabled: Boolean = true,
    val formulaOfDayEnabled: Boolean = true,
    val questionOfDayEnabled: Boolean = true,
    val challengesEnabled: Boolean = true,
    val wellnessRemindersEnabled: Boolean = true,
    val soundAndNotesEnabled: Boolean = true,
    val communityEnabled: Boolean = true
)

/**
 * Notification payload ready for dispatch, stamped with mascot Ranki identity.
 */
data class SmartNotificationPayload(
    val notificationId: String,
    val title: String,
    val body: String,
    val category: NotificationCategory,
    val personality: NotificationPersonality,
    val deepLink: String,
    val channelId: String = category.channelId,
    val notificationIntId: Int = (notificationId.hashCode() and 0x7FFFFFFF),
    val mascotName: String = "Ranki",
    val templateKey: String = ""
)

/**
 * Intelligence Score calculated dynamically from user progress.
 */
data class NotificationIntelligenceScore(
    val totalScore: Int, // 0 - 100
    val consistencyScore: Int, // 0 - 25
    val studyTimeScore: Int, // 0 - 25
    val targetDisciplineScore: Int, // 0 - 25
    val examProximityScore: Int, // 0 - 25
    val recommendedTiming: String,
    val recommendedTone: String,
    val description: String,
    val rankiFeedback: String = "Ranki is keeping an eye on your progress!"
)

/**
 * Engagement statistics for smart notifications.
 */
data class NotificationStatistics(
    val totalSent: Int = 0,
    val totalOpened: Int = 0,
    val totalDismissed: Int = 0,
    val openRatePercent: Float = 0f,
    val preferredMode: String = "MIXED",
    val activeStreak: Int = 0,
    val lastNotificationTime: Long = 0L,
    val lastOpenedTime: Long = 0L
)

/**
 * Schedule status snapshot for real-time monitoring and sync.
 */
data class NotificationScheduleStatus(
    val nextScheduledCheck: Long = System.currentTimeMillis() + (15 * 60 * 1000L),
    val dailySentToday: Int = 0,
    val dailyQuota: Int = 4,
    val isQuietHours: Boolean = false,
    val isInStudySession: Boolean = false,
    val isTestActive: Boolean = false,
    val isVideoPlaying: Boolean = false
)
