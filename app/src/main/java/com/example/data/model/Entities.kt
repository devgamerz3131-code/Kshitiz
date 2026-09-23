package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val studentClass: String = "Class 12",
    val board: String = "CBSE",
    val stream: String = "PCM",
    val subjects: String = "Physics, Chemistry, Mathematics",
    val targetPercentage: Int = 95,
    val dailyStudyGoalMinutes: Int = 180,
    val languagePreference: String = "Hinglish",
    val currentPrepLevel: String = "Intermediate",
    val streakDays: Int = 0,
    val lastStudyDate: String = "",
    val notificationIntensity: String = "Recommended",
    val isOnboarded: Boolean = true,
    val email: String = "",
    val firebaseUid: String = "",
    val hasUpcomingExam: Boolean = false,
    val examName: String = "",
    val examDate: String = "",
    val isCloudSynced: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val createdAtTimestamp: Long = 0L,
    val studyPlanConfigured: Boolean = false,
    val studyLevel: String = "",
    val studyPlanSummary: String = ""
)

sealed class SyncStatus {
    object Synced : SyncStatus() {
        val label: String = "All changes synced"
    }
    object Syncing : SyncStatus() {
        val label: String = "Syncing..."
    }
    object Offline : SyncStatus() {
        val label: String = "Offline (changes will sync automatically)"
    }
}

data class StudyPlanLevel(
    val id: String,
    val title: String,
    val subtitle: String,
    val recommendedGoalMinutes: Int,
    val recommendedTargetPercent: Int,
    val description: String,
    val keyFocus: String,
    val subjectPriorities: List<String>,
    val roadmapStages: List<String>
) {
    companion object {
        val LEVELS = listOf(
            StudyPlanLevel(
                id = "concept_foundation",
                title = "Concept Foundation",
                subtitle = "CBSE 12 Board Focus (Target 90%+)",
                recommendedGoalMinutes = 150,
                recommendedTargetPercent = 90,
                description = "Master core concepts with in-depth NCERT coverage, structured derivations, and regular formula checkpoints.",
                keyFocus = "NCERT Line-by-Line • Derivations • In-text & Back Exercises",
                subjectPriorities = listOf("Physics: Theory & Derivations", "Chemistry: NCERT Named Reactions", "Mathematics: NCERT Exercises"),
                roadmapStages = listOf(
                    "Stage 1: NCERT Comprehensive Concept Mapping",
                    "Stage 2: Core Derivations & Board Model Questions",
                    "Stage 3: 5-Year Board PYQ Mastery"
                )
            ),
            StudyPlanLevel(
                id = "dual_mastery",
                title = "Dual-Target Mastery",
                subtitle = "CBSE Boards + JEE/NEET (Target 95%+)",
                recommendedGoalMinutes = 210,
                recommendedTargetPercent = 95,
                description = "Balanced high-performance track combining Board exam presentation clarity with competitive speed problem-solving.",
                keyFocus = "Core Derivations • Multi-concept PYQs (2020-2025) • Timed Speed Drills",
                subjectPriorities = listOf("Physics: Electrodynamics & Optics", "Chemistry: Physical & Organic Mechanisms", "Mathematics: Calculus & Algebra"),
                roadmapStages = listOf(
                    "Stage 1: Board Step-Marking & High-Yield Derivations",
                    "Stage 2: JEE/NEET Mains Problem-Solving Drills",
                    "Stage 3: Full-Syllabus Timed Tests & Error Log Review"
                )
            ),
            StudyPlanLevel(
                id = "rank_booster",
                title = "Rank Booster",
                subtitle = "Top 1% Percentile (Advanced Focus)",
                recommendedGoalMinutes = 270,
                recommendedTargetPercent = 98,
                description = "Intensive problem-solving curriculum focused on high-difficulty questions, mock tests, and weak-area eradication.",
                keyFocus = "Advanced Multi-step Problems • Speed Elimination • Mistake Eradication",
                subjectPriorities = listOf("Physics: Advanced Mechanics & Modern Physics", "Chemistry: Complex Organic Syntheses", "Mathematics: Definite Integrals & 3D"),
                roadmapStages = listOf(
                    "Stage 1: High-Weightage Concept Blitz & Advanced Problem Sets",
                    "Stage 2: Competitive Mock Exams under strict time constraints",
                    "Stage 3: Error Analysis & Speed Accuracy Fine-Tuning"
                )
            )
        )
    }
}

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String,
    val subject: String,
    val examDate: String, // e.g. "2026-10-20"
    val targetScore: Int = 95,
    val syllabusCoverage: String = "Full Syllabus",
    val isActive: Boolean = true
)

