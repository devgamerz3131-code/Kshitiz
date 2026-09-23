package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity
import com.example.ui.components.MiniProgressRing
import com.example.ui.components.RankifyProgressRing
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
fun StudyScreen(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Syllabus, 1: Backlog, 2: Stopwatch
    val sections = listOf("Syllabus Tracker", "Backlog Manager", "Study Stopwatch")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
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
            0 -> SyllabusTrackerSection(state, viewModel, onOpenAiWithPrompt)
            1 -> BacklogManagerSection(state, viewModel, onOpenAiWithPrompt)
            2 -> StudyStopwatchSection(state, viewModel)
        }
    }
}

@Composable
fun SyllabusTrackerSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    var selectedSubject by remember { mutableStateOf("All") }
    val subjects = listOf("All", "Physics", "Chemistry", "Mathematics")

    val filteredChapters = if (selectedSubject == "All") {
        state.chapters
    } else {
        state.chapters.filter { it.subject == selectedSubject }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Rankify Exclusive Tool
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openExclusiveMain() }
                    .testTag("study_rankify_exclusive_item")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(RankifyPrimary, Color(0xFF7C3AED))),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Headset, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rankify Exclusive", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Free exclusive study songs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }

        item {
            // Subject Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subject ->
                    FilterChip(
                        selected = selectedSubject == subject,
                        onClick = { selectedSubject = subject },
                        label = { Text(subject, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        items(filteredChapters, key = { it.id }) { chapter ->
            ChapterTrackerCard(
                chapter = chapter,
                onToggleStage = { stageIndex ->
                    viewModel.toggleStage(chapter, stageIndex)
                },
                onAskAI = {
                    onOpenAiWithPrompt(
                        "Please explain key concepts, important formulas, and CBSE board exam tips for ${chapter.title}.",
                        chapter.subject,
                        chapter.title
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChapterTrackerCard(
    chapter: SyllabusChapterEntity,
    onToggleStage: (Int) -> Unit,
    onAskAI: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val subjectColor = getSubjectColor(chapter.subject)

    val progressStatusText = when {
        chapter.completionPercentage == 0 -> "Not Started (0%)"
        chapter.completionPercentage >= 100 -> "Completed (100%)"
        else -> "In Progress (${chapter.completionPercentage}%)"
    }
    val progressStatusBg = when {
        chapter.completionPercentage == 0 -> Color(0xFFF1F5F9)
        chapter.completionPercentage >= 100 -> Color(0xFFDCFCE7)
        else -> Color(0xFFFEF3C7)
    }
    val progressStatusFg = when {
        chapter.completionPercentage == 0 -> Color(0xFF475569)
        chapter.completionPercentage >= 100 -> Color(0xFF15803D)
        else -> Color(0xFFB45309)
    }
    val progressStatusBorder = when {
        chapter.completionPercentage == 0 -> Color(0xFFCBD5E1)
        chapter.completionPercentage >= 100 -> Color(0xFF86EFAC)
        else -> Color(0xFFFCD34D)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RankifyGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_card_${chapter.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Progress Ring for this chapter
                MiniProgressRing(
                    percentage = chapter.completionPercentage,
                    color = if (chapter.completionPercentage == 100) Color(0xFF10B981) else subjectColor,
                    size = 48.dp,
                    strokeWidth = 5.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = subjectColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${chapter.subject} • Ch ${chapter.chapterNumber}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = subjectColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }

                        // Explicit Per-Chapter Progress Status Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = progressStatusBg,
                            border = BorderStroke(1.dp, progressStatusBorder)
                        ) {
                            Text(
                                text = progressStatusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = progressStatusFg,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }

                        if (chapter.isWeakTopic) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "Needs Focus",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    // Linear progress bar with stage completion count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { chapter.completionPercentage / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (chapter.completionPercentage >= 100) Color(0xFF10B981) else subjectColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${chapter.completedStages}/6 done",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand stages",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "Preparation Stages (auto-updates via PDF, Practice, Revision, Target or tap):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val stages = listOf(
                        Pair("Concepts (AI)", chapter.conceptsDone),
                        Pair("NCERT Reading (PDF)", chapter.ncertReadingDone),
                        Pair("NCERT Questions", chapter.ncertQuestionsDone),
                        Pair("PYQs (Practice)", chapter.pyqDone),
                        Pair("Revision", chapter.revisionDone),
                        Pair("Mock Test", chapter.testDone)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stages.forEachIndexed { index, (name, isDone) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDone) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDone) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.clickable { onToggleStage(index) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = name,
                                        tint = if (isDone) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isDone) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDone) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAskAI,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Explain Chapter",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explain Chapter", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}

@Composable
fun BacklogManagerSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    var showAddBacklogDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // AI Clear My Backlog Hero Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
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
                        Column {
                            Text(
                                text = "Backlog Recovery Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${state.backlogTargets.size} pending tasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                onOpenAiWithPrompt(
                                    "Mera Class 12 CBSE backlog clear karne ke liye realistic plan banaiye without overloading. Pending tasks: ${state.backlogTargets.joinToString { it.task }}",
                                    "PCM",
                                    "Backlog Strategy"
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                            modifier = Modifier.testTag("clear_my_backlog_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Clear Backlog", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Backlog", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pending Items",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = { showAddBacklogDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Backlog", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Backlog", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        if (state.backlogTargets.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Great job! You have zero backlog. All planned work is on track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(state.backlogTargets, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.task,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.subject} • ${item.chapter} • Priority: ${item.priority}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = getSubjectColor(item.subject)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleTargetStatus(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Mark done",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddBacklogDialog) {
        AddTargetDialog(
            onDismiss = { showAddBacklogDialog = false },
            onAdd = { subject, chapter, task, priority, estMins ->
                viewModel.addTarget(subject, chapter, task, priority, estMins, isBacklog = true)
                showAddBacklogDialog = false
            }
        )
    }
}

@Composable
fun StudyStopwatchSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel
) {
    val isRunning by viewModel.isStopwatchRunning.collectAsState()
    val seconds by viewModel.stopwatchSeconds.collectAsState()
    val selectedSubject by viewModel.stopwatchSubject.collectAsState()
    val selectedChapter by viewModel.stopwatchChapter.collectAsState()

    var showFinishDialog by remember { mutableStateOf(false) }

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, secs)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Big Circular Timer Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    RankifyPrimary.copy(alpha = 0.05f),
                                    RankifyPurple.copy(alpha = 0.03f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RankifyPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "FOCUS STOPWATCH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = RankifyPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$selectedSubject • $selectedChapter",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Circular Progress Ring wrapping the elapsed time
                    val minuteProgress = (secs / 60f).coerceIn(0f, 1f)
                    RankifyProgressRing(
                        progress = if (isRunning) minuteProgress else 0.85f,
                        size = 170.dp,
                        strokeWidth = 9.dp,
                        startColor = RankifyPrimary,
                        endColor = RankifyPurple,
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isRunning) "ACTIVE" else if (seconds > 0) "PAUSED" else "READY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRunning) Color(0xFF10B981) else if (seconds > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    // Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRunning && seconds == 0L) {
                            Button(
                                onClick = { viewModel.startStopwatch() },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("start_stopwatch_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Session", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (isRunning) viewModel.pauseStopwatch() else viewModel.resumeStopwatch()
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRunning) Color(0xFFF59E0B) else RankifyPrimary
                                ),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isRunning) "Pause" else "Resume"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isRunning) "Pause" else "Resume", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            Button(
                                onClick = { showFinishDialog = true },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Finish")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finish", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // Recent study sessions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Study Sessions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (state.sessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No sessions recorded today yet. Start the stopwatch above when you study!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.sessions.take(6), key = { it.id }) { session ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${session.subject} • ${session.chapter}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Difficulty: ${session.difficultyRating} • ${if (session.notes.isNotBlank()) session.notes else "Completed focus"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RankifyPrimary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "${session.durationMinutes}m",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = RankifyPrimary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showFinishDialog) {
        val elapsedMins = (seconds / 60).coerceAtLeast(1).toInt()
        FinishSessionDialog(
            subject = selectedSubject,
            chapter = selectedChapter,
            elapsedMinutes = elapsedMins,
            onDismiss = { showFinishDialog = false },
            onConfirm = { difficulty, notes ->
                viewModel.finishStopwatch(difficulty, notes)
                showFinishDialog = false
            }
        )
    }
}
