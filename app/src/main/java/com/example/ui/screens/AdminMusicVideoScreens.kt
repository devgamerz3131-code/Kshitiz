package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.DefaultData
import com.example.data.model.AdminAccessState
import com.example.data.model.ExclusiveMusicVideo
import com.example.data.model.SyllabusChapterEntity
import com.example.ui.theme.RankifyPrimary
import com.example.util.GoogleDriveUrlHelper
import com.example.viewmodel.RankifyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMusicVideoManagerScreen(
    viewModel: RankifyViewModel,
    adminAccessState: AdminAccessState,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    if (adminAccessState !is AdminAccessState.Admin) {
        AdminAccessDeniedScreen(adminAccessState, onNavigateBack)
        return
    }

    val videos by viewModel.adminMusicVideos.collectAsState()
    val isLoading by viewModel.isAdminMusicVideosLoading.collectAsState()
    val opInProgressId by viewModel.musicVideoOperationInProgress.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("admin_music_manager_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Music Video Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAdminMusicVideos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddMusicVideoScreen() },
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Song")
            }
        }
    ) { padding ->
        if (isLoading && videos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C3AED))
            }
        } else if (videos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text("No music videos added yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Tap + to add your first educational music video.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                videos.forEach { video ->
                    AdminMusicVideoCard(
                        video = video,
                        isOperating = opInProgressId == video.videoId,
                        onPreview = { viewModel.openVideoPreview(video) },
                        onEdit = { viewModel.openAddMusicVideoScreen(video) },
                        onTogglePublish = { viewModel.updateMusicVideoStatus(video.videoId, !video.published) },
                        onDelete = { viewModel.deleteMusicVideo(video.videoId) }
                    )
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AdminMusicVideoCard(
    video: ExclusiveMusicVideo,
    isOperating: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    val subjectColor = when (video.subject.lowercase()) {
        "physics" -> Color(0xFF1D4ED8)
        "chemistry" -> Color(0xFF0F766E)
        "mathematics" -> Color(0xFF6D28D9)
        else -> MaterialTheme.colorScheme.primary
    }

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = subjectColor.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    Text(video.subject, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = subjectColor)
                }
                Surface(
                    color = if (video.published) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(if (video.published) "PUBLISHED" else "DRAFT", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (video.published) Color(0xFF15803D) else Color(0xFFB45309))
                }
            }
            Text(video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Chapter: ${video.chapterName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (isOperating) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(
                        onClick = onPreview,
                        modifier = Modifier.testTag("preview_video_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Preview Video")
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = RankifyPrimary) }
                    IconButton(onClick = onTogglePublish) { 
                        Icon(if (video.published) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle Publish", tint = if (video.published) Color(0xFFB45309) else Color(0xFF15803D)) 
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicVideoScreen(
    viewModel: RankifyViewModel,
    adminAccessState: AdminAccessState,
    videoToEdit: ExclusiveMusicVideo?,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    if (adminAccessState !is AdminAccessState.Admin) {
        AdminAccessDeniedScreen(adminAccessState, onNavigateBack)
        return
    }

    val isSaving by viewModel.isSavingMusicVideo.collectAsState()
    val successMsg by viewModel.musicVideoSaveSuccess.collectAsState()
    val errorMsg by viewModel.musicVideoSaveError.collectAsState()

    var title by remember { mutableStateOf(videoToEdit?.title ?: "") }
    var description by remember { mutableStateOf(videoToEdit?.description ?: "") }
    var selectedSubject by remember { mutableStateOf(videoToEdit?.subject ?: "Physics") }
    var videoUrl by remember { mutableStateOf("") } // Admin needs to paste GD link
    var thumbnailUrl by remember { mutableStateOf(videoToEdit?.thumbnailUrl ?: "") }
    var coinPrice by remember { mutableStateOf(videoToEdit?.coinPrice?.toString() ?: "100") }
    var duration by remember { mutableStateOf(videoToEdit?.durationSeconds?.toString() ?: "300") }
    var isPublished by remember { mutableStateOf(videoToEdit?.published ?: false) }

    val allChapters = DefaultData.initialChapters
    val availableChapters = remember(selectedSubject) {
        allChapters.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }
    var selectedChapter by remember { 
        mutableStateOf(availableChapters.find { it.id == videoToEdit?.chapterId } ?: availableChapters.firstOrNull()) 
    }

    LaunchedEffect(selectedSubject) {
        if (selectedChapter?.subject?.equals(selectedSubject, ignoreCase = true) != true) {
            selectedChapter = availableChapters.firstOrNull()
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (videoToEdit == null) "Add Music Video" else "Edit Music Video", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (successMsg != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)), modifier = Modifier.fillMaxWidth()) {
                    Text(successMsg!!, Modifier.padding(16.dp), color = Color(0xFF14532D), fontWeight = FontWeight.SemiBold)
                }
            }
            if (errorMsg != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)), modifier = Modifier.fillMaxWidth()) {
                    Text(errorMsg!!, Modifier.padding(16.dp), color = Color(0xFF7F1D1D))
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Song Title *") }, modifier = Modifier.fillMaxWidth())
            
            // Subject Select
            val subjects = listOf("Physics", "Chemistry", "Mathematics")
            var subExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = subExpanded, onExpandedChange = { subExpanded = !subExpanded }) {
                OutlinedTextField(
                    value = selectedSubject, onValueChange = {}, readOnly = true,
                    label = { Text("Subject *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }) {
                    subjects.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { selectedSubject = s; subExpanded = false }) }
                }
            }

            // Chapter Select
            var chExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = chExpanded, onExpandedChange = { chExpanded = !chExpanded }) {
                OutlinedTextField(
                    value = selectedChapter?.title ?: "Select Chapter", onValueChange = {}, readOnly = true,
                    label = { Text("Chapter *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = chExpanded, onDismissRequest = { chExpanded = false }) {
                    availableChapters.forEach { ch -> DropdownMenuItem(text = { Text(ch.title) }, onClick = { selectedChapter = ch; chExpanded = false }) }
                }
            }

            OutlinedTextField(
                value = videoUrl, onValueChange = { videoUrl = it },
                label = { Text("Google Drive Video Link *") },
                placeholder = { Text("https://drive.google.com/file/d/...") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Admin only. Not shown to students.") }
            )

            OutlinedTextField(value = thumbnailUrl, onValueChange = { thumbnailUrl = it }, label = { Text("Thumbnail URL (Optional)") }, modifier = Modifier.fillMaxWidth())
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = coinPrice, onValueChange = { coinPrice = it }, label = { Text("SP Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (sec)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp)) {
                Text("Publish to Students", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Switch(checked = isPublished, onCheckedChange = { isPublished = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF10B981)))
            }

            Button(
                onClick = {
                    val extract = GoogleDriveUrlHelper.validateAndExtractFileId(videoUrl)
                    if (title.isBlank() || (videoToEdit == null && extract.isFailure)) return@Button
                    
                    val fileId = extract.getOrDefault("")
                    
                    viewModel.saveExclusiveMusicVideo(
                        video = ExclusiveMusicVideo(
                            videoId = videoToEdit?.videoId ?: "",
                            title = title,
                            subject = selectedSubject,
                            chapterId = selectedChapter?.id ?: "",
                            chapterName = selectedChapter?.title ?: "",
                            description = description,
                            thumbnailUrl = thumbnailUrl,
                            coinPrice = coinPrice.toIntOrNull() ?: 100,
                            durationSeconds = duration.toIntOrNull() ?: 300,
                            published = isPublished
                        ),
                        videoUrl = videoUrl,
                        googleDriveFileId = fileId
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isPublished) Color(0xFF059669) else Color(0xFF7C3AED))
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                else Text(if (videoToEdit == null) "Add Music Video" else "Update Music Video", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
