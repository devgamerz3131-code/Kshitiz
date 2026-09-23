package com.example.notifications

import android.util.Log
import com.example.data.model.ExamEntity
import com.example.data.model.StudentProfile
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity
import com.example.notifications.model.NotificationCategory
import com.example.notifications.model.NotificationIntelligenceScore
import com.example.notifications.model.NotificationPersonality
import com.example.notifications.model.NotificationPreferences
import com.example.notifications.model.SmartNotificationPayload
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Intelligent Smart Notification Generator for Rankify.
 * - Enforces behavior-based adaptive daily frequency limits (Very Active 2-4, Average 5-7, Inactive 8-12, Exam +2/+3/up to 15).
 * - Enforces strict Quiet Hours (never before 7:00 AM, never after 10:15 PM).
 * - Enforces minimum 30-minute interval spacing.
 * - Pauses during active study sessions, tests, and video playback.
 * - Enforces full 14-day anti-repetition cooldown and duplicate prevention.
 * - Aligns with the 24-hour Student Notification Timeline.
 * - Records detailed debug logs for skips and decisions.
 */
object NotificationGenerator {

    private const val TAG = "NotificationGenerator"

    fun evaluateSmartNotification(
        currentTimeMs: Long = System.currentTimeMillis(),
        prefs: NotificationPreferences,
        prefsManager: NotificationPreferencesManager,
        profile: StudentProfile?,
        chapters: List<SyllabusChapterEntity>,
        targets: List<StudyTargetEntity>,
        todaySessions: List<StudySessionEntity>,
        exams: List<ExamEntity>
    ): SmartNotificationPayload? {

        // 1. Master toggle check
        if (!prefs.masterEnabled) {
            NotificationDebugLogger.logSkip("Master notification toggle is disabled.")
            return null
        }

        // 2. Active study protection: Never disturb a student in focus!
        if (prefsManager.isStudentInDoNotDisturbMode()) {
            NotificationDebugLogger.logSkip("Skipped because user studying (session/test/dock active).")
            return null
        }

        // 3. Strict Quiet Hours: Never notify before 7:00 AM, never after 10:15 PM
        if (NotificationAdaptiveFrequencyEngine.isStrictQuietHours(currentTimeMs)) {
            NotificationDebugLogger.logSkip("Skipped because quiet hours (strict 22:15 - 07:00 window).")
            return null
        }

        // 4. Minimum 30-minute interval between consecutive notifications
        if (!prefsManager.canSendWithCooldown(currentTimeMs)) {
            val lastSent = prefsManager.getLastNotificationTimestamp()
            val minsSince = TimeUnit.MILLISECONDS.toMinutes(currentTimeMs - lastSent)
            val remainingMins = 30 - minsSince
            NotificationDebugLogger.logSkip("Skipped because minimum spacing active (< 30 minutes since last alert, $remainingMins min left).")
            return null
        }

        val totalMinutesToday = todaySessions.sumOf { it.durationMinutes }
        val pendingTargets = targets.filter { !it.status.equals("Completed", ignoreCase = true) }
        val completedTargets = targets.filter { it.status.equals("Completed", ignoreCase = true) }
        val allTargetsComplete = targets.isNotEmpty() && pendingTargets.isEmpty()
        val lastOpenTime = prefsManager.getLastAppOpenTimestamp()

        // 5. Adaptive Daily Limit Check based on student behavior
        val adaptiveResult = NotificationAdaptiveFrequencyEngine.calculateAdaptiveDailyLimit(
            todayStudyMinutes = totalMinutesToday,
            completedTargetsCount = completedTargets.size,
            pendingTargetsCount = pendingTargets.size,
            streakDays = profile?.streakDays ?: 0,
            lastAppOpenMs = lastOpenTime,
            exams = exams
        )

        val dailySent = prefsManager.getDailySentCount()
        if (dailySent >= adaptiveResult.totalAllowedToday) {
            NotificationDebugLogger.logSkip(
                "Skipped because daily limit reached ($dailySent/${adaptiveResult.totalAllowedToday} today) [${adaptiveResult.tier.displayName}]."
            )
            return null
        }

        // 6. Time parsing for timeline slots
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val timeDec = currentHour + (currentMinute / 60.0)

        val hoursSinceLastOpen = TimeUnit.MILLISECONDS.toHours(currentTimeMs - lastOpenTime)
        val nearestExamInfo = NotificationAdaptiveFrequencyEngine.getNearestExamDays(exams)

        // =========================================================================
        // TIMELINE MATCHING & EVALUATION
        // =========================================================================

        // A. INACTIVITY TRIGGER: If inactive for > 3 hours during active hours (09:00 - 20:00) with 0 study
        if (timeDec in 9.0..20.0 && totalMinutesToday == 0 && hoursSinceLastOpen >= 3) {
            val candidate = findEligibleTemplate(
                category = NotificationCategory.MOTIVATION,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            ) ?: findEligibleTemplate(
                category = NotificationCategory.FUNNY_ROAST,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            ) ?: findEligibleTemplate(
                category = NotificationCategory.COMEBACK,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            )

            if (candidate != null) {
                return NotificationContentTemplates.createPayload(candidate)
            }
        }

        // B. 07:00 - 08:30: Morning Motivation
        if (timeDec in 7.0..8.5) {
            if (prefs.morningReminderEnabled && totalMinutesToday == 0) {
                val candidate = findEligibleTemplate(
                    category = NotificationCategory.MORNING,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                )
                if (candidate != null) {
                    return NotificationContentTemplates.createPayload(candidate)
                }
            }
        }

        // C. 13:30 - 14:00: School Leaving Reminder (if school profile exists)
        if (timeDec in 13.5..14.0) {
            val candidate = findEligibleTemplate(
                category = NotificationCategory.SCHOOL_LEAVING,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            ) ?: findEligibleTemplate(
                category = NotificationCategory.AFTER_SCHOOL,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            )
            if (candidate != null) {
                return NotificationContentTemplates.createPayload(candidate)
            }
        }

        // D. 14:00 - 15:00: After School Sprint
        if (timeDec in 14.0..15.0) {
            if (prefs.afterSchoolReminderEnabled) {
                val candidate = findEligibleTemplate(
                    category = NotificationCategory.AFTER_SCHOOL,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                )
                if (candidate != null) {
                    return NotificationContentTemplates.createPayload(candidate)
                }
            }
        }

        // E. 16:00 - 16:59: Start Studying Reminder
        if (timeDec in 16.0..17.0) {
            if (totalMinutesToday < 30) {
                val candidate = findEligibleTemplate(
                    category = NotificationCategory.START_STUDYING,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                ) ?: findEligibleTemplate(
                    category = NotificationCategory.TARGET,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                )
                if (candidate != null) {
                    return NotificationContentTemplates.createPayload(candidate)
                }
            }
        }

        // F. 17:00 - 17:59: Weak Chapter Reminder
        if (timeDec in 17.0..18.0) {
            if (prefs.weakChapterReminderEnabled) {
                val weakChapter = chapters.find { it.isWeakTopic }
                    ?: chapters.minByOrNull { it.completionPercentage }
                if (weakChapter != null) {
                    val candidate = findEligibleTemplate(
                        category = NotificationCategory.WEAK_CHAPTER,
                        userPersonality = prefs.personality,
                        prefsManager = prefsManager
                    )
                    if (candidate != null) {
                        return NotificationContentTemplates.createPayload(
                            candidate,
                            mapOf("chapter" to weakChapter.title, "subject" to weakChapter.subject)
                        )
                    }
                }
            }
        }

        // G. 18:00 - 18:59: Target Progress Check-in
        if (timeDec in 18.0..19.0) {
            if (allTargetsComplete) {
                NotificationDebugLogger.logSkip("Skipped because target completed (100% conquered today).")
                // Offer appreciation if enabled
                if (prefs.achievementReminderEnabled) {
                    val candidate = findEligibleTemplate(
                        category = NotificationCategory.TARGET_COMPLETED,
                        userPersonality = prefs.personality,
                        prefsManager = prefsManager
                    )
                    if (candidate != null) {
                        return NotificationContentTemplates.createPayload(candidate)
                    }
                }
            } else if (pendingTargets.isNotEmpty()) {
                val candidate = findEligibleTemplate(
                    category = NotificationCategory.TARGET_PROGRESS,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                ) ?: findEligibleTemplate(
                    category = NotificationCategory.TARGET,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                )
                if (candidate != null) {
                    return NotificationContentTemplates.createPayload(
                        candidate,
                        mapOf("pending" to pendingTargets.size.toString())
                    )
                }
            }
        }

        // H. 19:00 - 19:59: Break Reminder & Hydration
        if (timeDec in 19.0..20.0) {
            val candidate = findEligibleTemplate(
                category = NotificationCategory.BREAK_REMINDER,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            ) ?: findEligibleTemplate(
                category = NotificationCategory.WATER_REMINDER,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            )
            if (candidate != null) {
                return NotificationContentTemplates.createPayload(candidate)
            }
        }

        // I. 20:00 - 20:29: Revision Reminder
        if (timeDec in 20.0..20.5) {
            if (prefs.revisionReminderEnabled) {
                val unrevisedChapter = chapters
                    .filter { it.conceptsDone }
                    .minByOrNull { it.lastRevisedTimestamp }
                if (unrevisedChapter != null) {
                    val candidate = findEligibleTemplate(
                        category = NotificationCategory.REVISION,
                        userPersonality = prefs.personality,
                        prefsManager = prefsManager
                    )
                    if (candidate != null) {
                        return NotificationContentTemplates.createPayload(
                            candidate,
                            mapOf("chapter" to unrevisedChapter.title, "subject" to unrevisedChapter.subject)
                        )
                    }
                }
            }
        }

        // J. 20:30 - 21:29: Exam Countdown Warning / Streak at Risk
        if (timeDec in 20.5..21.5) {
            // Streak alert if 0 minutes studied
            val streak = profile?.streakDays ?: 0
            if (prefs.streakReminderEnabled && streak > 0 && totalMinutesToday == 0) {
                val candidate = findEligibleTemplate(
                    category = NotificationCategory.STREAK,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                )
                if (candidate != null) {
                    return NotificationContentTemplates.createPayload(
                        candidate,
                        mapOf("streak" to streak.toString())
                    )
                }
            }

            // Exam Countdown
            if (prefs.examReminderEnabled && nearestExamInfo != null) {
                val days = nearestExamInfo.first
                val exam = nearestExamInfo.second
                val candidate = findEligibleTemplate(
                    category = NotificationCategory.EXAM_COUNTDOWN,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                )
                if (candidate != null) {
                    return NotificationContentTemplates.createPayload(
                        candidate,
                        mapOf("days" to days.toString(), "exam" to exam.examName)
                    )
                }
            }
        }

        // K. 21:30 - 21:59: Night Wrap-up
        if (timeDec in 21.5..22.0) {
            if (prefs.nightReminderEnabled) {
                val candidate = findEligibleTemplate(
                    category = NotificationCategory.NIGHT,
                    userPersonality = prefs.personality,
                    prefsManager = prefsManager
                )
                if (candidate != null) {
                    return NotificationContentTemplates.createPayload(candidate)
                }
            }
        }

        // L. 22:00 - 22:15: Sleep Reminder
        if (timeDec in 22.0..22.25) {
            val candidate = findEligibleTemplate(
                category = NotificationCategory.SLEEP_REMINDER,
                userPersonality = prefs.personality,
                prefsManager = prefsManager
            )
            if (candidate != null) {
                return NotificationContentTemplates.createPayload(candidate)
            }
        }

        NotificationDebugLogger.logSkip("No slot matched current time ($currentHour:${String.format("%02d", currentMinute)}) or criteria satisfied.")
        return null
    }

