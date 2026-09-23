package com.example.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyDockLectureSession
import com.example.data.model.SyllabusChapterEntity
import com.example.ui.theme.RankifyAccent
import com.example.ui.theme.RankifyPrimary
import com.example.util.ParsedYouTubeUrl
import com.example.util.YouTubeUrlParser
import com.example.viewmodel.RankifyViewModel
import kotlinx.coroutines.launch

/**
 * Explicit validation states for StudyDock YouTube URL input.
 */
enum class UrlValidationState {
    EMPTY,
    VALIDATING,
    VALID,
    INVALID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDockScreen(
    initialUrl: String = "",
    initialSharedBanner: String? = null,
    onUrlChanged: (String) -> Unit = {},
    viewModel: RankifyViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var inputUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var sharedNoticeMessage by remember(initialSharedBanner) { mutableStateOf(initialSharedBanner) }
    var validationState by remember(initialUrl) {
        mutableStateOf(
            if (initialUrl.isBlank()) {
                UrlValidationState.EMPTY
            } else {
                val parsed = YouTubeUrlParser.parse(initialUrl)
                if (parsed != null) UrlValidationState.VALID else UrlValidationState.INVALID
            }
        )
    }
    var parsedResult by remember(initialUrl) {
        mutableStateOf(if (initialUrl.isNotBlank()) YouTubeUrlParser.parse(initialUrl) else null)
    }

    // Step 4 & 5: Academic Context & Active Lecture Session from ViewModel (or fallback local state)
    val activeSessionFromVm by (viewModel?.activeLectureSession?.collectAsState() ?: remember { mutableStateOf(null) })
    val exactResourcesFromVm by (viewModel?.studyDockExactResources?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val chapterResourcesFromVm by (viewModel?.studyDockChapterResources?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val isLoadingResourcesFromVm by (viewModel?.isStudyDockResourcesLoading?.collectAsState() ?: remember { mutableStateOf(false) })
    val vmSelectedSubject by (viewModel?.studyDockSelectedSubject?.collectAsState() ?: remember { mutableStateOf(null) })
    val vmSelectedChapter by (viewModel?.studyDockSelectedChapter?.collectAsState() ?: remember { mutableStateOf(null) })
    val uiStateChapters = viewModel?.uiState?.collectAsState()?.value?.chapters ?: emptyList()

    // Local fallback state if ViewModel is not provided
    var localSubject by remember { mutableStateOf<String?>(null) }
    var localChapter by remember { mutableStateOf<String?>(null) }
    var localSession by remember { mutableStateOf<StudyDockLectureSession?>(null) }
    var localExactResources by remember { mutableStateOf<List<com.example.data.model.StudyDockResource>>(emptyList()) }
    var localChapterResources by remember { mutableStateOf<List<com.example.data.model.StudyDockResource>>(emptyList()) }
    var localIsLoadingResources by remember { mutableStateOf(false) }

    val currentSubject = if (viewModel != null) vmSelectedSubject else localSubject
    val currentChapter = if (viewModel != null) vmSelectedChapter else localChapter
    val activeSession = if (viewModel != null) activeSessionFromVm else localSession
    val exactResources = if (viewModel != null) exactResourcesFromVm else localExactResources
    val chapterResources = if (viewModel != null) chapterResourcesFromVm else localChapterResources
    val isLoadingResources = if (viewModel != null) isLoadingResourcesFromVm else localIsLoadingResources

    // Standard CBSE Class 12 PCM subjects
    val availableSubjects = remember { listOf("Physics", "Chemistry", "Mathematics") }

    // Chapters for selected subject from existing Rankify syllabus
    val availableChapters: List<SyllabusChapterEntity> = remember(currentSubject, uiStateChapters) {
        if (currentSubject.isNullOrBlank()) {
            emptyList()
        } else {
            uiStateChapters.filter { it.subject.equals(currentSubject, ignoreCase = true) }
        }
    }

    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var chapterDropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Intercept hardware back if on session confirmation screen
    BackHandler(enabled = activeSession != null) {
        if (viewModel != null) {
            viewModel.clearActiveLectureSession()
        } else {
            localSession = null
        }
    }

    // Helper: evaluate input without mutative typing side-effects
    fun evaluateUrl(rawText: String, isLeavingOrSubmitting: Boolean = false) {
        val trimmed = YouTubeUrlParser.cleanRawInput(rawText)
        if (trimmed.isEmpty()) {
            validationState = UrlValidationState.EMPTY
            parsedResult = null
            return
        }

        val parsed = YouTubeUrlParser.parse(trimmed)
        if (parsed != null) {
            validationState = UrlValidationState.VALID
            parsedResult = parsed
        } else {
            if (isLeavingOrSubmitting) {
                validationState = UrlValidationState.INVALID
                parsedResult = null
            } else {
                validationState = UrlValidationState.VALIDATING
                parsedResult = null
            }
        }
    }

    fun onSubjectSelected(subject: String?) {
        if (viewModel != null) {
            viewModel.setStudyDockSubject(subject)
        } else {
            localSubject = subject
            if (localChapter != null && subject != null) {
                val belongs = uiStateChapters.any { it.subject.equals(subject, ignoreCase = true) && it.title.equals(localChapter, ignoreCase = true) }
                if (!belongs) localChapter = null
            } else if (subject == null) {
                localChapter = null
            }
        }
    }

    fun onChapterSelected(chapter: String?) {
        if (viewModel != null) {
            viewModel.setStudyDockChapter(chapter)
        } else {
            localChapter = chapter
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (activeSession != null) {
                                if (viewModel != null) {
                                    viewModel.clearActiveLectureSession()
                                } else {
                                    localSession = null
                                }
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("studydock_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "StudyDock",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // STEP 5: StudyDock Lecture Hub (External AI Study Assistant, YouTube Launch & Resources)
            if (activeSession != null) {
                StudyDockLectureHubView(
                    session = activeSession,
                    exactResources = exactResources,
                    chapterResources = chapterResources,
                    isLoadingResources = isLoadingResources,
                    viewModel = viewModel,
                    onBackToDock = {
                        if (viewModel != null) {
                            viewModel.clearActiveLectureSession()
                        } else {
                            localSession = null
                        }
                    }
                )
            } else {
                // Standard StudyDock Step 4 Input, Academic Selectors & Session Setup
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // StudyDock Badge / Icon
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        RankifyPrimary,
                                        Color(0xFF7C3AED),
                                        RankifyAccent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "StudyDock",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Screen Title
                    Text(
                        text = "StudyDock ✨",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = "Turn any lecture into a study session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Notice Banner when coming from YouTube Share or validation error
                    if (sharedNoticeMessage != null) {
                        Spacer(modifier = Modifier.height(18.dp))
                        val isSuccessNotice = sharedNoticeMessage!!.contains("✓")
                        val noticeBgColor = if (isSuccessNotice) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        val noticeBorderColor = if (isSuccessNotice) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        val noticeIcon = if (isSuccessNotice) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
                        val noticeTint = if (isSuccessNotice) Color(0xFF10B981) else MaterialTheme.colorScheme.error

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studydock_shared_banner"),
                            shape = RoundedCornerShape(12.dp),
                            color = noticeBgColor,
                            border = BorderStroke(1.dp, noticeBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = noticeIcon,
                                    contentDescription = null,
                                    tint = noticeTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sharedNoticeMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Input card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "YouTube Lecture Link",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Paste Button beside / above field
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val hasText = clipboard?.hasPrimaryClip() == true &&
                                                (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                                                        clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true)

                                        val clipText = if (hasText) {
                                            clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        } else {
                                            null
                                        }

                                        val cleaned = clipText?.let { YouTubeUrlParser.cleanRawInput(it) } ?: ""

                                        if (cleaned.isBlank()) {
                                            Toast.makeText(context, "Nothing to paste.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            inputUrl = cleaned
                                            onUrlChanged(cleaned)
                                            evaluateUrl(cleaned, isLeavingOrSubmitting = true)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("studydock_paste_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        modifier = Modifier.size(13.dp),
                                        tint = RankifyPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Paste",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = RankifyPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { newValue ->
                                    inputUrl = newValue
                                    onUrlChanged(newValue)
                                    evaluateUrl(newValue, isLeavingOrSubmitting = false)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            if (inputUrl.isNotBlank()) {
                                                evaluateUrl(inputUrl, isLeavingOrSubmitting = true)
                                            }
                                        }
                                    }
                                    .testTag("studydock_link_input"),
                                placeholder = {
                                    Text(
                                        text = "Paste a YouTube lecture link",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Link",
                                        tint = if (validationState == UrlValidationState.VALID) Color(0xFF10B981) else RankifyAccent
                                    )
                                },
                                trailingIcon = {
                                    if (inputUrl.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                inputUrl = ""
                                                onUrlChanged("")
                                                validationState = UrlValidationState.EMPTY
                                                parsedResult = null
                                            },
                                            modifier = Modifier.testTag("studydock_clear_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear URL",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        evaluateUrl(inputUrl, isLeavingOrSubmitting = true)
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                isError = validationState == UrlValidationState.INVALID,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (validationState == UrlValidationState.VALID) Color(0xFF10B981) else RankifyPrimary,
                                    unfocusedBorderColor = if (validationState == UrlValidationState.VALID) Color(0xFF10B981).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                )
                            )

                            // Subtle validation messages below field
                            AnimatedVisibility(
                                visible = validationState == UrlValidationState.VALID,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .testTag("studydock_valid_indicator"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Valid YouTube link ✓",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = validationState == UrlValidationState.INVALID,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .testTag("studydock_error_indicator"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Enter a valid YouTube lecture link.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Step 4: Subject Selection (Optional)
                            Text(
                                text = "Subject (Optional)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = subjectDropdownExpanded,
                                onExpandedChange = { subjectDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = currentSubject ?: "Select Subject",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (currentSubject != null) {
                                                IconButton(
                                                    onClick = { onSubjectSelected(null) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Clear Subject",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedBorderColor = RankifyPrimary
                                    ),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                        .fillMaxWidth()
                                        .testTag("studydock_subject_dropdown")
                                )

                                ExposedDropdownMenu(
                                    expanded = subjectDropdownExpanded,
                                    onDismissRequest = { subjectDropdownExpanded = false }
                                ) {
                                    availableSubjects.forEach { subj ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = subj,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (subj == currentSubject) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (subj == currentSubject) RankifyPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                onSubjectSelected(subj)
                                                subjectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Step 4: Chapter Selection (Optional)
                            Text(
                                text = "Chapter (Optional)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = chapterDropdownExpanded && !currentSubject.isNullOrBlank(),
                                onExpandedChange = {
                                    if (!currentSubject.isNullOrBlank()) {
                                        chapterDropdownExpanded = it
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val isChapterEnabled = !currentSubject.isNullOrBlank()
                                val chapterDisplayValue = when {
                                    currentSubject.isNullOrBlank() -> "Choose a subject first"
                                    currentChapter != null -> currentChapter
                                    else -> "Select Chapter"
                                }

                                OutlinedTextField(
                                    value = chapterDisplayValue,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = isChapterEnabled,
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (currentChapter != null && isChapterEnabled) {
                                                IconButton(
                                                    onClick = { onChapterSelected(null) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Clear Chapter",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterDropdownExpanded)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedBorderColor = RankifyPrimary,
                                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, isChapterEnabled)
                                        .fillMaxWidth()
                                        .testTag("studydock_chapter_dropdown")
                                )

                                if (isChapterEnabled) {
                                    ExposedDropdownMenu(
                                        expanded = chapterDropdownExpanded,
                                        onDismissRequest = { chapterDropdownExpanded = false },
                                        modifier = Modifier.heightIn(max = 280.dp)
                                    ) {
                                        availableChapters.forEach { chapterEntity ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "Ch ${chapterEntity.chapterNumber}: ${chapterEntity.title}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (chapterEntity.title == currentChapter) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (chapterEntity.title == currentChapter) RankifyPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    onChapterSelected(chapterEntity.title)
                                                    chapterDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Step 4: Analyze Lecture Button
                            val isButtonEnabled = validationState == UrlValidationState.VALID && !isSubmitting

                            Button(
                                onClick = {
                                    if (isSubmitting) return@Button
                                    keyboardController?.hide()
                                    focusManager.clearFocus()

                                    val parsed = YouTubeUrlParser.parse(inputUrl)
                                    if (parsed == null) {
                                        validationState = UrlValidationState.INVALID
                                        return@Button
                                    }

                                    // Prevent double-submission
                                    isSubmitting = true
                                    validationState = UrlValidationState.VALID

                                    if (viewModel != null) {
                                        viewModel.createLectureSession(
                                            originalUrl = parsed.originalUrl,
                                            normalizedUrl = parsed.normalizedUrl,
                                            videoId = parsed.videoId,
                                            subject = currentSubject,
                                            chapter = currentChapter
                                        )
                                    } else {
                                        localSession = StudyDockLectureSession(
                                            originalYouTubeUrl = parsed.originalUrl,
                                            normalizedYouTubeUrl = parsed.normalizedUrl,
                                            videoId = parsed.videoId,
                                            selectedSubject = currentSubject,
                                            selectedChapter = currentChapter
                                        )
                                        // Load local curated resources in preview/test mode
                                        coroutineScope.launch {
                                            localIsLoadingResources = true
                                            try {
                                                val (exact, related) = com.example.data.firebase.StudyDockResourceService.getResourcesForLecture(
                                                    videoId = parsed.videoId,
                                                    subject = currentSubject,
                                                    chapter = currentChapter
                                                )
                                                localExactResources = exact
                                                localChapterResources = related
                                            } catch (e: Exception) {
                                                localExactResources = emptyList()
                                                localChapterResources = emptyList()
                                            } finally {
                                                localIsLoadingResources = false
                                            }
                                        }
                                    }
                                    isSubmitting = false
                                },
                                enabled = isButtonEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("studydock_analyze_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RankifyPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyze Lecture",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Supported: youtube.com/watch, youtu.be, /live links",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
