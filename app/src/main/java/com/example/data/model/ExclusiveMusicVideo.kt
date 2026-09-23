package com.example.data.model

import com.google.firebase.Timestamp

data class ExclusiveMusicVideo(
    val videoId: String = "",
    val title: String = "",
    val subject: String = "",
    val chapterId: String = "",
    val chapterName: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val coinPrice: Int = 0,
    val durationSeconds: Int = 0,
    val published: Boolean = false,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class ExclusiveMusicVideoSource(
    val videoId: String = "",
    val videoUrl: String = "",
    val googleDriveFileId: String = "",
    val updatedAt: Timestamp? = null
)
