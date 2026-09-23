package com.example.data.model

import java.util.UUID

/**
 * Supported academic resource types for StudyDock.
 */
enum class StudyResourceType {
    NOTES,
    IMPORTANT_QUESTIONS,
    FORMULA_SHEET,
    WORKSHEET,
    PDF_MATERIAL,
    IMAGE,
    STUDY_LINK;

    fun displayName(): String = when (this) {
        NOTES -> "Lecture Notes"
        IMPORTANT_QUESTIONS -> "Important Questions"
        FORMULA_SHEET -> "Formula Sheet"
        WORKSHEET -> "Practice Worksheet"
        PDF_MATERIAL -> "Study Material (PDF)"
        IMAGE -> "Diagram / Mindmap"
        STUDY_LINK -> "Reference Link"
    }
}

/**
 * Metadata representation of an administrator-managed study resource.
 *
 * Supports two-level matching:
 * 1. Exact lecture resources (when [associatedYouTubeVideoId] matches the current lecture's videoId).
 * 2. Related chapter resources (when [subject] and [chapter] match the selected CBSE Class 12 topic).
 *
 * Cost safety:
 * No large binaries are stored in Firestore. Resources use external authorized links (PDFs, Google Docs/Drive
 * view links, official CBSE study links) or structured textual summaries.
 */
data class StudyDockResource(
    val resourceId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val chapter: String = "",
    val resourceType: StudyResourceType = StudyResourceType.NOTES,
    val associatedYouTubeVideoId: String? = null,
    val resourceUrl: String = "",
    val googleDriveFileId: String? = null,
    val contentSnippet: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = true,
    val createdByEmail: String? = null
)