@Entity(tableName = "syllabus_chapters")
data class SyllabusChapterEntity(
    @PrimaryKey val id: String, // e.g. "PHY_01"
    val subject: String, // "Physics", "Chemistry", "Mathematics"
    val chapterNumber: Int,
    val title: String,
    val conceptsDone: Boolean = false,
    val ncertReadingDone: Boolean = false,
    val ncertQuestionsDone: Boolean = false,
    val pyqDone: Boolean = false,
    val revisionDone: Boolean = false,
    val testDone: Boolean = false,
    val lastRevisedTimestamp: Long = 0L,
    val isWeakTopic: Boolean = false
) {
    val totalStages: Int get() = 6
    val completedStages: Int
        get() {
            var count = 0
            if (conceptsDone) count++
            if (ncertReadingDone) count++
            if (ncertQuestionsDone) count++
            if (pyqDone) count++
            if (revisionDone) count++
            if (testDone) count++
            return count
        }
    val completionPercentage: Int
        get() = (completedStages * 100) / totalStages
}

@Entity(tableName = "study_targets")
data class StudyTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val chapter: String,
    val task: String,
    val priority: String = "High", // "High", "Medium", "Low"
    val estimatedMinutes: Int = 45,
    val deadlineDate: String = "",
    val status: String = "Pending", // "Pending", "In Progress", "Completed", "Overdue"
    val isBacklog: Boolean = false,
    val isWeeklyGoal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val chapter: String,
    val durationMinutes: Int,
    val difficultyRating: String = "Medium", // "Easy", "Medium", "Hard"
    val goalCompleted: Boolean = true,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val chapter: String,
    val topic: String,
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val questionType: String = "MCQ", // "MCQ", "Numerical", "Board PYQ", "Competency"
    val questionText: String,
    val optionsListJson: String = "", // Comma-delimited or pipe-delimited options
    val correctAnswer: String,
    val hint: String,
    val solutionExplanation: String,
    val isBookmarked: Boolean = false,
    val isImportant: Boolean = false
)

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long = 0,
    val subject: String,
    val chapter: String,
    val topic: String,
    val questionSummary: String,
    val mistakeCategory: String, // "Concept error", "Formula error", "Calculation error", "Careless mistake", "Didn't know", "Time pressure"
    val studentNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "test_attempts")
data class TestAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testTitle: String,
    val subject: String,
    val chapter: String = "Mixed Chapters",
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val score: Int,
    val maxScore: Int,
    val timeTakenMinutes: Int,
    val aiAnalysisFeedback: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val chapter: String,
    val title: String,
    val content: String,
    val noteType: String = "Personal", // "Personal", "AI Summary", "Formula Sheet", "Flashcards", "Solution"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_chat_messages")
data class AIChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "assistant"
    val message: String,
    val subjectContext: String = "Physics",
    val chapterContext: String = "Current Electricity",
    val isVisualExplanation: Boolean = false,
    val studyMode: String = "Doubt Mode",
    val structuredDataJson: String? = null, // JSON string of AiTutorResponse
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "Study Reminder",
    val personality: String = "Mixed",
    val deepLink: String = "today_target",
    val createdTime: Long = System.currentTimeMillis(),
    val opened: Boolean = false,
    val dismissed: Boolean = false,
    val isSynced: Boolean = false
)

