package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyllabusChapterEntity
import com.example.ui.theme.RankifyGlassBorder
import com.example.ui.theme.RankifyPrimary
import com.example.viewmodel.RankifyViewModel
import kotlinx.coroutines.launch

@Composable
fun ChapterProgressSetupScreen(
    viewModel: RankifyViewModel,
    onFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val chapters = uiState.chapters.ifEmpty { com.example.data.db.DefaultData.initialChapters }
    val scope = rememberCoroutineScope()

    // Map chapter id to progress option: "Not Started", "Started", "Half", "Mostly", "Completed"
    val progressOptions = listOf("Not Started", "Started", "Half", "Mostly", "Completed")
    val chapterProgressMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(chapters) {
        chapters.forEach { ch ->
            if (!chapterProgressMap.containsKey(ch.id)) {
                chapterProgressMap[ch.id] = when {
                    ch.completedStages >= 6 -> "Completed"
                    ch.completedStages >= 5 -> "Mostly"
                    ch.completedStages >= 3 -> "Half"
                    ch.completedStages >= 1 -> "Started"
                    else -> "Not Started"
                }
            }
        }
    }

    val groupedChapters = chapters.groupBy { it.subject }

    // Calculate subject progress stats
    val subjectStats = remember(chapterProgressMap.values.toList(), chapters) {
        val stats = mutableListOf<SubjectCompletionStat>()
        val allSubjects = listOf("Physics", "Chemistry", "Mathematics")
        
        allSubjects.forEach { subj ->
            val subjChapters = chapters.filter { it.subject.equals(subj, ignoreCase = true) }
            if (subjChapters.isNotEmpty()) {
                val totalPoints = subjChapters.sumOf { ch ->
                    when (chapterProgressMap[ch.id] ?: "Not Started") {
                        "Completed" -> 100
                        "Mostly" -> 75
                        "Half" -> 50
                        "Started" -> 25
                        else -> 0
                    }
                }
                val pct = totalPoints.toFloat() / (subjChapters.size * 100f) * 100f
                val completedCount = subjChapters.count { chapterProgressMap[it.id] == "Completed" }
                val color = when (subj.lowercase()) {
                    "physics" -> Color(0xFF6366F1) // Indigo
                    "chemistry" -> Color(0xFF10B981) // Emerald
                    else -> Color(0xFFF59E0B) // Amber
                }
                stats.add(
                    SubjectCompletionStat(
                        subject = subj,
                        percentage = pct,
                        completedChapters = completedCount,
                        totalChapters = subjChapters.size,
                        color = color
                    )
                )
            }
        }
        stats
    }

    val overallPercentage = remember(subjectStats) {
        if (subjectStats.isEmpty()) 0f else subjectStats.map { it.percentage }.average().toFloat()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chapter_progress_setup_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
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
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RankifyPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = RankifyPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Class 12 CBSE Syllabus Setup",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

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
                        text = "Select your current completion status for each chapter. State is securely stored in Firestore to personalize your AI study roadmap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bulk Actions / Shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                chapters.forEach { ch ->
                                    chapterProgressMap[ch.id] = "Not Started"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark All Not Started", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                chapters.forEach { ch ->
                                    chapterProgressMap[ch.id] = "Completed"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark All Completed", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Summary Chart & Chapters List grouped by Subject
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Data Visualization Chart Item
                item(key = "syllabus_summary_chart") {
                    SyllabusCompletionSummaryChart(
                        subjectStats = subjectStats,
                        overallPercentage = overallPercentage
                    )
                }

                groupedChapters.forEach { (subject, subChapters) ->
                    item(key = "subject_$subject") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dotColor = when (subject.lowercase()) {
                                    "physics" -> Color(0xFF6366F1)
                                    "chemistry" -> Color(0xFF10B981)
                                    else -> Color(0xFFF59E0B)
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = dotColor,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                val stat = subjectStats.find { it.subject.equals(subject, ignoreCase = true) }
                                if (stat != null) {
                                    Text(
                                        text = "${stat.percentage.toInt()}% Covered",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = dotColor
                                    )
                                }
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

                                        // Selection Chips
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
                                                        .clickable { chapterProgressMap[chapter.id] = option }
                                                        .padding(vertical = 4.dp),
                                                    tonalElevation = if (isSelected) 4.dp else 0.dp
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = option,
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

            // Bottom Bar
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
                        onClick = {
                            scope.launch {
                                val updatedChapters = chapters.map { ch ->
                                    val opt = chapterProgressMap[ch.id] ?: "Not Started"
                                    when (opt) {
                                        "Completed" -> ch.copy(conceptsDone = true, ncertReadingDone = true, ncertQuestionsDone = true, pyqDone = true, revisionDone = true, testDone = true)
                                        "Mostly" -> ch.copy(conceptsDone = true, ncertReadingDone = true, ncertQuestionsDone = true, pyqDone = true, revisionDone = true, testDone = false)
                                        "Half" -> ch.copy(conceptsDone = true, ncertReadingDone = true, ncertQuestionsDone = true, pyqDone = false, revisionDone = false, testDone = false)
                                        "Started" -> ch.copy(conceptsDone = true, ncertReadingDone = false, ncertQuestionsDone = false, pyqDone = false, revisionDone = false, testDone = false)
                                        else -> ch.copy(conceptsDone = false, ncertReadingDone = false, ncertQuestionsDone = false, pyqDone = false, revisionDone = false, testDone = false)
                                    }
                                }
                                viewModel.saveChapterProgress(updatedChapters)
                                onFinished()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_chapter_progress_screen_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                    ) {
                        Text(
                            text = "Save State & Continue to Roadmap",
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
}

/**
 * Data model representing subject-wise completion statistics for chart visualization.
 */
data class SubjectCompletionStat(
    val subject: String,
    val percentage: Float,
    val completedChapters: Int,
    val totalChapters: Int,
    val color: Color
)

/**
 * Native Jetpack Compose Data Visualization Chart for Syllabus Completion Summary.
 * Visualizes completion across Physics, Chemistry, and Mathematics with animated bar charts,
 * gauge metrics, and comparative breakdown cards.
 */
@Composable
fun SyllabusCompletionSummaryChart(
    subjectStats: List<SubjectCompletionStat>,
    overallPercentage: Float,
    modifier: Modifier = Modifier
) {
    val animatedOverall by animateFloatAsState(
        targetValue = overallPercentage,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "overall_pct"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("syllabus_completion_summary_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, RankifyGlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chart Header with Overall Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Syllabus Chart",
                        tint = RankifyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Syllabus Progress Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RankifyPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${animatedOverall.toInt()}% Total",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RankifyPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Custom Multi-Bar Chart Canvas Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val bottomY = height - 24.dp.toPx()
                    val chartHeight = bottomY - 10.dp.toPx()

                    // Draw subtle grid lines at 25%, 50%, 75%, 100%
                    listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { fraction ->
                        val y = bottomY - (chartHeight * fraction)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.25f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Bars for each subject
                    if (subjectStats.isNotEmpty()) {
                        val barCount = subjectStats.size
                        val spacing = width / barCount
                        val barWidth = 32.dp.toPx().coerceAtMost(spacing * 0.5f)

                        subjectStats.forEachIndexed { index, stat ->
                            val centerX = (index + 0.5f) * spacing
                            val barLeft = centerX - (barWidth / 2f)
                            val normalizedPct = (stat.percentage / 100f).coerceIn(0f, 1f)
                            val barActualHeight = (chartHeight * normalizedPct).coerceAtLeast(4.dp.toPx())
                            val barTop = bottomY - barActualHeight

                            // Background Track
                            drawRoundRect(
                                color = stat.color.copy(alpha = 0.15f),
                                topLeft = Offset(barLeft, bottomY - chartHeight),
                                size = Size(barWidth, chartHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // Active Progress Fill (Gradient)
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        stat.color,
                                        stat.color.copy(alpha = 0.75f)
                                    ),
                                    startY = barTop,
                                    endY = bottomY
                                ),
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barActualHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Subject Breakdown Rows with Progress Indicators
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                subjectStats.forEach { stat ->
                    val animatedSubjectPct by animateFloatAsState(
                        targetValue = stat.percentage,
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "subj_pct_${stat.subject}"
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = stat.color,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = stat.subject,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${animatedSubjectPct.toInt()}% (${stat.completedChapters}/${stat.totalChapters} ch)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Linear progress bar
                        LinearProgressIndicator(
                            progress = { (animatedSubjectPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = stat.color,
                            trackColor = stat.color.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

