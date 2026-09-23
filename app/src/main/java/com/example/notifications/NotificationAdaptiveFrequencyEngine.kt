package com.example.notifications

import android.content.Context
import com.example.data.model.ExamEntity
import com.example.data.model.StudentProfile
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Student engagement tiers based on real-time daily study behaviour.
 */
enum class StudentActivityTier(
    val displayName: String,
    val minDaily: Int,
    val maxDaily: Int,
    val defaultBase: Int,
    val description: String
) {
    VERY_ACTIVE(
        displayName = "Very Active Student",
        minDaily = 2,
        maxDaily = 4,
        defaultBase = 3,
        description = "High focus (>=90 mins or targets completed). Minimal disruption."
    ),
    AVERAGE(
        displayName = "Average Student",
        minDaily = 5,
        maxDaily = 7,
        defaultBase = 6,
        description = "Moderate focus (1-89 mins or active sessions). Balanced momentum."
    ),
    INACTIVE(
        displayName = "Inactive Student",
        minDaily = 8,
        maxDaily = 12,
        defaultBase = 10,
        description = "0 mins studied or inactive for hours. Re-engagement reminders."
    )
}

/**
 * Result data class for calculated daily limits.
 */
data class AdaptiveDailyLimitResult(
    val tier: StudentActivityTier,
    val baseLimit: Int,
    val examDaysRemaining: Long?,
    val examBoost: Int,
    val isExamTomorrow: Boolean,
    val totalAllowedToday: Int,
    val explanation: String
)

/**
 * Data class representing a timeline slot for "Preview Today's Notifications".
 */
data class TimelineSlotPreview(
    val timeRange: String,
    val title: String,
    val category: String,
    val description: String,
    val status: String
)

/**
 * Engine that computes adaptive daily notification limits based on student activity,
 * exam proximity, and enforces strict timeline constraints (7:00 AM - 10:15 PM, 30-min intervals).
 */
object NotificationAdaptiveFrequencyEngine {

    const val MIN_SPACING_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes minimum spacing

    /**
     * Determines the student's current activity tier based on today's study minutes,
     * target progress, and streak status.
     */
    fun evaluateActivityTier(
        todayStudyMinutes: Int,
        completedTargetsCount: Int,
        pendingTargetsCount: Int,
        streakDays: Int,
        lastAppOpenMs: Long
    ): StudentActivityTier {
        val hoursSinceLastOpen = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastAppOpenMs)

