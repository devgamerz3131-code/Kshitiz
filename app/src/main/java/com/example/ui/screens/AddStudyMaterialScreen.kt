package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.db.DefaultData
import com.example.data.model.AdminAccessState
import com.example.data.model.SyllabusChapterEntity
import com.example.ui.theme.RankifyPrimary
import com.example.util.GoogleDriveUrlHelper
import com.example.viewmodel.RankifyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudyMaterialScreen(
    viewModel: RankifyViewModel,
    adminAccessState: AdminAccessState,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    // Route-level security: Deny access if not verified admin
    if (adminAccessState !is AdminAccessState.Admin) {
        AdminAccessDeniedScreen(
            adminAccessState = adminAccessState,
            onNavigateBack = onNavigateBack
        )
        return
    }

    val isSaving by viewModel.isSavingStudyMaterial.collectAsState()
    val successMsg by viewModel.studyMaterialSaveSuccess.collectAsState()
    val errorMsg by viewModel.studyMaterialSaveError.collectAsState()

    // Form fields state (preserved on errors)
    var title by remember { mutableStateOf("") }
    val classLevel = "12"
    val board = "CBSE"
    val stream = "PCM"

    val subjectOptions = listOf("Physics", "Chemistry", "Mathematics")
    var selectedSubject by remember { mutableStateOf("Physics") }

    val materialTypeOptions = listOf(
        "Question Bank",
        "Important Questions",
        "Notes",
        "Formula Sheet",
        "Worksheet",
        "Practice Paper",
        "Other"
    )
    var selectedMaterialType by remember { mutableStateOf("Question Bank") }

    val difficultyOptions = listOf("Easy", "Moderate", "Hard", "Mixed")
    var selectedDifficulty by remember { mutableStateOf("Moderate") }

    // Dynamic chapters loaded from existing Rankify syllabus
    val allChapters = remember { DefaultData.initialChapters }
    val availableChapters = remember(selectedSubject, allChapters) {
        allChapters.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }

    var selectedChapter by remember {
        mutableStateOf<SyllabusChapterEntity?>(availableChapters.firstOrNull())
    }

    // Auto-update selected chapter when subject changes
    LaunchedEffect(selectedSubject) {
        val newChapters = allChapters.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
        selectedChapter = newChapters.firstOrNull()
    }

    var description by remember { mutableStateOf("") }
    var pdfUrl by remember { mutableStateOf("") }
    var youtubeVideoId by remember { mutableStateOf("") }
    var isPublished by remember { mutableStateOf(false) }

    // Local validation feedback
    var titleError by remember { mutableStateOf<String?>(null) }
    var pdfUrlError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("add_study_material_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Study Material",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_material_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Study Material Manager"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Notification Card
            if (successMsg != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_material_success_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = successMsg ?: "Study material saved successfully.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF14532D),
                            modifier = Modifier.testTag("save_material_success_text")
                        )
                    }
                }
            }

            // Error Notification Card
            if (errorMsg != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_material_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMsg ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7F1D1D),
                            modifier = Modifier.testTag("save_material_error_text")
                        )
                    }
                }
            }

            // Target Context Summary (Class 12 CBSE PCM)
            OutlinedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target Curriculum",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Class $classLevel • $board • $stream",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RankifyPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Curated Repository",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = RankifyPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 1. Resource Title (Required)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Resource Title *",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = null
                    },
                    placeholder = { Text("e.g. Electric Charges and Fields — Important Questions") },
                    isError = titleError != null,
                    supportingText = {
                        if (titleError != null) {
                            Text(titleError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Clear and descriptive title for students.")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resource_title_input")
                )
            }

            // 2. Subject Dropdown (Required)
            var subjectExpanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Subject *",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("subject_dropdown_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        subjectOptions.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject) },
                                onClick = {
                                    selectedSubject = subject
                                    subjectExpanded = false
                                },
                                modifier = Modifier.testTag("subject_option_$subject")
                            )
                        }
                    }
                }
            }

            // 3. Chapter Dropdown (Required - from Rankify Syllabus)
            var chapterExpanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Chapter (from Rankify Syllabus) *",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                val chapterDisplayText = selectedChapter?.let {
                    "Ch ${it.chapterNumber}: ${it.title}"
                } ?: "Select Chapter"

                ExposedDropdownMenuBox(
                    expanded = chapterExpanded,
                    onExpandedChange = { chapterExpanded = !chapterExpanded }
                ) {
                    OutlinedTextField(
                        value = chapterDisplayText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("chapter_dropdown_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = chapterExpanded,
                        onDismissRequest = { chapterExpanded = false }
                    ) {
                        availableChapters.forEach { chapter ->
                            DropdownMenuItem(
                                text = {
                                    Text("Ch ${chapter.chapterNumber}: ${chapter.title}")
                                },
                                onClick = {
                                    selectedChapter = chapter
                                    chapterExpanded = false
                                },
                                modifier = Modifier.testTag("chapter_option_${chapter.id}")
                            )
                        }
                    }
                }
            }

            // 4. Material Type Dropdown (Required)
            var materialTypeExpanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Material Type *",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                ExposedDropdownMenuBox(
                    expanded = materialTypeExpanded,
                    onExpandedChange = { materialTypeExpanded = !materialTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedMaterialType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialTypeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("material_type_dropdown_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = materialTypeExpanded,
                        onDismissRequest = { materialTypeExpanded = false }
                    ) {
                        materialTypeOptions.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedMaterialType = type
                                    materialTypeExpanded = false
                                },
                                modifier = Modifier.testTag("material_type_option_$type")
                            )
                        }
                    }
                }
            }

            // 5. Difficulty Level Dropdown
            var difficultyExpanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Difficulty Level",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                ExposedDropdownMenuBox(
                    expanded = difficultyExpanded,
                    onExpandedChange = { difficultyExpanded = !difficultyExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDifficulty,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("difficulty_dropdown_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = difficultyExpanded,
                        onDismissRequest = { difficultyExpanded = false }
                    ) {
                        difficultyOptions.forEach { diff ->
                            DropdownMenuItem(
                                text = { Text(diff) },
                                onClick = {
                                    selectedDifficulty = diff
                                    difficultyExpanded = false
                                },
                                modifier = Modifier.testTag("difficulty_option_$diff")
                            )
                        }
                    }
                }
            }

            // 6. PDF Link (Required Google Drive Sharing URL)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PDF Link (Google Drive Sharing URL) *",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                val extractedIdResult = remember(pdfUrl) {
                    if (pdfUrl.isNotBlank()) GoogleDriveUrlHelper.validateAndExtractFileId(pdfUrl) else null
                }

                OutlinedTextField(
                    value = pdfUrl,
                    onValueChange = {
                        pdfUrl = it
                        if (pdfUrlError != null) pdfUrlError = null
                    },
                    placeholder = { Text("https://drive.google.com/file/d/...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = if (extractedIdResult?.isSuccess == true) Color(0xFF16A34A) else RankifyPrimary
                        )
                    },
                    isError = pdfUrlError != null || (pdfUrl.isNotBlank() && extractedIdResult?.isFailure == true),
                    supportingText = {
                        if (pdfUrlError != null) {
                            Text(pdfUrlError!!, color = MaterialTheme.colorScheme.error)
                        } else if (pdfUrl.isNotBlank() && extractedIdResult != null) {
                            if (extractedIdResult.isSuccess) {
                                Text(
                                    "Google Drive File ID: ${extractedIdResult.getOrNull()}",
                                    color = Color(0xFF16A34A)
                                )
                            } else {
                                Text(
                                    extractedIdResult.exceptionOrNull()?.message
                                        ?: "Must be a valid Google Drive HTTPS file link.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text("Paste the Google Drive HTTPS sharing link of the uploaded PDF.")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_drive_url_input")
                )
            }

            // 7. YouTube Video ID (Optional)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "YouTube Video ID (Optional)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                OutlinedTextField(
                    value = youtubeVideoId,
                    onValueChange = { youtubeVideoId = it },
                    placeholder = { Text("e.g. dQw4w9WgXcQ (Use if linked to an exact lecture)") },
                    supportingText = {
                        Text("Use only when this study resource belongs to a specific lecture.")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_video_id_input")
                )
            }

            // 8. Description (Optional)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Description (Optional)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Add key topics covered, formula index, or notes summary...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resource_description_input")
                )
            }

            // 9. Publish Status Switch (Draft by default)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPublished) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isPublished) Color(0xFF6EE7B7) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("publish_status_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Publish Status: ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPublished) Color(0xFF10B981) else Color(0xFFF59E0B)
                            ) {
                                Text(
                                    text = if (isPublished) "PUBLISHED" else "DRAFT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPublished) {
                                "Visible to students once saved."
                            } else {
                                "Saved as private Draft (default). Students will not see this material."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isPublished,
                        onCheckedChange = { isPublished = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.testTag("publish_status_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: Save Button ("Save as Draft" by default)
            val buttonLabel = if (isPublished) "Publish Material" else "Save as Draft"

            Button(
                onClick = {
                    // Local pre-validation
                    var hasError = false
                    if (title.trim().isBlank()) {
                        titleError = "Resource Title cannot be empty."
                        hasError = true
                    }
                    val urlCheck = GoogleDriveUrlHelper.validateAndExtractFileId(pdfUrl)
                    if (urlCheck.isFailure) {
                        pdfUrlError = urlCheck.exceptionOrNull()?.message ?: "Invalid Google Drive link."
                        hasError = true
                    }

                    if (selectedChapter == null) {
                        hasError = true
                    }

                    if (hasError) return@Button

                    viewModel.saveStudyMaterial(
                        title = title,
                        description = description,
                        classLevel = classLevel,
                        board = board,
                        stream = stream,
                        subject = selectedSubject,
                        chapterId = selectedChapter?.id ?: "",
                        chapterName = selectedChapter?.title ?: "",
                        materialType = selectedMaterialType,
                        difficultyLevel = selectedDifficulty,
                        pdfUrl = pdfUrl,
                        youtubeVideoId = youtubeVideoId.takeIf { it.isNotBlank() },
                        published = isPublished,
                        onSuccess = {
                            // Clear inputs on verified success
                            title = ""
                            description = ""
                            pdfUrl = ""
                            youtubeVideoId = ""
                            isPublished = false
                            titleError = null
                            pdfUrlError = null
                        }
                    )
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPublished) Color(0xFF059669) else RankifyPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_study_material_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Saving to Firestore...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.testTag("save_button_label")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
