package com.example.data.model

import java.util.UUID

/**
 * Lifecycle states for StudyDock lecture session analysis.
 * In Step 4, no AI analysis occurs. A session is READY when a valid YouTube link is confirmed.
 */
enum class StudyDockAnalysisStatus {
    IDLE,
    VALIDATING,
    READY,
    ANALYZING,
    COMPLETED,
    FAILED
}

/**
 * Future study content generated from AI video analysis.
 * In Step 4, all fields remain null or empty until Step 5 analysis is implemented.
 */
data class FutureStudyContent(
    val shortSummary: String? = null,
    val keyConcepts: List<String> = emptyList(),
    val importantFormulas: List<String> = emptyList(),
    val topicTimeline: List<String> = emptyList(),
    val importantPoints: List<String> = emptyList()
)

/**
 * Internal session model representing one StudyDock lecture session.
 * Organizes video identity, student-selected academic context, analysis status,
 * and future study content.
 */
data class StudyDockLectureSession(
    // Video Identity
    val sessionId: String = UUID.randomUUID().toString(),
    val originalYouTubeUrl: String,
    val normalizedYouTubeUrl: String,
    val videoId: String,

    // Lecture Metadata (nullable/empty initially; determined in future steps)
    val lectureTitle: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long? = null,
    val detectedSubject: String? = null,
    val detectedChapter: String? = null,

    // Student-Selected Academic Context (strictly separate from detected values)
    val selectedSubject: String? = null,
    val selectedChapter: String? = null,

    // Session Information
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val analysisStatus: StudyDockAnalysisStatus = StudyDockAnalysisStatus.READY,

    // Future Study Content placeholder
    val futureContent: FutureStudyContent? = null
)
