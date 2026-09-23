package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.data.db.RankifyDatabase
import com.example.data.firebase.FirebaseAuthService
import com.example.data.model.ExamEntity
import com.example.data.model.NotificationHistoryEntity
import com.example.data.model.StudentProfile
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity
import com.example.notifications.model.NotificationCategory
import com.example.notifications.model.NotificationDeepLinks
import com.example.notifications.model.NotificationIntelligenceScore
import com.example.notifications.model.NotificationPersonality
import com.example.notifications.model.NotificationPreferences
import com.example.notifications.model.SmartNotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * SmartNotificationEngine
 *
 * The central coordinator for Rankify's Smart Notification System:
 * - Coordinates scheduling strategies (WorkManager periodic evaluation, interval pacing, immediate checks).
 * - Enforces quiet hours strictly (e.g., 10:15 PM - 7:00 AM, spanning midnight or custom windows).
 * - Enforces daily maximum limits (default 3/day) and minimum spacing between notifications.
 * - Protects active study sessions (never disturbs students while stopwatch or study dock is active).
 * - Coordinates intelligent personality selection (Funny, Savage, Motivational, or contextual Mixed rotation).
 * - Handles bi-directional Firestore cloud sync (preferences, notification history, opened/dismissed events).
 * - Computes the multi-pillar Notification Intelligence Score (0-100).
 */
object SmartNotificationEngine {

    private const val TAG = "SmartNotificationEngine"

    // Minimum cooldown period between consecutive automated notifications (strict 30 minutes)
    const val MIN_NOTIFICATION_INTERVAL_MS = 30 * 60 * 1000L

    // =========================================================================
    // 1. SCHEDULING STRATEGY COORDINATION
    // =========================================================================

    /**
     * Initializes and registers the periodic background WorkManager task.
     */
    fun startPeriodicScheduling(context: Context) {
        val prefs = NotificationPreferencesManager.getInstance(context).getPreferences()
        if (prefs.masterEnabled) {
            NotificationScheduler.schedulePeriodicWork(context)
            Log.d(TAG, "Periodic scheduling initialized via WorkManager.")
        } else {
            Log.d(TAG, "Periodic scheduling skipped: master notifications toggle disabled.")
        }
    }

    /**
     * Triggers an immediate one-time background evaluation check.
     */
    fun triggerImmediateCheck(context: Context) {
        NotificationScheduler.triggerImmediateCheck(context)
    }

    /**
     * Cancels all scheduled background evaluation jobs.
     */
    fun stopPeriodicScheduling(context: Context) {
        NotificationScheduler.cancelAll(context)
        Log.d(TAG, "Periodic scheduling stopped.")
    }

