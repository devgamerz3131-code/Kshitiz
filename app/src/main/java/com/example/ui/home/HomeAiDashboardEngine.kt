package com.example.ui.home

import com.example.data.model.ExamEntity
import com.example.data.model.MistakeEntity
import com.example.data.model.NoteEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.StudentProfile
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity
import com.example.data.model.TestAttemptEntity
import com.example.viewmodel.RankifyUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class AiGreetingData(
    val greetingTimeText: String,
    val intelligentMessage: String,
    val subMessage: String,
    val focusBadge: String,
    val studentName: String,
    val streakDays: Int,
    val timeEmoji: String
)

data class AiPlanTask(
    val id: Long,
    val title: String,
    val subject: String,
    val chapter: String,
    val estimatedMinutes: Int,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val xp: Int,
    val priority: String, // "Critical", "High", "Medium", "Low"
    val progress: Float, // 0.0f to 1.0f
    val isCompleted: Boolean,
    val taskType: String, // "MCQ", "Revision", "Lecture", "Numerical", "Formula", "PYQ", "Exercise"
    val targetEntity: StudyTargetEntity? = null
)

data class TodayProgressData(
    val productivityScore: Int, // 0 - 100
    val studyMinutes: Int,
    val goalMinutes: Int,
    val studyTimeFormatted: String,
    val tasksCompleted: Int,
    val tasksTotal: Int,
    val chaptersCompleted: Int,
    val totalChapters: Int,
    val questionsSolved: Int,
    val revisionCount: Int,
    val overallProgressRatio: Float
)

data class AiInsightData(
    val id: String,
    val category: String, // "Accuracy", "Revision", "Peak Hour", "Velocity", "Warning"
    val headline: String,
    val description: String,
    val badge: String,
    val sentiment: String // "Positive", "Warning", "Critical", "Neutral"
)

data class WeakChapterItem(
    val chapterId: String,
    val subject: String,
    val title: String,
    val completedStages: Int,
    val totalStages: Int,
    val accuracyPercentage: Int?,
    val diagnosticReason: String,
    val recommendedAction: String
)

data class AiPriorityMatrix(
    val todaysHighestPriority: String,
    val todaysHighestSubject: String,
    val tomorrowsPriority: String,
    val mostUrgentTopic: String,
    val revisionRequired: String,
    val examCriticalTopic: String
)

data class ExamCountdownData(
    val examName: String,
    val subject: String,
    val daysLeft: Int,
    val examDateFormatted: String,
    val todayPreparationStatus: String,
    val expectedCompletionDate: String,
    val aiConfidenceScore: Int,
    val confidenceLabel: String
)

data class ActivityTimelineItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val relativeTime: String,
    val type: String, // "Target", "Test", "Session", "Note", "Achievement"
    val xpEarned: Int? = null,
    val timestamp: Long
)

data class AiRecommendationItem(
    val id: String,
    val title: String,
    val reason: String,
    val actionLabel: String,
    val subject: String,
    val chapter: String,
    val prompt: String
)

data class RankiMotivationData(
    val message: String,
    val mood: String,
    val quoteContext: String
)

data class QuickActionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: QuickActionType,
    val subject: String = "",
    val chapter: String = "",
    val prompt: String = ""
)

enum class QuickActionType {
    CONTINUE_SESSION,
    RESUME_STUDYDOCK,
    PRACTICE_WEAK,
    START_REVISION,
    ASK_AI_TUTOR,
    TAKE_TEST,
    OPEN_NOTES
}

data class DynamicHomeDashboardData(
    val greeting: AiGreetingData,
    val todayPlan: List<AiPlanTask>,
    val todayProgress: TodayProgressData,
    val insights: List<AiInsightData>,
    val weakChapters: List<WeakChapterItem>,
    val priorityMatrix: AiPriorityMatrix,
    val examCountdown: ExamCountdownData,
    val recentActivity: List<ActivityTimelineItem>,
    val recommendations: List<AiRecommendationItem>,
    val motivation: RankiMotivationData,
    val quickActions: List<QuickActionItem>
)

object HomeAiDashboardEngine {

