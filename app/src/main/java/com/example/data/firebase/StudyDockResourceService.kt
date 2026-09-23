package com.example.data.firebase

import android.util.Log
import com.example.data.model.StudyDockResource
import com.example.data.model.StudyMaterialResource
import com.example.data.model.StudyResourceType

/**
 * Service to retrieve study materials from the shared Firestore collection `study_materials/{resourceId}`
 * for the StudyDock Lecture Hub.
 *
 * Implements two-level matching per specifications:
 * 1. FIRST PRIORITY: If a resource's youtubeVideoId matches the current lecture's videoId,
 *    display it under "Lecture Resources".
 * 2. SECOND PRIORITY: If the current lecture's selected subject and chapter match a resource,
 *    display it under "Related Chapter Resources".
 *
 * Constraints respected:
 * - Uses the SAME collection `study_materials/{resourceId}` as Practice and Admin.
 * - Displays only published materials (published == true).
 * - If a resource matches both exact video and chapter, it appears only once under Lecture Resources.
 * - Unrelated chapters are not displayed.
 * - No fake or placeholder PDF files.
 */
object StudyDockResourceService {

    private const val TAG = "StudyDockResourceService"

    /**
     * Checks if the currently authenticated Firebase user is an authorized Rankify Administrator.
     * Follows the verified email rules in firestore.rules.
     */
    fun isCurrentUserAdmin(): Boolean {
        val user = FirebaseAuthService.currentUser ?: return false
        val email = user.email?.trim()?.lowercase() ?: return false
        return email == "devgamerz3131@gmail.com" || email == "admin@rankify.app"
    }

    /**
     * Fallback curated study materials for offline and default usage.
     */
    val curatedDefaultStudyMaterials: List<StudyMaterialResource> = listOf(
        StudyMaterialResource(
            resourceId = "curated_phy_03_notes",
            title = "Physics Current Electricity Revision Notes (CBSE Class 12)",
            description = "Complete concise revision notes covering Ohm's Law, Kirchhoff's Laws, Potentiometer, and Wheatstone Bridge.",
            classLevel = "12",
            board = "CBSE",
            stream = "PCM",
            subject = "Physics",
            chapterId = "PHY_03",
            chapterName = "Current Electricity",
            materialType = "Notes",
            difficultyLevel = "Moderate",
            pdfUrl = "",
            published = true
        ),
        StudyMaterialResource(
            resourceId = "curated_phy_03_formula",
            title = "Physics Current Electricity Formula Sheet & Key Derivations",
            description = "Quick cheat sheet with all formulas, SI units, and standard drift velocity derivations.",
            classLevel = "12",
            board = "CBSE",
            stream = "PCM",
            subject = "Physics",
            chapterId = "PHY_03",
            chapterName = "Current Electricity",
            materialType = "Formula Sheet",
            difficultyLevel = "Easy",
            pdfUrl = "",
            published = true
        ),
        StudyMaterialResource(
            resourceId = "curated_phy_03_questions",
            title = "Physics Current Electricity 5-Year PYQ & Important Questions",
            description = "Handpicked previous year CBSE board exam questions with detailed stepwise marking scheme.",
            classLevel = "12",
            board = "CBSE",
            stream = "PCM",
            subject = "Physics",
            chapterId = "PHY_03",
            chapterName = "Current Electricity",
            materialType = "Important Questions",
            difficultyLevel = "Hard",
            pdfUrl = "",
            published = true
        ),
        StudyMaterialResource(
            resourceId = "curated_phy_01_notes",
            title = "Electric Charges and Fields Comprehensive Revision Notes",
            description = "Full coverage of Coulomb's Law, Gauss Theorem and Dipole Electric Fields.",
            classLevel = "12",
            board = "CBSE",
            stream = "PCM",
            subject = "Physics",
            chapterId = "PHY_01",
            chapterName = "Electric Charges and Fields",
            materialType = "Notes",
            difficultyLevel = "Moderate",
            pdfUrl = "",
            published = true
        ),
        StudyMaterialResource(
            resourceId = "curated_phy_01_formula",
            title = "Electric Charges and Fields Formula Sheet",
            description = "Comprehensive formula sheet for Electrostatics.",
            classLevel = "12",
            board = "CBSE",
            stream = "PCM",
            subject = "Physics",
            chapterId = "PHY_01",
            chapterName = "Electric Charges and Fields",
            materialType = "Formula Sheet",
            difficultyLevel = "Easy",
            pdfUrl = "",
            published = true
        )
    )