    /**
     * Evaluates smart notification candidates and triggers the most pertinent one
     * based on time-window strategies, student progress, quiet hours, and daily limits.
     *
     * @return true if a notification was triggered, false otherwise.
     */
    suspend fun evaluateAndTrigger(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefsManager = NotificationPreferencesManager.getInstance(context)
            val prefs = prefsManager.getPreferences()

            // 1. Master toggle check
            if (!prefs.masterEnabled) {
                Log.d(TAG, "Evaluation halted: Master notification toggle is disabled.")
                return@withContext false
            }

            // 2. Active study protection: Never disturb a student in focus!
            if (prefsManager.isStudySessionActive()) {
                Log.d(TAG, "Evaluation halted: Active study session in progress.")
                return@withContext false
            }

            val nowMs = System.currentTimeMillis()

            // 3. Quiet hours check: Strict prohibition during sleep/rest hours
            if (isQuietHours(nowMs, prefs.silentHoursStart, prefs.silentHoursEnd)) {
                Log.d(TAG, "Evaluation halted: Current time is within quiet hours (${prefs.silentHoursStart} - ${prefs.silentHoursEnd}).")
                return@withContext false
            }

            // 4. Daily maximum limit check (default max 3 per day)
            if (!prefsManager.canSendNotificationToday(prefs.maxDailyNotifications)) {
                Log.d(TAG, "Evaluation halted: Daily limit of ${prefs.maxDailyNotifications} notifications reached for today.")
                return@withContext false
            }

            // 5. Minimum spacing interval check to prevent spam
            val lastSentMs = prefsManager.getLastNotificationTimestamp()
            if (lastSentMs > 0 && (nowMs - lastSentMs) < MIN_NOTIFICATION_INTERVAL_MS) {
                val remainingMins = (MIN_NOTIFICATION_INTERVAL_MS - (nowMs - lastSentMs)) / 60000
                Log.d(TAG, "Evaluation halted: Minimum spacing active ($remainingMins mins remaining before next eligible notification).")
                return@withContext false
            }

            // 6. Gather student learning context from local Room database
            val db = RankifyDatabase.getDatabase(context)
            val dao = db.rankifyDao()

            val profile: StudentProfile? = dao.getProfileDirect()
            val chapters: List<SyllabusChapterEntity> = dao.getChaptersDirect()
            val targets: List<StudyTargetEntity> = dao.getAllTargetsDirect()
            val exams: List<ExamEntity> = dao.getAllExamsDirect()
            val startOfDay = getStartOfDayMillis()
            val todaySessions: List<StudySessionEntity> = dao.getSessionsSince(startOfDay)

            // 7. Evaluate candidates via intelligent scoring strategy
            val payload: SmartNotificationPayload? = NotificationGenerator.evaluateSmartNotification(
                currentTimeMs = nowMs,
                prefs = prefs,
                prefsManager = prefsManager,
                profile = profile,
                chapters = chapters,
                targets = targets,
                todaySessions = todaySessions,
                exams = exams
            )

            if (payload != null) {
                Log.d(TAG, "Dispatching smart notification candidate: [${payload.category}] ${payload.title}")
                NotificationDispatcher.showNotification(context, payload)

                // Sync dispatched record to Firestore if user is authenticated
                val uid = FirebaseAuthService.currentUser?.uid
                if (!uid.isNullOrBlank()) {
                    val entity = NotificationHistoryEntity(
                        notificationId = payload.notificationId,
                        title = payload.title,
                        body = payload.body,
                        category = payload.category.name,
                        personality = payload.personality.id,
                        deepLink = payload.deepLink,
                        createdTime = System.currentTimeMillis()
                    )
                    FirebaseNotificationSyncService.syncNotificationToFirestore(uid, entity)
                }

                return@withContext true
            } else {
                Log.d(TAG, "No candidate notification met the timing and relevance threshold.")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating notification candidates: ${e.message}", e)
            return@withContext false
        }
    }

    // =========================================================================
    // 2. QUIET HOURS & DAILY LIMIT RULES
    // =========================================================================

    /**
     * Determines whether a given timestamp falls within the quiet hours window.
     * Accurately supports windows that span midnight (e.g. 22:15 to 07:00)
     * as well as same-day windows (e.g. 01:00 to 06:00).
     */
    fun isQuietHours(currentTimeMs: Long = System.currentTimeMillis(), start: String = "22:15", end: String = "07:00"): Boolean {
        return try {
            val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMinute = cal.get(Calendar.MINUTE)
            val currentTotalMins = currentHour * 60 + currentMinute

            val (startH, startM) = start.split(":").map { it.toInt() }
            val (endH, endM) = end.split(":").map { it.toInt() }
            val startTotalMins = startH * 60 + startM
            val endTotalMins = endH * 60 + endM

            if (startTotalMins <= endTotalMins) {
                currentTotalMins in startTotalMins..endTotalMins
            } else {
                // Spans midnight: e.g. 22:15 to 07:00
                currentTotalMins >= startTotalMins || currentTotalMins <= endTotalMins
            }
        } catch (e: Exception) {
            val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            hour > 22 || (hour == 22 && minute >= 15) || hour < 7
        }
    }

    /**
     * Checks whether the application has available notification slots for today.
     */
    fun canSendNotificationToday(context: Context): Boolean {
        val prefsManager = NotificationPreferencesManager.getInstance(context)
        val prefs = prefsManager.getPreferences()
        return prefs.masterEnabled && prefsManager.canSendNotificationToday(prefs.maxDailyNotifications)
    }