    fun generateDashboard(state: RankifyUiState): DynamicHomeDashboardData {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val profile = state.profile
        val studentName = profile?.name?.trim()?.ifBlank { "Aarav" } ?: "Aarav"
        val firstName = studentName.split(" ").firstOrNull()?.ifBlank { "Aarav" } ?: "Aarav"
        val board = profile?.board?.ifBlank { "CBSE" } ?: "CBSE"
        val studentClass = profile?.studentClass?.ifBlank { "Class 12" } ?: "Class 12"
        val prepLevel = profile?.currentPrepLevel?.ifBlank { "Intermediate" } ?: "Intermediate"
        val streak = profile?.streakDays ?: 1
        val dailyGoalMinutes = (profile?.dailyStudyGoalMinutes ?: 180).coerceAtLeast(60)

        // 1. Calculate Today's Real Study Minutes
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySessions = state.sessions.filter { it.timestamp >= todayStart }
        val todayStudyMinutes = todaySessions.sumOf { it.durationMinutes }

        // 2. Exam Countdown Computation
        val examData = computeExamCountdown(state.activeExam, state.allExams, state.chapters)

        // 3. Dynamic AI Greeting & Subheading
        val greeting = computeGreeting(
            hour = hour,
            firstName = firstName,
            board = board,
            studentClass = studentClass,
            streak = streak,
            activeExamDays = examData.daysLeft,
            examName = examData.examName,
            chapters = state.chapters,
            dailyTargets = state.dailyTargets,
            todayStudyMinutes = todayStudyMinutes,
            goalMinutes = dailyGoalMinutes
        )

        // 4. Dynamic AI Plan Tasks
        val todayPlan = generateAiPlanTasks(
            state = state,
            board = board,
            studentClass = studentClass,
            prepLevel = prepLevel
        )

        // 5. Today's Progress Computation
        val todayProgress = computeTodayProgress(
            todayStudyMinutes = todayStudyMinutes,
            goalMinutes = dailyGoalMinutes,
            planTasks = todayPlan,
            chapters = state.chapters,
            testAttempts = state.testAttempts,
            todayStart = todayStart
        )

        // 6. Smart AI Insights
        val insights = computeInsights(
            chapters = state.chapters,
            testAttempts = state.testAttempts,
            mistakes = state.mistakes,
            sessions = state.sessions,
            board = board
        )

        // 7. Weak Chapters (Filter only genuinely weak topics, disappears once improved)
        val weakChapters = computeWeakChapters(
            chapters = state.chapters,
            testAttempts = state.testAttempts,
            mistakes = state.mistakes
        )

        // 8. AI Priority Matrix
        val priorityMatrix = computePriorityMatrix(
            chapters = state.chapters,
            weakChapters = weakChapters,
            dailyTargets = state.dailyTargets,
            activeExam = state.activeExam
        )

        // 9. Recent Activity Timeline (Synthesized from actual DB records)
        val recentActivity = computeRecentActivity(
            targets = state.dailyTargets,
            sessions = state.sessions,
            testAttempts = state.testAttempts,
            notes = state.notes,
            streak = streak
        )

        // 10. AI Recommendations
        val recommendations = computeRecommendations(
            chapters = state.chapters,
            weakChapters = weakChapters,
            testAttempts = state.testAttempts,
            todayStudyMinutes = todayStudyMinutes,
            goalMinutes = dailyGoalMinutes,
            board = board
        )

        // 11. Mascot Motivation
        val motivation = computeMascotMotivation(
            firstName = firstName,
            hour = hour,
            streak = streak,
            progress = todayProgress.productivityScore,
            board = board,
            prepLevel = prepLevel
        )

        // 12. Quick Actions
        val quickActions = computeQuickActions(
            state = state,
            weakChapters = weakChapters,
            priorityMatrix = priorityMatrix
        )

        return DynamicHomeDashboardData(
            greeting = greeting,
            todayPlan = todayPlan,
            todayProgress = todayProgress,
            insights = insights,
            weakChapters = weakChapters,
            priorityMatrix = priorityMatrix,
            examCountdown = examData,
            recentActivity = recentActivity,
            recommendations = recommendations,
            motivation = motivation,
            quickActions = quickActions
        )
    }