    /**
     * Finds a candidate template matching the category, taking into account user personality
     * preference and enforcing the 14-day anti-repetition and duplicate prevention rules.
     */
    private fun findEligibleTemplate(
        category: NotificationCategory,
        userPersonality: NotificationPersonality,
        prefsManager: NotificationPreferencesManager,
        keyFilter: (String) -> Boolean = { true }
    ): NotificationTemplate? {
        val allForCategory = NotificationContentTemplates.TEMPLATES
            .filter { (it.category == category || it.category.id == category.id) && keyFilter(it.templateKey) }

        if (allForCategory.isEmpty()) return null

        // Filter by user personality unless MIXED is chosen
        val personalityFiltered = if (userPersonality == NotificationPersonality.MIXED) {
            allForCategory
        } else {
            val match = allForCategory.filter { it.personality == userPersonality }
            if (match.isNotEmpty()) match else allForCategory
        }

        // 14-day anti-repetition filter and duplicate title filter
        val eligible = personalityFiltered.filter {
            !prefsManager.wasTemplateSentInPast14Days(it.templateKey) &&
                    !prefsManager.wasExactTitleSentRecently(it.title)
        }

        return if (eligible.isNotEmpty()) {
            eligible.random()
        } else {
            NotificationDebugLogger.logSkip("Skipped because duplicate (template recently used in 14-day window).", category.name)
            null
        }
    }

