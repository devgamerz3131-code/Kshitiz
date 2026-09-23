package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyPlanLevel
import com.example.data.model.SyllabusChapterEntity
import com.example.ui.theme.RankifyAccent
import com.example.ui.theme.RankifyGlassBorder
import com.example.ui.theme.RankifyPrimary
import com.example.ui.theme.RankifyPurple
import com.example.ui.theme.RankifySoftGradient
import com.example.viewmodel.RankifyViewModel
import kotlinx.coroutines.delay

enum class OnboardingStep {
    LEVEL_SELECTION,
    CHAPTER_PROGRESS_SETUP,
    AI_PLANNING_LOADING
}

@Composable
fun StudyPlannerSetupScreen(
    viewModel: RankifyViewModel,
    onFinished: () -> Unit
) {
    val levels = remember { StudyPlanLevel.LEVELS }
    var selectedLevelId by remember { mutableStateOf(levels.getOrNull(1)?.id ?: levels.first().id) }
    var currentStep by remember { mutableStateOf(OnboardingStep.LEVEL_SELECTION) }

    val currentSelected = levels.firstOrNull { it.id == selectedLevelId } ?: levels.first()
    val uiState by viewModel.uiState.collectAsState()
    val initialChapters = uiState.chapters.ifEmpty { com.example.data.db.DefaultData.initialChapters }

    // Map chapter id to progress option: "Not Started", "Started", "Half Completed", "Mostly Completed", "Completed"
    val chapterProgressMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(initialChapters) {
        initialChapters.forEach { ch ->
            if (!chapterProgressMap.containsKey(ch.id)) {
                chapterProgressMap[ch.id] = when {
                    ch.completedStages >= 6 -> "Completed"
                    ch.completedStages >= 5 -> "Mostly Completed"
                    ch.completedStages >= 3 -> "Half Completed"
                    ch.completedStages >= 1 -> "Started"
                    else -> "Not Started"
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_planner_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "onboardingStepTransition"
        ) { step ->
            when (step) {
                OnboardingStep.LEVEL_SELECTION -> {
                    StudyPlannerLevelSelectionView(
                        levels = levels,
                        selectedLevelId = selectedLevelId,
                        onSelectLevel = { selectedLevelId = it },
                        onNext = {
                            currentStep = OnboardingStep.CHAPTER_PROGRESS_SETUP
                        }
                    )
                }
                OnboardingStep.CHAPTER_PROGRESS_SETUP -> {
                    ChapterProgressSetupView(
                        chapters = initialChapters,
                        chapterProgressMap = chapterProgressMap,
                        onProgressChange = { id, progress ->
                            chapterProgressMap[id] = progress
                        },
                        onMarkAll = { targetProgress ->
                            initialChapters.forEach { ch ->
                                chapterProgressMap[ch.id] = targetProgress
                            }
                        },
                        onBack = {
                            currentStep = OnboardingStep.LEVEL_SELECTION
                        },
                        onCompleteSetup = {
                            // Save chapter progress to repo and schedule cloud sync
                            val updatedChapters = initialChapters.map { ch ->
                                val opt = chapterProgressMap[ch.id] ?: "Not Started"
                                when (opt) {
                                    "Completed" -> ch.copy(conceptsDone = true, ncertReadingDone = true, ncertQuestionsDone = true, pyqDone = true, revisionDone = true, testDone = true)
                                    "Mostly Completed" -> ch.copy(conceptsDone = true, ncertReadingDone = true, ncertQuestionsDone = true, pyqDone = true, revisionDone = true, testDone = false)
                                    "Half Completed" -> ch.copy(conceptsDone = true, ncertReadingDone = true, ncertQuestionsDone = true, pyqDone = false, revisionDone = false, testDone = false)
                                    "Started" -> ch.copy(conceptsDone = true, ncertReadingDone = false, ncertQuestionsDone = false, pyqDone = false, revisionDone = false, testDone = false)
                                    else -> ch.copy(conceptsDone = false, ncertReadingDone = false, ncertQuestionsDone = false, pyqDone = false, revisionDone = false, testDone = false)
                                }
                            }
                            viewModel.saveChapterProgress(updatedChapters)
                            currentStep = OnboardingStep.AI_PLANNING_LOADING
                        }
                    )
                }
                OnboardingStep.AI_PLANNING_LOADING -> {
                    StudyPlannerProgressAnimationView(
                        level = currentSelected,
                        viewModel = viewModel,
                        onCompleted = onFinished
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyPlannerLevelSelectionView(
    levels: List<StudyPlanLevel>,
    selectedLevelId: String,
    onSelectLevel: (String) -> Unit,
    onNext: () -> Unit
) {
    val selectedLevel = levels.firstOrNull { it.id == selectedLevelId } ?: levels.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = RankifyPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RankifyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Study Architect",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = RankifyPrimary
                            )
                        }
                    }

                    Text(
                        text = "Customize Your Study Plan",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Select your current preparation stage. Rankify's AI engine will tailor your daily targets, revision roadmap, and subject focus to your goals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            items(levels) { level ->
                val isSelected = level.id == selectedLevelId
                StudyPlanLevelCard(
                    level = level,
                    isSelected = isSelected,
                    onClick = { onSelectLevel(level.id) }
                )
            }
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("proceed_to_chapters_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                ) {
                    Text(
                        text = "Continue to Chapter Progress Setup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterProgressSetupView(
    chapters: List<SyllabusChapterEntity>,
    chapterProgressMap: Map<String, String>,
    onProgressChange: (String, String) -> Unit,
    onMarkAll: (String) -> Unit,
    onBack: () -> Unit,
    onCompleteSetup: () -> Unit
) {
    val progressOptions = listOf("Not Started", "Started", "Half Completed", "Mostly Completed", "Completed")
    val groupedChapters = chapters.groupBy { it.subject }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapter Progress Setup",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RankifyPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${chapters.size} Chapters",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RankifyPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select your current completion status for each chapter so Rankify AI can build your personalized roadmap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onMarkAll("Not Started") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark All Not Started", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onMarkAll("Completed") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark All Completed", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Chapters List grouped by Subject
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            groupedChapters.forEach { (subject, subChapters) ->
                item(key = "subject_$subject") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RankifyPrimary,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Text(
                                text = subject,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        subChapters.forEach { chapter ->
                            val currentStatus = chapterProgressMap[chapter.id] ?: "Not Started"
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, RankifyGlassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "${chapter.chapterNumber}. ${chapter.title}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Progress selection chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        progressOptions.forEach { option ->
                                            val isSelected = currentStatus == option
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) RankifyPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onProgressChange(chapter.id, option) }
                                                    .padding(vertical = 4.dp),
                                                tonalElevation = if (isSelected) 4.dp else 0.dp
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = option.replace(" Completed", ""),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(0.4f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onCompleteSetup,
                    modifier = Modifier
                        .weight(0.6f)
                        .height(52.dp)
                        .testTag("save_chapters_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                ) {
                    Text(
                        text = "Generate Roadmap",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyPlanLevelCard(
    level: StudyPlanLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RankifyPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) RankifyPrimary else RankifyGlassBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) RankifyPrimary else RankifyPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (level.id) {
                                    "concept_foundation" -> Icons.Default.School
                                    "dual_mastery" -> Icons.Default.Psychology
                                    else -> Icons.Default.MilitaryTech
                                },
                                contentDescription = null,
                                tint = if (isSelected) Color.White else RankifyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = level.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = level.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = RankifyPrimary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.TrackChanges, contentDescription = null, tint = RankifyPrimary, modifier = Modifier.size(16.dp))
                        Column {
                            Text(text = "Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${level.recommendedTargetPercent}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = RankifyPrimary, modifier = Modifier.size(16.dp))
                        Column {
                            Text(text = "Daily Study", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${level.recommendedGoalMinutes / 60}h ${level.recommendedGoalMinutes % 60}m", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium full-screen loading page with animation.
 * Title: "Rankify AI is Planning Your Study Journey"
 * Subtitle: "Analyzing your syllabus, current progress, strengths and weak chapters to create your personalized study roadmap."
 */
@Composable
private fun StudyPlannerProgressAnimationView(
    level: StudyPlanLevel,
    viewModel: RankifyViewModel,
    onCompleted: () -> Unit
) {
    val steps = remember {
        listOf(
            "Analyzing completed vs incomplete chapters...",
            "Identifying weak and strong areas...",
            "Creating personalized study roadmap...",
            "Generating initial daily and weekly targets...",
            "Storing generated plan in Firebase...",
            "Finalizing your AI Study Journey..."
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var targetProgress by remember { mutableFloatStateOf(0.15f) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(Unit) {
        // Step 1
        currentStepIndex = 0
        targetProgress = 0.25f
        delay(450)

        // Step 2
        currentStepIndex = 1
        targetProgress = 0.45f
        delay(450)

        // Step 3
        currentStepIndex = 2
        targetProgress = 0.65f
        viewModel.applyStudyPlan(
            level = level,
            targetPercentage = level.recommendedTargetPercent,
            dailyGoalMinutes = level.recommendedGoalMinutes
        )
        delay(450)

        // Step 4
        currentStepIndex = 3
        targetProgress = 0.85f
        delay(400)

        // Step 5
        currentStepIndex = 4
        targetProgress = 0.95f
        delay(350)

        // Step 6 (Complete)
        currentStepIndex = 5
        targetProgress = 1.0f
        delay(300)

        // Automatically navigate to Home screen
        onCompleted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        RankifyPrimary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated AI Core Ring
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(RankifySoftGradient),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Planning",
                        tint = RankifyPrimary,
                        modifier = Modifier
                            .size(44.dp)
                            .rotate(rotation)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Rankify AI is Planning Your Study Journey",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Analyzing your syllabus, current progress, strengths and weak chapters to create your personalized study roadmap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Linear Progress Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = RankifyPrimary,
                    trackColor = RankifyPrimary.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Optimizing Roadmap...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RankifyPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Indicator Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val currentStepText = steps.getOrElse(currentStepIndex) { steps.last() }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = RankifyPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentStepText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