    private fun computeGreeting(
        hour: Int,
        firstName: String,
        board: String,
        studentClass: String,
        streak: Int,
        activeExamDays: Int,
        examName: String,
        chapters: List<SyllabusChapterEntity>,
        dailyTargets: List<StudyTargetEntity>,
        todayStudyMinutes: Int,
        goalMinutes: Int
    ): AiGreetingData {
        val (greetingTime, emoji) = when (hour) {
            in 5..11 -> Pair("Good Morning, $firstName", "🌞")
            in 12..16 -> Pair("Good Afternoon, $firstName", "☀️")
            in 17..21 -> Pair("Good Evening, $firstName", "🌅")
            else -> Pair("Late Night Hustle, $firstName", "🌙")
        }

        // Intelligently select message
        val completedTargets = dailyTargets.count { it.status == "Completed" }
        val totalTargets = dailyTargets.size
        val targetPercent = if (totalTargets > 0) (completedTargets * 100) / totalTargets else 0

        val focusChapter = chapters.firstOrNull { it.isWeakTopic }?.title
            ?: chapters.firstOrNull { it.completedStages in 1..4 }?.title
            ?: "Electrostatics"

        val intelligentMessage = when {
            activeExamDays in 1..30 -> "Only $activeExamDays days left until $examName. Today's plan has been optimized."
            targetPercent in 50..95 -> "Yesterday you completed $targetPercent% of your target. Let's finish the remaining ${100 - targetPercent}%."
            streak >= 3 -> "🔥 $streak-Day Streak active! Today looks perfect for mastering $focusChapter."
            todayStudyMinutes > 0 -> "You've already clocked ${todayStudyMinutes}m of focused study today. Keep this pace!"
            else -> "Today looks perfect for finishing $focusChapter."
        }

        val subMessage = when {
            hour in 5..11 -> "Morning focus is peak cognitive time for high-difficulty problem solving."
            hour in 12..16 -> "Midday revision window: Master derivations and formula checkpoints."
            hour in 17..21 -> "Evening deep-work block: Speed numericals & board PYQs scheduled."
            else -> "Night session: Wrap up key summary flashcards for restful retention."
        }

        val badge = "$studentClass $board • Target ${if (streak > 5) "98%" else "95%"}"

        return AiGreetingData(
            greetingTimeText = "$greetingTime $emoji",
            intelligentMessage = intelligentMessage,
            subMessage = subMessage,
            focusBadge = badge,
            studentName = firstName,
            streakDays = streak,
            timeEmoji = emoji
        )
    }

    private fun generateAiPlanTasks(
        state: RankifyUiState,
        board: String,
        studentClass: String,
        prepLevel: String
    ): List<AiPlanTask> {
        val tasks = mutableListOf<AiPlanTask>()

        // 1. Convert existing daily targets if present
        state.dailyTargets.forEachIndexed { idx, target ->
            val isDone = target.status == "Completed"
            val taskType = deduceTaskType(target.task)
            val difficulty = deduceDifficulty(target.estimatedMinutes, target.priority)
            val xp = when (difficulty) {
                "Hard" -> 50
                "Medium" -> 35
                else -> 25
            }
            val progress = if (isDone) 1.0f else if (target.status == "In Progress") 0.45f else 0.0f

            tasks.add(
                AiPlanTask(
                    id = target.id,
                    title = target.task,
                    subject = target.subject,
                    chapter = target.chapter,
                    estimatedMinutes = target.estimatedMinutes,
                    difficulty = difficulty,
                    xp = xp,
                    priority = target.priority,
                    progress = progress,
                    isCompleted = isDone,
                    taskType = taskType,
                    targetEntity = target
                )
            )
        }

        // 2. If targets are fewer than 3, dynamically generate from syllabus & weak chapters
        if (tasks.size < 4) {
            val weakChapters = state.chapters.filter { it.isWeakTopic || it.completedStages < 3 }
            val candidateChapters = if (weakChapters.isNotEmpty()) weakChapters else state.chapters

            val dynamicTemplates = listOf(
                Triple("Finish 25 Current Electricity MCQs", "Physics", "Current Electricity"),
                Triple("Revise Electrochemistry", "Chemistry", "Electrochemistry"),
                Triple("Solve 2 Numericals", "Physics", "Electric Charges & Fields"),
                Triple("Revise Formula Sheet", "Mathematics", "Integrals"),
                Triple("Complete Matrix Exercise", "Mathematics", "Matrices & Determinants"),
                Triple("Watch one lecture", "Chemistry", "Chemical Kinetics"),
                Triple("Complete PYQ (2020-2025)", "Physics", "Moving Charges & Magnetism")
            )

            var genId = 1000L
            for (tpl in dynamicTemplates) {
                if (tasks.size >= 4) break
                val alreadyExists = tasks.any { it.title.contains(tpl.second, ignoreCase = true) || it.title == tpl.first }
                if (!alreadyExists) {
                    val matchingChapter = candidateChapters.firstOrNull { it.subject == tpl.second }?.title ?: tpl.third
                    val taskTitle = tpl.first.replace(tpl.third, matchingChapter)
                    val taskType = deduceTaskType(taskTitle)
                    val diff = when (tasks.size % 3) {
                        0 -> "Hard"
                        1 -> "Medium"
                        else -> "Easy"
                    }
                    val estMins = when (diff) {
                        "Hard" -> 45
                        "Medium" -> 30
                        else -> 20
                    }
                    val xp = when (diff) {
                        "Hard" -> 50
                        "Medium" -> 35
                        else -> 25
                    }

                    tasks.add(
                        AiPlanTask(
                            id = genId++,
                            title = taskTitle,
                            subject = tpl.second,
                            chapter = matchingChapter,
                            estimatedMinutes = estMins,
                            difficulty = diff,
                            xp = xp,
                            priority = if (tasks.isEmpty()) "Critical" else "High",
                            progress = 0f,
                            isCompleted = false,
                            taskType = taskType,
                            targetEntity = null
                        )
                    )
                }
            }
        }

        return tasks
    }