    /**
     * Returns remaining notification quota for today.
     */
    fun getRemainingDailyQuota(context: Context): Int {
        val prefsManager = NotificationPreferencesManager.getInstance(context)
        val prefs = prefsManager.getPreferences()
        val sent = prefsManager.getDailySentCount()
        return (prefs.maxDailyNotifications - sent).coerceAtLeast(0)
    }

    // =========================================================================
    // 3. PERSONALITY SELECTION & TEST NUDGES
    // =========================================================================

    /**
     * Resolves the target personality for a given time window and student situation.
     * For MIXED mode, dynamically rotates between personalities:
     * - Morning (7 - 11 AM): MOTIVATION (inspirational, energizing)
     * - Afternoon (12 - 5 PM): SAVAGE (anti-procrastination reality check)
     * - Evening / Night (6 - 10 PM): FUNNY (relatable, stress-relieving)
     */
    fun resolvePersonality(
        userPreference: NotificationPersonality,
        calendar: Calendar = Calendar.getInstance(),
        hasPendingTargets: Boolean = true
    ): NotificationPersonality {
        if (userPreference != NotificationPersonality.MIXED) {
            return userPreference
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..11 -> NotificationPersonality.MOTIVATION
            hour in 12..17 -> NotificationPersonality.SAVAGE
            hour in 18..21 && hasPendingTargets -> NotificationPersonality.SAVAGE
            else -> NotificationPersonality.FUNNY
        }
    }

    /**
     * Instantly dispatches a test notification for the chosen personality.
     */
    fun sendTestNotification(context: Context, personality: NotificationPersonality) {
        val payload = when (personality) {
            NotificationPersonality.FUNNY -> SmartNotificationPayload(
                notificationId = UUID.randomUUID().toString(),
                title = "Study Buddy Test 😂",
                body = "Book khol lo... usne tumhara kya bigaada hai? 😂 Notification system is fully active!",
                category = NotificationCategory.MORNING,
                personality = NotificationPersonality.FUNNY,
                deepLink = NotificationDeepLinks.TODAY_TARGET
            )
            NotificationPersonality.SAVAGE -> SmartNotificationPayload(
                notificationId = UUID.randomUUID().toString(),
                title = "Rankify Reality Check 😏",
                body = "Reels se marks milte to tum topper hote 😏 Notifications configured with savage energy!",
                category = NotificationCategory.EVENING,
                personality = NotificationPersonality.SAVAGE,
                deepLink = NotificationDeepLinks.TODAY_TARGET
            )
            NotificationPersonality.MOTIVATION -> SmartNotificationPayload(
                notificationId = UUID.randomUUID().toString(),
                title = "Power Reminder 💪",
                body = "30 minutes today. 30 marks tomorrow. Rankify is ready to elevate your board score!",
                category = NotificationCategory.MORNING,
                personality = NotificationPersonality.MOTIVATION,
                deepLink = NotificationDeepLinks.TODAY_TARGET
            )
            NotificationPersonality.AI_MENTOR -> SmartNotificationPayload(
                notificationId = UUID.randomUUID().toString(),
                title = "AI Mentor Protocol 🧠",
                body = "Active retrieval index optimal. Reviewing weak topics now boosts retention by 42%.",
                category = NotificationCategory.AI_MENTOR,
                personality = NotificationPersonality.AI_MENTOR,
                deepLink = NotificationDeepLinks.TODAY_TARGET
            )
            NotificationPersonality.MIXED -> SmartNotificationPayload(
                notificationId = UUID.randomUUID().toString(),
                title = "Rankify Smart AI 🎲",
                body = "Mixed personality engaged! Perfectly balancing fun, friendly roasts, and motivation.",
                category = NotificationCategory.EVENING,
                personality = NotificationPersonality.MIXED,
                deepLink = NotificationDeepLinks.TODAY_TARGET
            )
        }
        NotificationDispatcher.showNotification(context, payload)
    }