    /**
     * Calculates an AI-driven Notification Intelligence Score (0–100) based on student consistency,
     * study hours, target discipline, and upcoming exam proximity.
     */
    fun calculateIntelligenceScore(
        profile: StudentProfile?,
        chapters: List<SyllabusChapterEntity>,
        targets: List<StudyTargetEntity>,
        todaySessions: List<StudySessionEntity>,
        exams: List<ExamEntity>
    ): NotificationIntelligenceScore {
        val consistencyScore = ((profile?.streakDays ?: 0) * 3).coerceIn(5, 25)
        val todayMinutes = todaySessions.sumOf { it.durationMinutes }
        val studyTimeScore = (todayMinutes / 5).coerceIn(5, 25)
        val completedTargets = targets.count { it.status.equals("Completed", ignoreCase = true) }
        val targetDisciplineScore = (completedTargets * 8).coerceIn(5, 25)
        val examProximityScore = if (exams.isNotEmpty()) 22 else 15

        val total = consistencyScore + studyTimeScore + targetDisciplineScore + examProximityScore

        val recommendedTiming = when {
            todayMinutes < 30 -> "Recommended study block: 6:00 PM – 7:30 PM"
            else -> "Peak momentum achieved. Evening revision recommended."
        }

        val recommendedTone = when {
            total > 80 -> "Elite Topper (Motivational)"
            total > 60 -> "Focused Scholar (AI Mentor)"
            else -> "High Accountability (Savage & Roasts)"
        }

        val description = when {
            total >= 85 -> "Outstanding momentum! Ranki is keeping reminders minimal and celebrations high."
            total >= 65 -> "Solid consistency. Balanced daily nudges scheduled across peak cognitive hours."
            else -> "Study habit formation mode active. Ranki will nudge you with high motivation today."
        }

        return NotificationIntelligenceScore(
            totalScore = total,
            consistencyScore = consistencyScore,
            studyTimeScore = studyTimeScore,
            targetDisciplineScore = targetDisciplineScore,
            examProximityScore = examProximityScore,
            recommendedTiming = recommendedTiming,
            recommendedTone = recommendedTone,
            description = description
        )
    }
}