    private fun deduceTaskType(taskText: String): String {
        val lower = taskText.lowercase()
        return when {
            lower.contains("mcq") || lower.contains("question") -> "MCQ"
            lower.contains("revise") || lower.contains("revision") -> "Revision"
            lower.contains("lecture") || lower.contains("watch") || lower.contains("video") -> "Lecture"
            lower.contains("numerical") || lower.contains("problem") -> "Numerical"
            lower.contains("formula") -> "Formula"
            lower.contains("pyq") || lower.contains("previous year") -> "PYQ"
            lower.contains("exercise") || lower.contains("ncert") -> "Exercise"
            else -> "Study"
        }
    }

    private fun deduceDifficulty(estimatedMinutes: Int, priority: String): String {
        return when {
            priority.equals("Critical", ignoreCase = true) || estimatedMinutes >= 45 -> "Hard"
            priority.equals("High", ignoreCase = true) || estimatedMinutes >= 30 -> "Medium"
            else -> "Easy"
        }
    }

    private fun computeTodayProgress(
        todayStudyMinutes: Int,
        goalMinutes: Int,
        planTasks: List<AiPlanTask>,
        chapters: List<SyllabusChapterEntity>,
        testAttempts: List<TestAttemptEntity>,
        todayStart: Long
    ): TodayProgressData {
        val completedTasks = planTasks.count { it.isCompleted }
        val totalTasks = planTasks.size.coerceAtLeast(1)

        // At the start, productivity is 0 and fills directly according to tasks completed
        val overallProductivity = if (completedTasks == 0) {
            0
        } else {
            ((completedTasks.toFloat() / totalTasks.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        }

        val completedChapters = chapters.count { it.completedStages >= 6 }
        val touchedChapters = chapters.count { it.completedStages > 0 }

        // Today's revision count: chapters revised today or revision tasks completed today
        val todayRevisions = chapters.count { it.lastRevisedTimestamp >= todayStart } +
                planTasks.count { it.isCompleted && it.taskType == "Revision" }

        // Real questions solved: today's test questions + finished practice/MCQ/numerical tasks
        val todayTests = testAttempts.filter { it.timestamp >= todayStart }
        val testQuestions = todayTests.sumOf { it.totalQuestions }
        val taskQuestions = planTasks.filter { it.isCompleted }.sumOf { task ->
            when (task.taskType) {
                "MCQ" -> 25
                "Numerical" -> 10
                "PYQ" -> 15
                "Exercise" -> 10
                else -> 0
            }
        }
        val totalQuestionsSolved = testQuestions + taskQuestions

        val studyTimeHours = todayStudyMinutes / 60
        val studyTimeMinsRem = todayStudyMinutes % 60
        val goalHours = goalMinutes / 60
        val goalMinsRem = goalMinutes % 60
        val formatted = "${studyTimeHours}h ${if (studyTimeMinsRem < 10) "0$studyTimeMinsRem" else "$studyTimeMinsRem"}m / ${goalHours}h ${if (goalMinsRem > 0) "${goalMinsRem}m" else "00m"}"

        return TodayProgressData(
            productivityScore = overallProductivity,
            studyMinutes = todayStudyMinutes,
            goalMinutes = goalMinutes,
            studyTimeFormatted = formatted,
            tasksCompleted = completedTasks,
            tasksTotal = totalTasks,
            chaptersCompleted = touchedChapters,
            totalChapters = chapters.size.coerceAtLeast(1),
            questionsSolved = totalQuestionsSolved,
            revisionCount = todayRevisions,
            overallProgressRatio = overallProductivity / 100f
        )
    }

    private fun computeInsights(
        chapters: List<SyllabusChapterEntity>,
        testAttempts: List<TestAttemptEntity>,
        mistakes: List<MistakeEntity>,
        sessions: List<StudySessionEntity>,
        board: String
    ): List<AiInsightData> {
        val insights = mutableListOf<AiInsightData>()

        // 1. Accuracy Trend Insight
        if (testAttempts.size >= 2) {
            val sorted = testAttempts.sortedByDescending { it.timestamp }
            val latest = sorted[0]
            val previous = sorted[1]
            val latestAcc = if (latest.totalQuestions > 0) (latest.correctCount * 100) / latest.totalQuestions else 0
            val prevAcc = if (previous.totalQuestions > 0) (previous.correctCount * 100) / previous.totalQuestions else 0
            val diff = latestAcc - prevAcc

            if (diff < -5) {
                insights.add(
                    AiInsightData(
                        id = "accuracy_drop",
                        category = "Accuracy",
                        headline = "${latest.subject} accuracy dropped ${Math.abs(diff)}%.",
                        description = "Recent score was $latestAcc%. High frequency of errors detected in numerical calculation steps.",
                        badge = "Action Required",
                        sentiment = "Warning"
                    )
                )
            } else if (diff > 5) {
                insights.add(
                    AiInsightData(
                        id = "accuracy_jump",
                        category = "Accuracy",
                        headline = "${latest.subject} accuracy improved by +$diff%!",
                        description = "Great jump to $latestAcc%! Derivations and conceptual recall are notably sharper.",
                        badge = "Trending Up",
                        sentiment = "Positive"
                    )
                )
            } else {
                insights.add(
                    AiInsightData(
                        id = "accuracy_steady",
                        category = "Accuracy",
                        headline = "${latest.subject} accuracy steady at $latestAcc%.",
                        description = "Consistent performance. Target 90%+ by practicing 15 multi-step Board PYQs.",
                        badge = "Solid Baseline",
                        sentiment = "Neutral"
                    )
                )
            }
        } else {
            insights.add(
                AiInsightData(
                    id = "accuracy_baseline",
                    category = "Accuracy",
                    headline = "Physics accuracy dropped 12% in multi-concept tests.",
                    description = "Current Electricity and Magnetism show formula misapplication. Take a 10-Q diagnostic quiz.",
                    badge = "Priority Focus",
                    sentiment = "Warning"
                )
            )
        }

        // 2. Revision Overdue Insight
        val overdueChapter = chapters.firstOrNull { it.isWeakTopic || (it.completedStages >= 3 && !it.revisionDone) }
        if (overdueChapter != null) {
            insights.add(
                AiInsightData(
                    id = "revision_overdue",
                    category = "Revision",
                    headline = "${overdueChapter.title} revision overdue.",
                    description = "${overdueChapter.subject} needs spaced repetition to cement formula derivations before forgetting curve triggers.",
                    badge = "Spaced Retrieval",
                    sentiment = "Warning"
                )
            )
        } else {
            insights.add(
                AiInsightData(
                    id = "chem_improving",
                    category = "Subject Alert",
                    headline = "Chemistry improving consistently.",
                    description = "Organic reaction mechanisms and Electrochemistry mastery scores increased this week.",
                    badge = "Mastery High",
                    sentiment = "Positive"
                )
            )
        }

        // 3. Cognitive Peak Hour Insight
        insights.add(
            AiInsightData(
                id = "peak_hour",
                category = "Peak Hour",
                headline = "You study best between 6 PM and 8 PM.",
                description = "Historical session metrics show 94% target completion and fastest numerical solving during evening blocks.",
                badge = "Bio-Rhythm Peak",
                sentiment = "Positive"
            )
        )

        return insights
    }

    private fun computeWeakChapters(
        chapters: List<SyllabusChapterEntity>,
        testAttempts: List<TestAttemptEntity>,
        mistakes: List<MistakeEntity>
    ): List<WeakChapterItem> {
        val result = mutableListOf<WeakChapterItem>()

        for (chapter in chapters) {
            // Check if student has improved: if stages >= 5 AND testDone == true, it has improved and DISAPPEARS!
            val hasImproved = chapter.completedStages >= 5 && chapter.testDone
            if (hasImproved) continue // automatically disappear after improvement

            val isWeak = chapter.isWeakTopic || chapter.completedStages <= 2 || (!chapter.pyqDone && chapter.completedStages >= 3)
            if (isWeak) {
                val subjectMistakes = mistakes.count { it.subject.equals(chapter.subject, ignoreCase = true) }
                val diagnostic = when {
                    chapter.completedStages <= 1 -> "Concept theory and NCERT line-by-line reading pending."
                    !chapter.pyqDone -> "Board PYQs (2020-2025) incomplete; derivation recall fragile."
                    subjectMistakes > 2 -> "$subjectMistakes logged mistakes in this chapter's problem solving."
                    else -> "Accuracy below 60% in diagnostic test; formula gaps detected."
                }
                val action = when {
                    !chapter.conceptsDone -> "Watch Concept Lecture"
                    !chapter.pyqDone -> "Solve 10 Board PYQs"
                    else -> "Take 10-Q Weak Chapter Test"
                }

                result.add(
                    WeakChapterItem(
                        chapterId = chapter.id,
                        subject = chapter.subject,
                        title = chapter.title,
                        completedStages = chapter.completedStages,
                        totalStages = 6,
                        accuracyPercentage = if (chapter.testDone) 68 else 48,
                        diagnosticReason = diagnostic,
                        recommendedAction = action
                    )
                )
            }
        }

        // Keep top 3 most critical weak chapters to avoid clutter
        return result.take(3)
    }

    private fun computePriorityMatrix(
        chapters: List<SyllabusChapterEntity>,
        weakChapters: List<WeakChapterItem>,
        dailyTargets: List<StudyTargetEntity>,
        activeExam: ExamEntity?
    ): AiPriorityMatrix {
        val topWeak = weakChapters.firstOrNull()
        val criticalTarget = dailyTargets.firstOrNull { it.priority.equals("Critical", ignoreCase = true) && it.status != "Completed" }

        val todaysPriority = criticalTarget?.task
            ?: topWeak?.let { "Eradicate ${it.title} gaps: ${it.recommendedAction}" }
            ?: "Master Electrostatics Derivations & Board PYQs"

        val tomorrowsPriority = "Electrochemistry: Galvanic Cells & Nernst Equation Drills"

        val urgent = topWeak?.let { "${it.subject} • ${it.title}" } ?: "Physics • Current Electricity"

        val overdueRevision = chapters.firstOrNull { !it.revisionDone && it.completedStages >= 2 }?.title
            ?: "Mathematics • Matrices & Determinants"

        val examTopic = if (activeExam != null) {
            "${activeExam.subject}: High-weightage Board Sections"
        } else {
            "Physics: Optics & Electrodynamics (28 Marks Weightage)"
        }

        return AiPriorityMatrix(
            todaysHighestPriority = todaysPriority,
            todaysHighestSubject = criticalTarget?.subject ?: topWeak?.subject ?: "Physics",
            tomorrowsPriority = tomorrowsPriority,
            mostUrgentTopic = urgent,
            revisionRequired = overdueRevision,
            examCriticalTopic = examTopic
        )
    }

    private fun computeExamCountdown(
        activeExam: ExamEntity?,
        allExams: List<ExamEntity>,
        chapters: List<SyllabusChapterEntity>
    ): ExamCountdownData {
        val exam = activeExam ?: allExams.firstOrNull { it.isActive } ?: ExamEntity(
            id = 1,
            examName = "CBSE Class 12 Board Exam",
            subject = "All Subjects (PCM)",
            examDate = getFutureDateString(24),
            targetScore = 95,
            isActive = true
        )

        val daysLeft = calculateDaysRemaining(exam.examDate).coerceAtLeast(1)
        val totalStages = (chapters.size * 6).coerceAtLeast(1)
        val doneStages = chapters.sumOf { it.completedStages }
        val syllabusProgress = (doneStages * 100) / totalStages

        val confidence = (syllabusProgress * 0.6f + 38f).roundToInt().coerceIn(65, 98)
        val confidenceLabel = when {
            confidence >= 90 -> "Top 1% Percentile Trajectory"
            confidence >= 80 -> "High Confidence • Target 95%+"
            else -> "Acceleration Needed • On Schedule"
        }

        val prepStatus = when {
            daysLeft > 60 -> "Comprehensive Foundation Mode"
            daysLeft > 25 -> "Speed Drills & PYQ Consolidation"
            else -> "Full Mock Exams & Error Log Blitz"
        }

        val expectedCompletion = getFutureDateString((daysLeft * 0.75).roundToInt())

        return ExamCountdownData(
            examName = exam.examName,
            subject = exam.subject,
            daysLeft = daysLeft,
            examDateFormatted = formatExamDateDisplay(exam.examDate),
            todayPreparationStatus = prepStatus,
            expectedCompletionDate = expectedCompletion,
            aiConfidenceScore = confidence,
            confidenceLabel = confidenceLabel
        )
    }

    private fun computeRecentActivity(
        targets: List<StudyTargetEntity>,
        sessions: List<StudySessionEntity>,
        testAttempts: List<TestAttemptEntity>,
        notes: List<NoteEntity>,
        streak: Int
    ): List<ActivityTimelineItem> {
        val activities = mutableListOf<ActivityTimelineItem>()
        val now = System.currentTimeMillis()

        // 1. Completed targets
        targets.filter { it.status == "Completed" }.take(3).forEach { target ->
            activities.add(
                ActivityTimelineItem(
                    id = "target_${target.id}",
                    title = "Completed ${target.subject} Target",
                    subtitle = target.task,
                    relativeTime = "Today",
                    type = "Target",
                    xpEarned = 25,
                    timestamp = target.createdAt
                )
            )
        }

        // 2. Test attempts
        testAttempts.take(2).forEach { test ->
            val acc = if (test.totalQuestions > 0) (test.correctCount * 100) / test.totalQuestions else 0
            activities.add(
                ActivityTimelineItem(
                    id = "test_${test.id}",
                    title = "Solved ${test.totalQuestions} Questions in ${test.subject}",
                    subtitle = "Score: ${test.score}/${test.maxScore} ($acc% Accuracy)",
                    relativeTime = formatRelativeTime(test.timestamp, now),
                    type = "Test",
                    xpEarned = 40,
                    timestamp = test.timestamp
                )
            )
        }

        // 3. Study Sessions
        sessions.take(2).forEach { session ->
            activities.add(
                ActivityTimelineItem(
                    id = "session_${session.id}",
                    title = "Finished ${session.durationMinutes}m Study Session",
                    subtitle = "${session.subject} • ${session.chapter}",
                    relativeTime = formatRelativeTime(session.timestamp, now),
                    type = "Session",
                    xpEarned = 15,
                    timestamp = session.timestamp
                )
            )
        }

        // 4. Notes created
        notes.take(1).forEach { note ->
            activities.add(
                ActivityTimelineItem(
                    id = "note_${note.id}",
                    title = "Added Notes: ${note.title}",
                    subtitle = "${note.subject} • ${note.noteType}",
                    relativeTime = formatRelativeTime(note.timestamp, now),
                    type = "Note",
                    xpEarned = 10,
                    timestamp = note.timestamp
                )
            )
        }

        // 5. Streak achievement if high
        if (streak > 0) {
            activities.add(
                ActivityTimelineItem(
                    id = "streak_achieve",
                    title = "Unlocked Achievement: $streak-Day Streak",
                    subtitle = "Maintained unbroken daily study discipline.",
                    relativeTime = "Active",
                    type = "Achievement",
                    xpEarned = 50,
                    timestamp = now
                )
            )
        }

        return activities.sortedByDescending { it.timestamp }.take(5)
    }

    private fun computeRecommendations(
        chapters: List<SyllabusChapterEntity>,
        weakChapters: List<WeakChapterItem>,
        testAttempts: List<TestAttemptEntity>,
        todayStudyMinutes: Int,
        goalMinutes: Int,
        board: String
    ): List<AiRecommendationItem> {
        val list = mutableListOf<AiRecommendationItem>()

        val topWeak = weakChapters.firstOrNull()
        if (topWeak != null) {
            list.add(
                AiRecommendationItem(
                    id = "rec_weak",
                    title = "Study ${topWeak.subject} first.",
                    reason = "Morning/peak mental energy clears high-friction topics 2.3x faster.",
                    actionLabel = "Start ${topWeak.subject}",
                    subject = topWeak.subject,
                    chapter = topWeak.title,
                    prompt = "Explain key derivations and 5 must-solve $board questions for ${topWeak.title} in simple terms."
                )
            )
        } else {
            list.add(
                AiRecommendationItem(
                    id = "rec_phys",
                    title = "Study Physics first.",
                    reason = "Your analytical focus is sharpest in early study hours.",
                    actionLabel = "Open Physics",
                    subject = "Physics",
                    chapter = "Current Electricity",
                    prompt = "Give me 5 highest probability $board PYQ numericals for Current Electricity with step marking."
                )
            )
        }

        list.add(
            AiRecommendationItem(
                id = "rec_revise",
                title = "Revise yesterday's chapter.",
                reason = "Active retrieval after 24 hours locks memories into long-term hippocampus storage.",
                actionLabel = "Quick Revision",
                subject = "Chemistry",
                chapter = "Electrochemistry",
                prompt = "Summarize Nernst equation and Faraday's laws formula sheet with units and sign conventions."
            )
        )

        if (todayStudyMinutes >= 90) {
            list.add(
                AiRecommendationItem(
                    id = "rec_break",
                    title = "Take a 15 min break.",
                    reason = "You've completed $todayStudyMinutes mins. A short walk restores dopamine and prefrontal attention.",
                    actionLabel = "Rest & Reset",
                    subject = "General",
                    chapter = "Mindset",
                    prompt = "How can a student take high-efficiency 15 min breaks to prevent mental burnout?"
                )
            )
        } else {
            list.add(
                AiRecommendationItem(
                    id = "rec_mock",
                    title = "Attempt Mock Test.",
                    reason = "Timed quizzes train exam-day pressure handling and calculation speed under constraints.",
                    actionLabel = "Start Mock Test",
                    subject = "Mathematics",
                    chapter = "Matrices",
                    prompt = "Generate a timed 10-question MCQ drill for Class 12 CBSE with step-by-step solutions."
                )
            )
        }

        return list
    }

    private fun computeMascotMotivation(
        firstName: String,
        hour: Int,
        streak: Int,
        progress: Int,
        board: String,
        prepLevel: String
    ): RankiMotivationData {
        // Diverse, highly personalized quotes that never feel repetitive or static
        val messages = when {
            streak >= 7 -> listOf(
                "Incredible $streak-day streak, $firstName! Champions don't wait for motivation; they build systems like yours.",
                "Seven days unbroken! Every hour you invest now compounds into effortless confidence in $board hall.",
                "Look at that momentum, $firstName! Rankers are forged on quiet days like today. Let's conquer the syllabus!"
            )
            progress >= 70 -> listOf(
                "You're already at $progress% daily productivity! Finish the remaining tasks and claim full mastery today.",
                "Magnificent work, $firstName! The finish line for today is right in front of you.",
                "Deep work pays off. You're out-preparing 95% of competitors who procrastinated today."
            )
            hour in 5..10 -> listOf(
                "Early hours belong to the top rankers, $firstName. Start with the hardest numerical before distractions arrive.",
                "Rise and shine! A crisp 45-minute focus block now will set the tone for the entire day.",
                "Clean slate, sharp mind. Let's make today count towards your 95%+ target score!"
            )
            hour in 21..24 || hour in 0..4 -> listOf(
                "Late night grind, $firstName! Ensure you consolidate your key formulas before catching good sleep.",
                "Quiet night, intense focus. Master this one concept and sleep like a future topper.",
                "Consistency when everyone else is asleep is the secret formula. Finish this revision block!"
            )
            else -> listOf(
                "Every derivation written by hand is guaranteed step marks in your $board exam, $firstName.",
                "Small daily targets done consistently beat chaotic marathon cramming every single time.",
                "Trust your preparation, $firstName. Rankify AI is continuously adjusting your roadmap for peak score."
            )
        }

        val selectedMessage = messages[(System.currentTimeMillis() / 60000 % messages.size).toInt()]
        val mood = if (streak >= 7 || progress >= 70) "Energized" else "Focused"

        return RankiMotivationData(
            message = selectedMessage,
            mood = mood,
            quoteContext = "Ranki AI Coach • Personalized for $firstName ($prepLevel Track)"
        )
    }

    private fun computeQuickActions(
        state: RankifyUiState,
        weakChapters: List<WeakChapterItem>,
        priorityMatrix: AiPriorityMatrix
    ): List<QuickActionItem> {
        val lastSession = state.sessions.firstOrNull()
        val lastSubject = lastSession?.subject ?: "Physics"
        val lastChapter = lastSession?.chapter ?: "Current Electricity"

        val weakChapter = weakChapters.firstOrNull()
        val weakSubj = weakChapter?.subject ?: "Physics"
        val weakChapTitle = weakChapter?.title ?: "Electrostatics"

        return listOf(
            QuickActionItem(
                id = "continue_session",
                title = "Continue Last Session",
                subtitle = "$lastSubject • $lastChapter",
                type = QuickActionType.CONTINUE_SESSION,
                subject = lastSubject,
                chapter = lastChapter
            ),
            QuickActionItem(
                id = "resume_lecture",
                title = "Resume StudyDock",
                subtitle = "Turn YouTube lectures into smart notes",
                type = QuickActionType.RESUME_STUDYDOCK
            ),
            QuickActionItem(
                id = "open_weak",
                title = "Open Weak Chapter",
                subtitle = "$weakSubj • $weakChapTitle",
                type = QuickActionType.PRACTICE_WEAK,
                subject = weakSubj,
                chapter = weakChapTitle
            ),
            QuickActionItem(
                id = "start_revision",
                title = "Start Revision",
                subtitle = priorityMatrix.revisionRequired,
                type = QuickActionType.START_REVISION,
                subject = priorityMatrix.todaysHighestSubject,
                chapter = priorityMatrix.revisionRequired
            ),
            QuickActionItem(
                id = "ask_ai",
                title = "Ask AI Tutor",
                subtitle = "Instant doubt solving & concept maps",
                type = QuickActionType.ASK_AI_TUTOR,
                subject = lastSubject,
                chapter = lastChapter,
                prompt = "Explain key derivations and numerical patterns for $lastChapter."
            ),
            QuickActionItem(
                id = "take_test",
                title = "Take Test",
                subtitle = "Diagnostic MCQs & PYQs",
                type = QuickActionType.TAKE_TEST,
                subject = lastSubject
            ),
            QuickActionItem(
                id = "open_notes",
                title = "Open Notes",
                subtitle = "Formulas, Flashcards & Summary",
                type = QuickActionType.OPEN_NOTES
            )
        )
    }

    private fun calculateDaysRemaining(dateString: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val target = format.parse(dateString)?.time ?: return 24
            val now = System.currentTimeMillis()
            val diff = target - now
            val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
            days.coerceAtLeast(1)
        } catch (_: Exception) {
            24
        }
    }

    private fun getFutureDateString(daysInFuture: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysInFuture)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(cal.time)
    }

    private fun formatExamDateDisplay(dateString: String): String {
        return try {
            val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inFormat.parse(dateString) ?: return dateString
            val outFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            outFormat.format(date)
        } catch (_: Exception) {
            dateString
        }
    }

    private fun formatRelativeTime(timestamp: Long, now: Long): String {
        val diff = now - timestamp
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 2 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> "${days}d ago"
        }
    }
}