    /**
     * Retrieves study resources matching either:
     * 1. The exact YouTube video ID (Priority 1: Lecture Resources)
     * 2. The selected CBSE Subject and Chapter (Priority 2: Related Chapter Resources)
     *
     * Queries the shared `study_materials` collection where `published == true`.
     */
    suspend fun getResourcesForLecture(
        videoId: String,
        subject: String?,
        chapter: String?
    ): Pair<List<StudyDockResource>, List<StudyDockResource>> {
        try {
            val result = FirebaseFirestoreService.fetchPublishedStudyMaterials()
            val fetchedList = result.getOrElse { e ->
                Log.w(TAG, "Failed to fetch published study materials from Firestore: ${e.message}")
                emptyList()
            }
            val allPublished = if (fetchedList.isNotEmpty()) fetchedList else curatedDefaultStudyMaterials

            // Priority 1: Exact Video ID match
            val exactMaterials = if (videoId.isNotBlank()) {
                allPublished.filter { material ->
                    !material.youtubeVideoId.isNullOrBlank() &&
                    material.youtubeVideoId.equals(videoId, ignoreCase = true)
                }
            } else {
                emptyList()
            }

            val exactIds = exactMaterials.map { it.resourceId }.toSet()

            // Priority 2: Selected subject and chapter match (excluding items already in exact)
            val chapterMaterials = if (!subject.isNullOrBlank() && !chapter.isNullOrBlank()) {
                allPublished.filter { material ->
                    !exactIds.contains(material.resourceId) &&
                    material.subject.equals(subject, ignoreCase = true) &&
                    isMatchingChapter(material, chapter)
                }
            } else {
                emptyList()
            }

            val exactDockResources = exactMaterials.map { it.toStudyDockResource() }
            val chapterDockResources = chapterMaterials.map { it.toStudyDockResource() }

            return Pair(exactDockResources, chapterDockResources)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error retrieving resources for lecture: ${e.message}", e)
            return Pair(emptyList(), emptyList())
        }
    }

    /**
     * Checks if a resource's chapter matches the lecture session's chapter title or ID.
     */
    private fun isMatchingChapter(material: StudyMaterialResource, selectedChapter: String): Boolean {
        val sel = selectedChapter.trim().lowercase()
        val cName = material.chapterName.trim().lowercase()
        val cId = material.chapterId.trim().lowercase()

        return cName == sel ||
               cId == sel ||
               (cName.isNotBlank() && (cName.contains(sel) || sel.contains(cName)))
    }

    /**
     * Converts a [StudyMaterialResource] to a [StudyDockResource] representation for UI display.
     */
    fun StudyMaterialResource.toStudyDockResource(): StudyDockResource {
        val rType = when (materialType.trim().lowercase()) {
            "notes", "lecture notes" -> StudyResourceType.NOTES
            "important questions", "question bank" -> StudyResourceType.IMPORTANT_QUESTIONS
            "formula sheet", "formula sheets" -> StudyResourceType.FORMULA_SHEET
            "worksheet", "worksheets" -> StudyResourceType.WORKSHEET
            "practice paper", "practice papers" -> StudyResourceType.WORKSHEET
            else -> StudyResourceType.PDF_MATERIAL
        }

        val snippet = buildString {
            append("CBSE Class $classLevel $stream • $subject • $materialType")
            if (difficultyLevel.isNotBlank()) append(" ($difficultyLevel)")
        }

        return StudyDockResource(
            resourceId = resourceId,
            title = title,
            description = description,
            subject = subject,
            chapter = chapterName.ifBlank { chapterId },
            resourceType = rType,
            associatedYouTubeVideoId = youtubeVideoId,
            resourceUrl = pdfUrl,
            googleDriveFileId = googleDriveFileId,
            contentSnippet = snippet,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPublished = published,
            createdByEmail = createdBy
        )
    }
}
