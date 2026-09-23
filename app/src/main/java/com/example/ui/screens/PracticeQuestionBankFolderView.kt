package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DefaultData
import com.example.data.model.StudyMaterialResource
import com.example.data.model.SyllabusChapterEntity
import com.example.ui.theme.RankifyPrimary

/**
 * Practice Subject-Wise Question Bank Folder System
 *
 * Implements the requested hierarchical folder navigation:
 * Level 0: Subject Folders (Physics, Chemistry, Mathematics)
 * Level 1: Selected Subject -> Question Bank -> Chapter-wise question banks
 * Level 2: Selected Chapter -> Actual published Question Bank PDF resources
 *
 * Filtering rules:
 * - Published resources only (`published == true`).
 * - Matching subject.
 * - Matching chapter.
 * - Material type: "Question Bank", "Important Questions", or question-bank category.
 * - Does not mix unrelated notes or formula sheets.
 * - Uses existing Rankify chapter identifiers and syllabus metadata.
 * - Opens PDFs with existing secure viewer intent.
 */
@Composable
fun PracticeQuestionBankFolderView(
    publishedMaterials: List<StudyMaterialResource>,
    chapters: List<SyllabusChapterEntity>,
    viewModel: com.example.viewmodel.RankifyViewModel,
    onCloseFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<SyllabusChapterEntity?>(null) }
    var activePdfParams by remember { mutableStateOf<PdfReaderParams?>(null) }

    // Display native in-app PDF Reader when a PDF is selected
    if (activePdfParams != null) {
        RankifyPdfReaderScreen(
            params = activePdfParams!!,
            onClose = { activePdfParams = null }
        )
        return
    }

    // Intercept system back button to step backwards through folder levels
    BackHandler {
        when {
            selectedChapter != null -> selectedChapter = null
            selectedSubject != null -> selectedSubject = null
            else -> onCloseFolder()
        }
    }

    val allChapters = remember(chapters) {
        if (chapters.isNotEmpty()) chapters else com.example.data.db.DefaultData.initialChapters
    }

    // Filter question bank resources: only published + question bank / important questions
    val questionBankMaterials = remember(publishedMaterials) {
        publishedMaterials.filter { res ->
            res.published && isQuestionBankMaterialType(res.materialType)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("practice_question_bank_folder_view")
    ) {
        val currentChapter = selectedChapter
        val currentSubject = selectedSubject

        when {
            // Level 2: Chapter selected -> display actual published Question Bank PDFs
            currentChapter != null && currentSubject != null -> {
                ChapterQuestionBankList(
                    subject = currentSubject,
                    chapter = currentChapter,
                    materials = questionBankMaterials.filter { res ->
                        res.subject.equals(currentSubject, ignoreCase = true) &&
                        isMatchingChapter(res, currentChapter)
                    },
                    onBack = { selectedChapter = null },
                    onOpenPdf = { res ->
                        // Automatically record PDF read progress for this chapter
                        viewModel.recordChapterPdfRead(res.subject, res.chapterName)
                        
                        activePdfParams = PdfReaderParams(
                            resourceId = res.resourceId,
                            title = res.title,
                            pdfUrl = res.pdfUrl,
                            googleDriveFileId = res.googleDriveFileId,
                            subject = res.subject,
                            chapter = res.chapterName
                        )
                    }
                )
            }

            // Level 1: Subject selected -> display Chapter-wise question banks
            currentSubject != null -> {
                SubjectChapterFolderList(
                    subject = currentSubject,
                    chapters = allChapters.filter { it.subject.equals(currentSubject, ignoreCase = true) },
                    allMaterials = questionBankMaterials.filter { it.subject.equals(currentSubject, ignoreCase = true) },
                    onSelectChapter = { chapter -> selectedChapter = chapter },
                    onBack = { selectedSubject = null }
                )
            }

            // Level 0: Subject folders (Physics, Chemistry, Mathematics)
            else -> {
                SubjectFolderGrid(
                    allChapters = allChapters,
                    allMaterials = questionBankMaterials,
                    onSelectSubject = { subj -> selectedSubject = subj },
                    onBack = onCloseFolder
                )
            }
        }
    }
}

/**
 * Level 0: Subject Folders view.
 */
