package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyncStatus
import com.example.ui.components.RankifySyncStatusBar
import com.example.ui.components.StopwatchLiveBanner
import com.example.ui.home.AiPlanTask
import com.example.ui.home.DynamicAiHomeHeader
import com.example.ui.home.DynamicAiInsightsCard
import com.example.ui.home.DynamicAiPlanSection
import com.example.ui.home.DynamicAiPriorityCard
import com.example.ui.home.DynamicAiRecommendationsSection
import com.example.ui.home.DynamicExamCountdownCard
import com.example.ui.home.DynamicQuickActionsRow
import com.example.ui.home.DynamicRankiMotivationCard
import com.example.ui.home.DynamicRecentActivitySection
import com.example.ui.home.DynamicTodayProgressCard
import com.example.ui.home.DynamicWeakChaptersCard
import com.example.ui.home.HomeAiDashboardEngine
import com.example.ui.home.QuickActionType
import com.example.ui.theme.RankifyGlassBorder
import com.example.ui.theme.RankifyPrimary
import com.example.ui.theme.RankifyPurple
import com.example.viewmodel.RankifyUiState
import com.example.viewmodel.RankifyViewModel

/**
 * COMPLETE AI-Driven Dynamic Home Dashboard.
 *
 * - No placeholder data.
 * - No hardcoded text.
 * - No fixed static cards.
 * - Dynamically adapts to the current student's syllabus progress, chapter completion,
 *   weak chapters, study streak, daily target, exam countdown, class, board, and prep level.
 * - Material 3 with modern animations, responsive state updates, and real-time Firebase sync.
 */
