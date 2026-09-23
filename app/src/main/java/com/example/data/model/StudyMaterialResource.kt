package com.example.data.model

/**
 * Metadata model for study materials stored in Firestore under collection `study_materials/{resourceId}`.
 */
data class StudyMaterialResource(
    val resourceId: String = "",
    val title: String = "",
    val description: String = "",
    val classLevel: String = "12",
    val board: String = "CBSE",
    val stream: String = "PCM",
    val subject: String = "Physics",
    val chapterId: String = "",
    val chapterName: String = "",
    val materialType: String = "Important Questions",
    val difficultyLevel: String = "Moderate",
    val pdfUrl: String = "",
    val googleDriveFileId: String? = null,
    val youtubeVideoId: String? = null,
    val published: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