@Composable
private fun SubjectFolderGrid(
    allChapters: List<SyllabusChapterEntity>,
    allMaterials: List<StudyMaterialResource>,
    onSelectSubject: (String) -> Unit,
    onBack: () -> Unit
) {
    val subjects = listOf(
        SubjectFolderInfo(
            name = "Physics",
            color = Color(0xFF1D4ED8),
            bgColor = Color(0xFFEFF6FF),
            borderColor = Color(0xFFBFDBFE),
            tag = "subject_folder_physics"
        ),
        SubjectFolderInfo(
            name = "Chemistry",
            color = Color(0xFF0F766E),
            bgColor = Color(0xFFF0FDFA),
            borderColor = Color(0xFF99F6E4),
            tag = "subject_folder_chemistry"
        ),
        SubjectFolderInfo(
            name = "Mathematics",
            color = Color(0xFF6D28D9),
            bgColor = Color(0xFFF5F3FF),
            borderColor = Color(0xFFDDD6FE),
            tag = "subject_folder_mathematics"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("qb_folder_back_to_practice")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Practice"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Question Bank",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Class 12 CBSE Subject Folders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Subject Folders Cards
        subjects.forEach { subj ->
            val subjChapters = allChapters.filter { it.subject.equals(subj.name, ignoreCase = true) }
            val publishedQbCount = allMaterials.count { it.subject.equals(subj.name, ignoreCase = true) }

            OutlinedCard(
                onClick = { onSelectSubject(subj.name) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = subj.bgColor),
                border = BorderStroke(1.5.dp, subj.borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(subj.tag)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(subj.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = subj.name,
                                tint = subj.color,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = subj.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${subjChapters.size} Chapters • Chapter-wise question banks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (publishedQbCount > 0) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (publishedQbCount > 0) "$publishedQbCount Published QB Available" else "Syllabus Chapters Ready",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (publishedQbCount > 0) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open ${subj.name}",
                        tint = subj.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Level 1: Chapter-wise Question Banks under selected subject.
 */
@Composable
private fun SubjectChapterFolderList(
    subject: String,
    chapters: List<SyllabusChapterEntity>,
    allMaterials: List<StudyMaterialResource>,
    onSelectChapter: (SyllabusChapterEntity) -> Unit,
    onBack: () -> Unit
) {
    val subjectColor = when (subject.lowercase()) {
        "physics" -> Color(0xFF1D4ED8)
        "chemistry" -> Color(0xFF0F766E)
        "mathematics" -> Color(0xFF6D28D9)
        else -> RankifyPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Breadcrumbs & Back Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("qb_folder_back_to_subjects")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Subjects"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = subjectColor
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Question Bank",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Chapter-wise question banks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chapters List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chapters, key = { it.id }) { chapter ->
                val qbCountForChapter = allMaterials.count { mat ->
                    isMatchingChapter(mat, chapter)
                }

                OutlinedCard(
                    onClick = { onSelectChapter(chapter) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chapter_folder_${chapter.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(subjectColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = subjectColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Chapter ${chapter.chapterNumber} • ${chapter.completedStages}/${chapter.totalStages} Stages",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (qbCountForChapter > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = "$qbCountForChapter PDF Available",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Chapter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Level 2: Display published Question Bank resources for the selected chapter.
 */
@Composable
private fun ChapterQuestionBankList(
    subject: String,
    chapter: SyllabusChapterEntity,
    materials: List<StudyMaterialResource>,
    onBack: () -> Unit,
    onOpenPdf: (StudyMaterialResource) -> Unit
) {
    val subjectColor = when (subject.lowercase()) {
        "physics" -> Color(0xFF1D4ED8)
        "chemistry" -> Color(0xFF0F766E)
        "mathematics" -> Color(0xFF6D28D9)
        else -> RankifyPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Breadcrumb & Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("qb_folder_back_to_chapters")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Chapters"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = subjectColor
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Question Bank",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (materials.isEmpty()) {
            // Clean Empty State — Never displays fake placeholder PDF files
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_chapter_qb_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Question Bank published yet",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The administrator has not yet published a Question Bank PDF for ${chapter.title}. Check back soon!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "Published Question Banks (${materials.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(materials, key = { it.resourceId }) { resource ->
                    QuestionBankResourceCard(
                        resource = resource,
                        subjectColor = subjectColor,
                        onOpenPdf = { onOpenPdf(resource) }
                    )
                }
            }
        }
    }
}

/**
 * Card representing a published Question Bank resource.
 */
@Composable
private fun QuestionBankResourceCard(
    resource: StudyMaterialResource,
    subjectColor: Color,
    onOpenPdf: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qb_resource_item_${resource.resourceId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = subjectColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = resource.subject,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = subjectColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = resource.materialType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = resource.difficultyLevel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Text(
                        text = "PUBLISHED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Title
            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Description if available
            if (resource.description.isNotBlank()) {
                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Action: Open PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Document",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Google Drive PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenPdf,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                    modifier = Modifier.testTag("open_pdf_${resource.resourceId}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open PDF",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open PDF",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * Matches a study material's chapter with a syllabus chapter.
 */
private fun isMatchingChapter(material: StudyMaterialResource, chapter: SyllabusChapterEntity): Boolean {
    val mName = material.chapterName.trim().lowercase()
    val mId = material.chapterId.trim().lowercase()
    val cTitle = chapter.title.trim().lowercase()
    val cId = chapter.id.trim().lowercase()

    return mName == cTitle ||
           mId == cId ||
           (mName.isNotBlank() && (mName.contains(cTitle) || cTitle.contains(mName))) ||
           (mId.isNotBlank() && (mId.contains(cId) || cId.contains(mId)))
}

/**
 * Checks if a materialType belongs to the Question Bank category.
 */
private fun isQuestionBankMaterialType(materialType: String): Boolean {
    val norm = materialType.trim().lowercase()
    return norm.contains("question") ||
           norm.contains("qb") ||
           norm.contains("practice paper") ||
           norm.contains("worksheet")
}

private data class SubjectFolderInfo(
    val name: String,
    val color: Color,
    val bgColor: Color,
    val borderColor: Color,
    val tag: String
)