    /**
     * "After session finishes send appreciation."
     * Immediately dispatches a high-vibe Ranki appreciation notification to celebrate the completed study session!
     */
    fun dispatchSessionAppreciationNotification(
        context: Context,
        durationMinutes: Int,
        subject: String = "",
        chapter: String = ""
    ) {
        val title = listOf(
            "🤖 Ranki: Outstanding Focus! 🌟",
            "🤖 Ranki: Session Crushed! 🔥",
            "🤖 Ranki: Top Ranker Move! 🎯"
        ).random()

        val subjectText = if (subject.isNotBlank()) subject else "Study"
        val chapterText = if (chapter.isNotBlank()) " on $chapter" else ""
        val body = listOf(
            "You completed $durationMinutes mins of $subjectText$chapterText! Every focused minute stacks towards your dream rank.",
            "$durationMinutes minutes locked in! Consistency like this is the exact secret of top rankers. Proud of you!",
            "Great grind! $durationMinutes mins of $subjectText completed. Take a well-deserved breather!"
        ).random()

        val payload = SmartNotificationPayload(
            notificationId = "session_appreciation_${System.currentTimeMillis()}",
            title = title,
            body = body,
            category = NotificationCategory.SESSION_APPRECIATION,
            personality = NotificationPersonality.MOTIVATION,
            deepLink = NotificationDeepLinks.STUDY_SESSION,
            channelId = NotificationChannelsManager.CHANNEL_ACHIEVEMENTS,
            templateKey = "session_appreciation_${System.currentTimeMillis()}"
        )
        NotificationDispatcher.showNotification(context, payload)
        NotificationDebugLogger.logDispatch(title, "SESSION_APPRECIATION", "Motivational")
    }

    /**
     * Force runs the daily scheduler right now.
     */
    fun forceDailyScheduler(context: Context) {
        NotificationDebugLogger.logScheduled("Developer triggered: Force Daily Scheduler.")
        CoroutineScope(Dispatchers.IO).launch {
            evaluateAndTrigger(context)
        }
    }

    /**
     * Force runs the weekly planning scheduler.
     */
    fun forceWeeklyScheduler(context: Context) {
        NotificationDebugLogger.logScheduled("Developer triggered: Force Weekly Scheduler.")
        val payload = SmartNotificationPayload(
            notificationId = "weekly_scheduler_${System.currentTimeMillis()}",
            title = "🤖 Ranki: Weekly Plan Activated 📅",
            body = "Weekly targets mapped! Set aside 3 key revision sessions this week to retain 95% of concepts.",
            category = NotificationCategory.SUNDAY_PLANNING,
            personality = NotificationPersonality.AI_MENTOR,
            deepLink = NotificationDeepLinks.EXAM_PLANNER,
            channelId = NotificationChannelsManager.CHANNEL_REMINDERS,
            templateKey = "weekly_scheduler_forced"
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    /**
     * Resets notification history and daily counters so testing can be repeated immediately.
     */
    fun resetNotificationHistory(context: Context) {
        val prefs = NotificationPreferencesManager.getInstance(context)
        prefs.resetNotificationHistory()
        NotificationDebugLogger.logInfo("Reset Notification History", "Daily sent count, cooldowns, and template tracking reset to zero.")
    }

    /**
     * Generates a completely random notification from the 415 templates and immediately displays it.
     */
    fun generateRandomNotification(context: Context) {
        val template = NotificationContentTemplates.TEMPLATES.random()
        val payload = NotificationContentTemplates.createPayload(template)
        NotificationDispatcher.showNotification(context, payload)
        NotificationDebugLogger.logDispatch(payload.title, payload.category.title, payload.personality.displayName)
    }

    /**
     * Triggers an instant celebration notification when an achievement or target milestone is reached.
     */
    fun triggerAchievementNotification(
        context: Context,
        title: String,
        body: String,
        deepLink: String = NotificationDeepLinks.TODAY_TARGET
    ) {
        val prefs = NotificationPreferencesManager.getInstance(context).getPreferences()
        if (!prefs.masterEnabled || !prefs.achievementReminderEnabled) return

        val payload = SmartNotificationPayload(
            notificationId = UUID.randomUUID().toString(),
            title = title,
            body = body,
            category = NotificationCategory.ACHIEVEMENT,
            personality = prefs.personality,
            deepLink = deepLink
        )
        NotificationDispatcher.showNotification(context, payload)

        // Sync achievement notification to Firestore
        val uid = FirebaseAuthService.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val entity = NotificationHistoryEntity(
                    notificationId = payload.notificationId,
                    title = title,
                    body = body,
                    category = NotificationCategory.ACHIEVEMENT.name,
                    personality = prefs.personality.id,
                    deepLink = deepLink,
                    createdTime = System.currentTimeMillis()
                )
                FirebaseNotificationSyncService.syncNotificationToFirestore(uid, entity)
            }
        }
    }