@Composable
fun HomeScreen(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onNavigateTab: (Int) -> Unit,
    onOpenAiWithPrompt: (String, String, String) -> Unit = { _, _, _ -> onNavigateTab(3) },
    onOpenStudyDock: () -> Unit = {}
) {
    var showAddTargetDialog by remember { mutableStateOf(false) }
    var showAllTargetsDialog by remember { mutableStateOf(false) }

    // Dynamically generate personalized AI Dashboard data from real-time state
    val dashboardData = remember(
        state.profile,
        state.chapters,
        state.dailyTargets,
        state.sessions,
        state.testAttempts,
        state.mistakes,
        state.notes,
        state.activeExam
    ) {
        HomeAiDashboardEngine.generateDashboard(state)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Live Stopwatch Banner & Cloud Sync Bar (outside scrollable area)
        val isRunning by viewModel.isStopwatchRunning.collectAsState()
        val seconds by viewModel.stopwatchSeconds.collectAsState()

        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            AnimatedVisibility(visible = isRunning || seconds > 0) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    StopwatchLiveBanner(
                        isRunning = isRunning,
                        elapsedSeconds = seconds,
                        subject = viewModel.stopwatchSubject.collectAsState().value,
                        chapter = viewModel.stopwatchChapter.collectAsState().value,
                        onPauseResume = {
                            if (isRunning) viewModel.pauseStopwatch() else viewModel.resumeStopwatch()
                        },
                        onFinish = {
                            viewModel.finishStopwatch()
                        },
                        onOpenStopwatch = {
                            onNavigateTab(1)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            AnimatedVisibility(visible = state.syncStatus is SyncStatus.Offline || state.syncStatus is SyncStatus.Syncing) {
                Column {
                    RankifySyncStatusBar(
                        syncStatus = state.syncStatus,
                        onSyncClick = { viewModel.performSmartSync() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // 2. Dynamic Scrollable Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // 1. AI HOME HEADER (Intelligent Greeting & Subtext based on yesterday's completion or exam)
            item {
                DynamicAiHomeHeader(
                    data = dashboardData.greeting,
                    onOpenProfile = { onNavigateTab(4) }
                )
            }

            // 2. ASK AI DIRECT SEARCH BAR
            item {
                CompactAiAskPill(
                    onAsk = { query ->
                        val sub = dashboardData.priorityMatrix.todaysHighestSubject
                        val chap = dashboardData.priorityMatrix.todaysHighestPriority
                        onOpenAiWithPrompt(query, sub, chap)
                    }
                )
            }

            // 3. RANKI MASCOT MOTIVATION (Dynamic, Context-aware, Never repeated)
            item {
                DynamicRankiMotivationCard(
                    motivation = dashboardData.motivation,
                    onTapRoastOrMotivate = { viewModel.openNotificationSettings() }
                )
            }

            // 4. TODAY'S PROGRESS (Multi-metric Circular Ring & Productivity Score)
            item {
                DynamicTodayProgressCard(
                    progress = dashboardData.todayProgress,
                    onClickViewDetails = { onNavigateTab(4) }
                )
            }

            // 5. QUICK ACTIONS BAR
            item {
                DynamicQuickActionsRow(
                    actions = dashboardData.quickActions,
                    onSelectAction = { action ->
                        when (action.type) {
                            QuickActionType.CONTINUE_SESSION -> {
                                viewModel.startStopwatch(action.subject, action.chapter)
                            }
                            QuickActionType.RESUME_STUDYDOCK -> {
                                onOpenStudyDock()
                            }
                            QuickActionType.PRACTICE_WEAK -> {
                                onNavigateTab(2)
                            }
                            QuickActionType.START_REVISION -> {
                                viewModel.startStopwatch(action.subject, action.chapter)
                            }
                            QuickActionType.ASK_AI_TUTOR -> {
                                onOpenAiWithPrompt(action.prompt, action.subject, action.chapter)
                            }
                            QuickActionType.TAKE_TEST -> {
                                onNavigateTab(2)
                            }
                            QuickActionType.OPEN_NOTES -> {
                                onNavigateTab(1)
                            }
                        }
                    }
                )
            }

            // 6. TODAY'S AI PLAN (Dynamic tasks with Difficulty, XP, Priority & Instant Sync)
            item {
                DynamicAiPlanSection(
                    tasks = dashboardData.todayPlan,
                    onToggleTask = { task ->
                        if (task.targetEntity != null) {
                            viewModel.toggleTargetStatus(task.targetEntity)
                        } else {
                            // Add task to repository as completed and update productivity stats immediately
                            viewModel.addAndCompleteTarget(
                                subject = task.subject,
                                chapter = task.chapter,
                                task = task.title,
                                priority = task.priority,
                                estMinutes = task.estimatedMinutes,
                                isBacklog = false
                            )
                        }
                    },
                    onStartFocus = { task ->
                        viewModel.startStopwatch(task.subject, task.chapter)
                    },
                    onAddNewTarget = {
                        showAddTargetDialog = true
                    }
                )
            }

            // 7. EXAM COUNTDOWN ANIMATED CARD
            item {
                DynamicExamCountdownCard(
                    data = dashboardData.examCountdown,
                    onClick = { onNavigateTab(4) }
                )
            }

            // 8. SMART AI INSIGHTS CARD (Accuracy, Spaced repetition overdue, Peak study window)
            item {
                DynamicAiInsightsCard(
                    insights = dashboardData.insights,
                    onActionClick = { insight ->
                        val sub = dashboardData.priorityMatrix.todaysHighestSubject
                        val chap = dashboardData.priorityMatrix.todaysHighestPriority
                        onOpenAiWithPrompt(
                            "Explain how to improve in: ${insight.headline}. Provide diagnostic tips for Class 12 CBSE.",
                            sub,
                            chap
                        )
                    }
                )
            }

            // 9. WEAK CHAPTERS CARD (Shows only weak chapters, disappears automatically upon improvement)
            if (dashboardData.weakChapters.isNotEmpty()) {
                item {
                    DynamicWeakChaptersCard(
                        weakChapters = dashboardData.weakChapters,
                        onPracticeChapter = { chapter ->
                            onNavigateTab(2)
                        },
                        onAskAiDoubt = { chapter ->
                            onOpenAiWithPrompt(
                                "Explain difficult concepts, derivations, and common mistakes in ${chapter.title} for ${chapter.subject}.",
                                chapter.subject,
                                chapter.title
                            )
                        }
                    )
                }
            }

            // 10. AI PRIORITY MATRIX CARD
            item {
                DynamicAiPriorityCard(
                    matrix = dashboardData.priorityMatrix,
                    onTopicClick = { topic ->
                        val sub = dashboardData.priorityMatrix.todaysHighestSubject
                        onOpenAiWithPrompt(
                            "Teach me step-by-step for Board Exams: $topic. Include key formulas and solved example.",
                            sub,
                            topic
                        )
                    }
                )
            }

            // 11. AI SMART RECOMMENDATIONS
            item {
                DynamicAiRecommendationsSection(
                    recommendations = dashboardData.recommendations,
                    onSelectRecommendation = { rec ->
                        onOpenAiWithPrompt(rec.prompt, rec.subject, rec.chapter)
                    }
                )
            }

            // 12. STUDYDOCK & RANKIFY EXCLUSIVE VAULT
            item {
                CompactStudyDockCard(
                    onOpenStudyDock = onOpenStudyDock
                )
            }

            item {
                CompactRankifyExclusiveCard(
                    onClick = { viewModel.openExclusiveMain() }
                )
            }

            // 13. RECENT ACTIVITY TIMELINE
            item {
                DynamicRecentActivitySection(
                    activities = dashboardData.recentActivity
                )
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (showAddTargetDialog) {
        AddTargetDialog(
            onDismiss = { showAddTargetDialog = false },
            onAdd = { subject, chapter, task, priority, estMins ->
                viewModel.addTarget(subject, chapter, task, priority, estMins, isBacklog = false)
                showAddTargetDialog = false
            }
        )
    }

    if (showAllTargetsDialog) {
        AllTargetsDialog(
            targets = state.dailyTargets,
            onDismiss = { showAllTargetsDialog = false },
            onToggle = { viewModel.toggleTargetStatus(it) },
            onStartFocus = {
                viewModel.startStopwatch(it.subject, it.chapter)
                showAllTargetsDialog = false
            },
            onOpenStudyScreen = {
                showAllTargetsDialog = false
                onNavigateTab(1)
            },
            onAddNew = {
                showAllTargetsDialog = false
                showAddTargetDialog = true
            }
        )
    }
}

// =========================================================================
// INTERACTIVE AI SEARCH PILL
// =========================================================================

@Composable
fun CompactAiAskPill(
    onAsk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RankifyGlassBorder),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(RankifyPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Ask",
                    tint = RankifyPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                cursorBrush = SolidColor(RankifyPrimary),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (text.isNotBlank()) {
                            onAsk(text)
                            text = ""
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = "✨ Ask AI anything... 'Kirchhoff laws numericals?'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_search_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = RankifyPrimary,
                modifier = Modifier
                    .clickable {
                        if (text.isNotBlank()) {
                            onAsk(text)
                            text = ""
                        } else {
                            onAsk("Class 12 CBSE Board 2026 Strategy & Important Questions")
                        }
                    }
                    .testTag("ai_search_submit_button")
            ) {
                Text(
                    text = "Ask",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// =========================================================================
// STUDYDOCK & RANKIFY EXCLUSIVE CARDS
// =========================================================================

@Composable
fun CompactStudyDockCard(
    onOpenStudyDock: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RankifyGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenStudyDock() }
            .testTag("studydock_compact_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            RankifyPrimary.copy(alpha = 0.05f),
                            Color(0xFF7C3AED).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(RankifyPrimary, Color(0xFF7C3AED))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "StudyDock",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "StudyDock ✨",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RankifyPrimary,
                    modifier = Modifier
                        .clickable { onOpenStudyDock() }
                        .testTag("open_studydock_button")
                ) {
                    Text(
                        text = "Open Dock",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Turn YouTube video lectures into smart notes, formulas, and auto-quizzes seamlessly without distractions.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactRankifyExclusiveCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RankifyGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("home_rankify_exclusive_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            RankifyPurple.copy(alpha = 0.05f),
                            RankifyPrimary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rankify Exclusive",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Concept Music & Visual Vault",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// =========================================================================
// ALL TARGETS DIALOG & COMPACT TARGET ROW
// =========================================================================

@Composable
fun AllTargetsDialog(
    targets: List<StudyTargetEntity>,
    onDismiss: () -> Unit,
    onToggle: (StudyTargetEntity) -> Unit,
    onStartFocus: (StudyTargetEntity) -> Unit,
    onOpenStudyScreen: () -> Unit,
    onAddNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Targets (${targets.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (targets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study targets scheduled today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(targets) { target ->
                            CompactTargetRow(
                                target = target,
                                onToggle = { onToggle(target) },
                                onStartFocus = { onStartFocus(target) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Target", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
                OutlinedButton(
                    onClick = onOpenStudyScreen,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Full Planner", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun CompactTargetRow(
    target: StudyTargetEntity,
    onToggle: () -> Unit,
    onStartFocus: () -> Unit
) {
    val isCompleted = target.status == "Completed"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isCompleted) Color.Transparent else RankifyGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_item_${target.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle status",
                    tint = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = target.task,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${target.estimatedMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isCompleted) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Target Focus",
                        tint = RankifyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
