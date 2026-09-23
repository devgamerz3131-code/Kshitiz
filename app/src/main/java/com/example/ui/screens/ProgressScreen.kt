package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FirebaseAuthService
import com.example.data.model.AdminAccessState
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.StudentProfile
import com.example.ui.components.getSubjectColor
import com.example.ui.theme.RankifyAccent
import com.example.ui.theme.RankifyGlassBorder
import com.example.ui.theme.RankifyGlassSurface
import com.example.ui.theme.RankifyPrimary
import com.example.ui.theme.RankifyPurple
import com.example.ui.theme.RankifySoftGradient
import com.example.viewmodel.RankifyUiState
import com.example.viewmodel.RankifyViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    val selectedSection by viewModel.selectedProgressSection.collectAsState()
    val sections = listOf("Analytics", "Formula Sheets", "Profile")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { viewModel.setProgressSection(index) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                )
            }
        }

        when (selectedSection) {
            0 -> AnalyticsSection(state, onOpenAiWithPrompt)
            1 -> NotesSection(state, viewModel)
            2 -> ProfileAndExamSection(state, viewModel)
        }
    }
}

@Composable
fun AnalyticsSection(
    state: RankifyUiState,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    val totalStudyMins = state.sessions.sumOf { it.durationMinutes }
    val totalTests = state.testAttempts.size
    val avgScore = if (totalTests > 0) {
        (state.testAttempts.sumOf { it.score } * 100) / state.testAttempts.sumOf { it.maxScore }.coerceAtLeast(1)
    } else 0

    val weakChapters = state.chapters.filter { it.isWeakTopic }
    val strongChapters = state.chapters.filter { it.completionPercentage >= 80 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Key Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Study Time",
                    value = "${totalStudyMins / 60}h ${totalStudyMins % 60}m",
                    subtitle = "${state.sessions.size} recorded sessions",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Test Accuracy",
                    value = "$avgScore%",
                    subtitle = "$totalTests tests taken",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // AI Weekly Review
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    RankifyPrimary.copy(alpha = 0.08f),
                                    RankifyPurple.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "AI Study Diagnosis",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Class 12 Weekly Overview",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "• Strongest Subject: Mathematics (Matrices & Determinants completed with 90% accuracy).\n• Focus Needed: Physics (Capacitance numericals) & Electrochemistry (Nernst equation).\n• Study Consistency: 5-day active streak! Recommended target this week: 14 hours total focus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onOpenAiWithPrompt(
                                    "Mera complete Class 12 CBSE progress analyze karo and next 7 days ka revision roadmap do.",
                                    "PCM",
                                    "Weekly Review"
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detailed AI Weekly Diagnosis", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Topic Health Breakdown (Weak vs Strong)
        item {
            Text(
                text = "Topic Health Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Weak Areas Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ Weak Topics (${weakChapters.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (weakChapters.isEmpty()) {
                            Text(
                                text = "No weak topics flagged!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7F1D1D)
                            )
                        } else {
                            weakChapters.forEach { ch ->
                                Text(
                                    text = "• ${ch.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F1D1D),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Strong Areas Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✅ Strong Topics (${strongChapters.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (strongChapters.isEmpty()) {
                            Text(
                                text = "Keep studying to build mastery!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF14532D)
                            )
                        } else {
                            strongChapters.take(3).forEach { ch ->
                                Text(
                                    text = "• ${ch.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF14532D),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RankifyGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = RankifyPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotesSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel
) {
    var showAddNoteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Notes & Formula Sheets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = { showAddNoteDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Sheet", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (state.notes.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No notes saved yet. You can save any AI Tutor explanation or tap 'New Sheet' above!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(state.notes, key = { it.id }) { note ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = getSubjectColor(note.subject).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, getSubjectColor(note.subject).copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = note.noteType,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = getSubjectColor(note.subject),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${note.subject} • ${note.chapter}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddNoteDialog) {
        var subject by remember { mutableStateOf("Physics") }
        var chapter by remember { mutableStateOf("Current Electricity") }
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Add Formula Sheet / Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { chapter = it },
                        label = { Text("Chapter") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Formulas / Notes") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.addNote(subject, chapter, title, content, "Formula Sheet")
                            showAddNoteDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                ) {
                    Text("Save", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddNoteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileAndExamSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel
) {
    val profile = state.profile ?: return
    val currentAuthUser = FirebaseAuthService.currentUser
    val isUserLoggedIn = (currentAuthUser != null && !currentAuthUser.isAnonymous) || state.isLoggedIn

    // Local form state for Edit Profile
    var editName by remember(profile.name) { mutableStateOf(profile.name) }
    var editClass by remember(profile.studentClass) { mutableStateOf(profile.studentClass) }
    var editBoard by remember(profile.board) { mutableStateOf(profile.board) }
    var editStream by remember(profile.stream) { mutableStateOf(profile.stream) }
    var editSubjects by remember(profile.subjects) { mutableStateOf(profile.subjects) }
    var editTargetPercent by remember(profile.targetPercentage) { mutableStateOf(profile.targetPercentage.toString()) }
    var editDailyGoalMins by remember(profile.dailyStudyGoalMinutes) { mutableStateOf(profile.dailyStudyGoalMinutes.toString()) }
    var editLanguage by remember(profile.languagePreference) { mutableStateOf(profile.languagePreference) }
    var editHasUpcomingExam by remember(profile.hasUpcomingExam) { mutableStateOf(profile.hasUpcomingExam) }
    var editExamName by remember(profile.examName) { mutableStateOf(profile.examName) }
    var editExamDate by remember(profile.examDate) { mutableStateOf(profile.examDate) }

    var isSavingProfile by remember { mutableStateOf(false) }
    var localFormError by remember { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showExamDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Loading State indicator
        if (state.isProfileLoading) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Loading cloud profile...",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Status banner (Profile save / Cloud status message)
        val statusMsg = state.profileStatusMessage
        if (!statusMsg.isNullOrBlank()) {
            item {
                val isSuccess = statusMsg.contains("success", ignoreCase = true) || statusMsg.contains("locally", ignoreCase = true)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(1.dp, if (isSuccess) Color(0xFF15803D).copy(alpha = 0.3f) else Color(0xFFDC2626).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isSuccess) Color(0xFF15803D) else Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSuccess) Color(0xFF15803D) else Color(0xFFDC2626)
                            )
                        }
                        IconButton(onClick = { viewModel.clearProfileStatusMessage() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Account & Cloud Sync Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isUserLoggedIn) {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF16A34A).copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        RankifyPrimary.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                )
                            }
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isUserLoggedIn) Color(0xFF16A34A) else RankifyPrimary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isUserLoggedIn) Icons.Default.CheckCircle else Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Account & Cloud Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isUserLoggedIn) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7),
                                    border = BorderStroke(1.dp, Color(0xFF15803D).copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = "Connected",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, RankifyGlassBorder)
                                ) {
                                    Text(
                                        text = "Offline / Guest",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        if (isUserLoggedIn) {
                            val displayEmail = if (profile.email.isNotBlank()) {
                                profile.email
                            } else {
                                state.authUserEmail ?: currentAuthUser?.email ?: "Connected User"
                            }
                            Text(
                                text = "Account: $displayEmail",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "All study metrics, ${state.testAttempts.size} test analyses, ${state.notes.size} notes, and study sessions are synchronized with Cloud Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!state.cloudSyncMessage.isNullOrBlank()) {
                                Text(
                                    text = state.cloudSyncMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.performSmartSync() },
                                    enabled = !state.isCloudSyncing,
                                    modifier = Modifier.weight(1f).testTag("sync_now_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                                ) {
                                    if (state.isCloudSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Syncing...")
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync Now", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showSignOutDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign Out", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        } else {
                            Text(
                                text = "Backup your Class 12 study progress, notes, formula bookmarks, and test analyses to Cloud Firestore. Access them seamlessly across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showAuthDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In or Register with Firebase", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // AI Study Planner Card
        item {
            val studyLevelTitle = when (profile.studyLevel) {
                "concept_foundation" -> "Concept Foundation (CBSE 12)"
                "dual_mastery" -> "Dual-Target Mastery (CBSE + JEE/NEET)"
                "rank_booster" -> "Rank Booster (Top 1% Percentile)"
                else -> profile.studyLevel.ifBlank { "Dual-Target Mastery" }
            }
            val isPlanActive = profile.studyPlanConfigured

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_study_planner_profile_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    RankifyPrimary.copy(alpha = 0.05f),
                                    RankifyPurple.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(RankifySoftGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "AI Study Planner",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isPlanActive) "Active & Tailored" else "Ready to Configure",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPlanActive) Color(0xFF16A34A) else RankifyPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPlanActive) Color(0xFFDCFCE7) else RankifyPrimary.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, if (isPlanActive) Color(0xFF15803D).copy(alpha = 0.25f) else RankifyPrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (isPlanActive) "Configured" else "First-Time",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isPlanActive) Color(0xFF15803D) else RankifyPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Current Study Track: $studyLevelTitle",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (profile.studyPlanSummary.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = profile.studyPlanSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Daily Target", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF1E40AF))
                                    Text("${profile.dailyStudyGoalMinutes / 60}h ${profile.dailyStudyGoalMinutes % 60}m", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1D4ED8))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Target Score", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF166534))
                                    Text("${profile.targetPercentage}%+", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF15803D))
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetStudyPlan()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_study_plan_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RankifyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset & Reconfigure Study Plan",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = RankifyPrimary
                            )
                        }
                    }
                }
            }
        }

        // Private Administrator Dashboard Entry
        if (state.adminAccessState is AdminAccessState.Admin) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_dashboard_profile_entry_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        RankifyPrimary.copy(alpha = 0.08f),
                                        RankifyPurple.copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Admin Dashboard",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("admin_dashboard_title_text")
                                    )
                                    Text(
                                        text = "Administrator access verified",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.openAdminDashboard() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                                modifier = Modifier.testTag("open_admin_dashboard_button")
                            ) {
                                Text("Open", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // Student Profile Summary Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Student Profile Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    val studentDisplayName = profile.name.takeIf { it.isNotBlank() } ?: "Student"
                    val studentDisplayEmail = if (profile.email.isNotBlank()) {
                        profile.email
                    } else {
                        state.authUserEmail ?: currentAuthUser?.email ?: "Not registered (Guest)"
                    }
                    Text("Name: $studentDisplayName", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text("Email: $studentDisplayEmail", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Class: ${profile.studentClass} • Board: ${profile.board}", style = MaterialTheme.typography.bodyMedium)
                    Text("Stream: ${profile.stream} • Subjects: ${profile.subjects}", style = MaterialTheme.typography.bodyMedium)
                    Text("Target: ${profile.targetPercentage}% • Daily Goal: ${profile.dailyStudyGoalMinutes} mins (${profile.dailyStudyGoalMinutes / 60}h ${profile.dailyStudyGoalMinutes % 60}m)", style = MaterialTheme.typography.bodyMedium)
                    Text("Preferred Language: ${profile.languagePreference}", style = MaterialTheme.typography.bodyMedium)
                    val examInfo = if (profile.hasUpcomingExam && profile.examName.isNotBlank()) {
                        "${profile.examName} (${profile.examDate})"
                    } else {
                        "No exam right now"
                    }
                    Text("Exam: $examInfo", style = MaterialTheme.typography.bodyMedium)
                    Text("Active Streak: ${profile.streakDays} Days 🔥", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = RankifyPrimary))
                }
            }
        }

        // Edit / Update Profile Section
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = RankifyPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Edit / Update Profile",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Changes are saved to Cloud Firestore users/{uid} and local storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Full Name
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            localFormError = null
                        },
                        label = { Text("Full Name *") },
                        placeholder = { Text("Enter your full name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_student_name_input")
                    )

                    // Class selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Class",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Class 12", "Class 11", "Dropper", "Class 10").forEach { cls ->
                                FilterChip(
                                    selected = editClass == cls,
                                    onClick = { editClass = cls },
                                    label = { Text(cls, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Board selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Board",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CBSE", "ICSE", "State Board", "Other").forEach { brd ->
                                FilterChip(
                                    selected = editBoard == brd,
                                    onClick = { editBoard = brd },
                                    label = { Text(brd, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Stream selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Stream",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("PCM", "PCB", "PCMB", "Commerce", "Arts").forEach { stm ->
                                FilterChip(
                                    selected = editStream == stm,
                                    onClick = { editStream = stm },
                                    label = { Text(stm, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Subjects
                    OutlinedTextField(
                        value = editSubjects,
                        onValueChange = { editSubjects = it },
                        label = { Text("Subjects") },
                        placeholder = { Text("Physics, Chemistry, Mathematics") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target Percentage & Daily Goal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editTargetPercent,
                            onValueChange = { editTargetPercent = it },
                            label = { Text("Target %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editDailyGoalMins,
                            onValueChange = { editDailyGoalMins = it },
                            label = { Text("Goal (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Preferred Language
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred Language",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Hinglish", "English", "Hindi").forEach { lang ->
                                FilterChip(
                                    selected = editLanguage == lang,
                                    onClick = { editLanguage = lang },
                                    label = { Text(lang, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Target Exam Settings
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Exam Settings",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !editHasUpcomingExam,
                                onClick = { editHasUpcomingExam = false },
                                label = { Text("No exam right now") }
                            )
                            FilterChip(
                                selected = editHasUpcomingExam,
                                onClick = { editHasUpcomingExam = true },
                                label = { Text("Upcoming exam") }
                            )
                        }

                        if (editHasUpcomingExam) {
                            OutlinedTextField(
                                value = editExamName,
                                onValueChange = { editExamName = it },
                                label = { Text("Exam Name") },
                                placeholder = { Text("e.g. CBSE Class 12 Boards, JEE Main") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editExamDate,
                                onValueChange = { editExamDate = it },
                                label = { Text("Exam Date (YYYY-MM-DD)") },
                                placeholder = { Text("2026-10-20") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (localFormError != null) {
                        Text(
                            text = localFormError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Save Profile Button
                    Button(
                        onClick = {
                            val trimmed = editName.trim()
                            if (trimmed.isBlank()) {
                                localFormError = "Please enter your name."
                                return@Button
                            }
                            if (editHasUpcomingExam && editExamName.trim().isBlank()) {
                                localFormError = "Please enter your exam name."
                                return@Button
                            }
                            if (editHasUpcomingExam && editExamDate.trim().isBlank()) {
                                localFormError = "Please enter your exam date."
                                return@Button
                            }

                            localFormError = null
                            isSavingProfile = true
                            viewModel.saveUserProfileSettings(
                                name = trimmed,
                                studentClass = editClass,
                                board = editBoard,
                                stream = editStream,
                                subjects = editSubjects,
                                targetPercentage = editTargetPercent.toIntOrNull() ?: 95,
                                dailyStudyGoalMinutes = editDailyGoalMins.toIntOrNull() ?: 180,
                                preferredLanguage = editLanguage,
                                hasUpcomingExam = editHasUpcomingExam,
                                examName = editExamName,
                                examDate = editExamDate,
                                onSuccess = {
                                    isSavingProfile = false
                                },
                                onError = {
                                    isSavingProfile = false
                                }
                            )
                        },
                        enabled = !isSavingProfile,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button")
                    ) {
                        if (isSavingProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving Profile...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showExamDialog) {
        var examName by remember { mutableStateOf("CBSE Pre-Board Exam") }
        var examSubject by remember { mutableStateOf("Physics & Chemistry") }
        var examDate by remember { mutableStateOf("2026-10-18") }
        var targetScore by remember { mutableStateOf("95") }
        var syllabus by remember { mutableStateOf("Ch 1 to Ch 5 (Term 1)") }

        AlertDialog(
            onDismissRequest = { showExamDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Configure Upcoming Exam", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = examName, onValueChange = { examName = it }, label = { Text("Exam Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = examSubject, onValueChange = { examSubject = it }, label = { Text("Subjects") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = examDate, onValueChange = { examDate = it }, label = { Text("Exam Date (YYYY-MM-DD)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = targetScore, onValueChange = { targetScore = it }, label = { Text("Target Score %") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = syllabus, onValueChange = { syllabus = it }, label = { Text("Syllabus Coverage") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveExam(examName, examSubject, examDate, targetScore.toIntOrNull() ?: 95, syllabus)
                        showExamDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                ) {
                    Text("Save Exam", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExamDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Sign Out of Firebase?") },
            text = { Text("Your local study progress and bookmarks will be kept safely on this device. You can sign in anytime to re-sync with Cloud Firestore.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAuthDialog) {
        Dialog(
            onDismissRequest = { showAuthDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuthScreen(
                    state = state,
                    viewModel = viewModel,
                    onAuthSuccess = { showAuthDialog = false },
                    onContinueAsGuest = { showAuthDialog = false },
                    onDismiss = { showAuthDialog = false }
                )
            }
        }
    }
}