        return when {
            // Very Active: studied >= 90 mins, or completed >= 2 targets, or high streak with active study today
            todayStudyMinutes >= 90 || completedTargetsCount >= 2 || (streakDays >= 5 && todayStudyMinutes >= 45) -> {
                StudentActivityTier.VERY_ACTIVE
            }
            // Average: studied 1-89 mins, or 1 target completed, or opened app recently (< 4 hours)
            todayStudyMinutes > 0 || completedTargetsCount >= 1 || hoursSinceLastOpen < 4 -> {
                StudentActivityTier.AVERAGE
            }
            // Inactive: 0 mins studied, 0 targets completed, inactive for hours
            else -> {
                StudentActivityTier.INACTIVE
            }
        }
    }

    /**
     * Finds nearest active exam days remaining.
     */
    fun getNearestExamDays(exams: List<ExamEntity>): Pair<Long, ExamEntity>? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Date()
        return exams
            .filter { it.isActive && it.examDate.isNotBlank() }
            .mapNotNull { exam ->
                try {
                    val date = sdf.parse(exam.examDate) ?: return@mapNotNull null
                    val diff = date.time - now.time
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    if (days >= 0) Pair(days, exam) else null
                } catch (e: Exception) {
                    null
                }
            }
            .minByOrNull { it.first }
    }

    /**
     * Calculates the adaptive daily notification limit according to the exact brief specifications:
     * - Very Active: 2-4 (base: 3)
     * - Average: 5-7 (base: 6)
     * - Inactive: 8-12 (base: 10)
     * - Exam within 30 days: +2
     * - Exam within 7 days: +3
     * - Exam tomorrow (<= 1 day): allow up to 15 (only useful reminders, never spam)
     */
    fun calculateAdaptiveDailyLimit(
        todayStudyMinutes: Int,
        completedTargetsCount: Int,
        pendingTargetsCount: Int,
        streakDays: Int,
        lastAppOpenMs: Long,
        exams: List<ExamEntity>
    ): AdaptiveDailyLimitResult {
        val tier = evaluateActivityTier(
            todayStudyMinutes = todayStudyMinutes,
            completedTargetsCount = completedTargetsCount,
            pendingTargetsCount = pendingTargetsCount,
            streakDays = streakDays,
            lastAppOpenMs = lastAppOpenMs
        )

        val nearestExamInfo = getNearestExamDays(exams)
        val daysRemaining = nearestExamInfo?.first

        var base = tier.defaultBase
        var examBoost = 0
        var isExamTomorrow = false
        var totalAllowed: Int

        if (daysRemaining != null) {
            when {
                daysRemaining <= 1L -> {
                    // Exam tomorrow -> allow up to 15 useful reminders
                    isExamTomorrow = true
                    totalAllowed = 15
                }
                daysRemaining <= 7L -> {
                    // Exam within 7 days -> increase by 3
                    examBoost = 3
                    totalAllowed = (base + examBoost).coerceAtMost(15)
                }
                daysRemaining <= 30L -> {
                    // Exam within 30 days -> increase by 2
                    examBoost = 2
                    totalAllowed = (base + examBoost).coerceAtMost(14)
                }
                else -> {
                    totalAllowed = base
                }
            }
        } else {
            totalAllowed = base
        }

        val examDesc = when {
            isExamTomorrow -> "Exam tomorrow (${nearestExamInfo?.second?.examName ?: "Board/Competitive"}) -> Max 15 useful reminders allowed"
            daysRemaining != null && daysRemaining <= 7L -> "${nearestExamInfo?.second?.examName ?: "Exam"} in $daysRemaining days (+3 boost)"
            daysRemaining != null && daysRemaining <= 30L -> "${nearestExamInfo?.second?.examName ?: "Exam"} in $daysRemaining days (+2 boost)"
            else -> "No immediate exam (< 30 days)"
        }

        val explanation = "${tier.displayName} (Base: $base) + $examDesc = $totalAllowed notifications/day"

        return AdaptiveDailyLimitResult(
            tier = tier,
            baseLimit = base,
            examDaysRemaining = daysRemaining,
            examBoost = examBoost,
            isExamTomorrow = isExamTomorrow,
            totalAllowedToday = totalAllowed,
            explanation = explanation
        )
    }

    /**
     * Checks if current time is within strict Quiet Hours:
     * Never notify before 7:00 AM (07:00)
     * Never notify after 10:15 PM (22:15)
     */
    fun isStrictQuietHours(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        // Before 7:00 AM
        if (hour < 7) return true

        // After 10:15 PM (22:15)
        if (hour > 22 || (hour == 22 && minute > 15)) return true

        return false
    }

    /**
     * Returns full timeline preview for Developer Tools: Preview Today's Notifications.
     */
    fun getTimelinePreview(
        currentSentCount: Int,
        totalAllowed: Int,
        hasSchoolProfile: Boolean = true
    ): List<TimelineSlotPreview> {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMin = cal.get(Calendar.MINUTE)
        val currentTimeDec = currentHour + currentMin / 60.0

        fun getStatus(startDec: Double, endDec: Double): String {
            return when {
                currentTimeDec > endDec -> "Completed"
                currentTimeDec in startDec..endDec -> "Active Window"
                else -> "Upcoming"
            }
        }

        return listOf(
            TimelineSlotPreview(
                timeRange = "07:00 - 08:30",
                title = "Morning Motivation",
                category = "MORNING",
                description = "High energy start, day targets blueprint, and mindset activation.",
                status = getStatus(7.0, 8.5)
            ),
            TimelineSlotPreview(
                timeRange = "13:30 - 14:00",
                title = "School Leaving Reminder",
                category = "SCHOOL_LEAVING",
                description = if (hasSchoolProfile) "Pack bag, travel recall, and school-to-desk transition." else "Inactive (school profile omitted)",
                status = getStatus(13.5, 14.0)
            ),
            TimelineSlotPreview(
                timeRange = "14:00 - 15:00",
                title = "After School Sprint",
                category = "AFTER_SCHOOL",
                description = "Quick 25-min decompression sprint before fatigue sets in.",
                status = getStatus(14.0, 15.0)
            ),
            TimelineSlotPreview(
                timeRange = "16:00 - 16:59",
                title = "Start Studying Reminder",
                category = "START_STUDYING",
                description = "Desk setup reminder, eliminate distractions, initiate stopwatch.",
                status = getStatus(16.0, 17.0)
            ),
            TimelineSlotPreview(
                timeRange = "17:00 - 17:59",
                title = "Weak Chapter Remediation",
                category = "WEAK_CHAPTER",
                description = "AI directs focus to lowest accuracy chapter (e.g. Wave Optics / Equilibrium).",
                status = getStatus(17.0, 18.0)
            ),
            TimelineSlotPreview(
                timeRange = "18:00 - 18:59",
                title = "Target Progress Check-in",
                category = "TARGET_PROGRESS",
                description = "Mid-day target progress update & sprint motivation.",
                status = getStatus(18.0, 19.0)
            ),
            TimelineSlotPreview(
                timeRange = "19:00 - 19:59",
                title = "Study Break & Hydration",
                category = "BREAK_REMINDER",
                description = "Stretch, hydrate, eye-rest to prepare for peak evening focus.",
                status = getStatus(19.0, 20.0)
            ),
            TimelineSlotPreview(
                timeRange = "20:00 - 20:29",
                title = "Revision Reminder",
                category = "REVISION",
                description = "Spaced repetition prompt for older completed chapters.",
                status = getStatus(20.0, 20.5)
            ),
            TimelineSlotPreview(
                timeRange = "20:30 - 21:29",
                title = "Exam Countdown Warning",
                category = "EXAM_COUNTDOWN",
                description = "Countdown alerts for upcoming board/competitive exams.",
                status = getStatus(20.5, 21.5)
            ),
            TimelineSlotPreview(
                timeRange = "21:30 - 21:59",
                title = "Night Wrap-up",
                category = "NIGHT",
                description = "Log final targets, lock streak, celebrate today's hours.",
                status = getStatus(21.5, 22.0)
            ),
            TimelineSlotPreview(
                timeRange = "22:00 - 22:15",
                title = "Sleep & Rest Reminder",
                category = "SLEEP_REMINDER",
                description = "Memory consolidation sleep reminder before 10:15 PM quiet hours.",
                status = getStatus(22.0, 22.25)
            )
        )
    }
}