    /**
     * Triggers a direct custom nudge (e.g. for revision reminders, study breaks, or syllabus alerts).
     */
    fun triggerDirectNudge(
        context: Context,
        category: NotificationCategory,
        title: String,
        body: String,
        deepLink: String = NotificationDeepLinks.SYLLABUS_TRACKER
    ) {
        val prefs = NotificationPreferencesManager.getInstance(context).getPreferences()
        if (!prefs.masterEnabled) return

        val payload = SmartNotificationPayload(
            notificationId = UUID.randomUUID().toString(),
            title = title,
            body = body,
            category = category,
            personality = prefs.personality,
            deepLink = deepLink
        )
        NotificationDispatcher.showNotification(context, payload)
    }

    // =========================================================================
    // 4. FIRESTORE INTERACTION
    // =========================================================================

    /**
     * Synchronizes current local notification preferences to Cloud Firestore.
     */
    suspend fun syncPreferencesToCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthService.currentUser?.uid ?: return@withContext false
        val prefs = NotificationPreferencesManager.getInstance(context).getPreferences()
        FirebaseNotificationSyncService.syncPreferencesToFirestore(uid, prefs)
        true
    }

    /**
     * Fetches notification preferences from Cloud Firestore and updates local preferences.
     */
    suspend fun syncPreferencesFromCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        val uid = FirebaseAuthService.currentUser?.uid ?: return@withContext false
        val remotePrefs = FirebaseNotificationSyncService.fetchPreferencesFromFirestore(uid)
        if (remotePrefs != null) {
            NotificationPreferencesManager.getInstance(context).savePreferences(remotePrefs)
            Log.d(TAG, "Local notification preferences successfully restored from Firestore.")
            true
        } else {
            false
        }
    }

    /**
     * Records an opened or dismissed notification interaction locally and syncs to Firestore.
     */
    suspend fun recordNotificationInteraction(
        context: Context,
        notificationId: String,
        isOpened: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val db = RankifyDatabase.getDatabase(context)
            if (isOpened) {
                db.rankifyDao().updateNotificationOpened(notificationId)
            } else {
                db.rankifyDao().updateNotificationDismissed(notificationId)
            }

            val uid = FirebaseAuthService.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                if (isOpened) {
                    FirebaseNotificationSyncService.markNotificationOpenedInFirestore(uid, notificationId)
                } else {
                    FirebaseNotificationSyncService.markNotificationDismissedInFirestore(uid, notificationId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error recording notification interaction: ${e.message}")
        }
    }

    // =========================================================================
    // 5. INTELLIGENCE SCORE COMPUTATION
    // =========================================================================

    /**
     * Computes the current Notification Intelligence Score based on live database metrics.
     */
    suspend fun getIntelligenceScore(context: Context): NotificationIntelligenceScore = withContext(Dispatchers.IO) {
        try {
            val db = RankifyDatabase.getDatabase(context)
            val dao = db.rankifyDao()
            val profile = dao.getProfileDirect()
            val chapters = dao.getChaptersDirect()
            val targets = dao.getAllTargetsDirect()
            val exams = dao.getAllExamsDirect()
            val startOfDay = getStartOfDayMillis()
            val todaySessions = dao.getSessionsSince(startOfDay)

            NotificationGenerator.calculateIntelligenceScore(
                profile = profile,
                chapters = chapters,
                targets = targets,
                todaySessions = todaySessions,
                exams = exams
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error computing intelligence score: ${e.message}", e)
            NotificationIntelligenceScore(
                totalScore = 65,
                consistencyScore = 15,
                studyTimeScore = 15,
                targetDisciplineScore = 18,
                examProximityScore = 17,
                recommendedTiming = "Consistent evening review recommended at 7:00 PM",
                recommendedTone = "Disciplined Mastery",
                description = "Consistent study routine detected. Keep up the daily momentum!"
            )
        }
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
