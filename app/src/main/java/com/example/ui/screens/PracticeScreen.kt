package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.MistakeEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.TestAttemptEntity
import com.example.ui.components.RankifyProgressRing
import com.example.ui.components.getSubjectColor
import com.example.ui.theme.RankifyAccent
import com.example.ui.theme.RankifyGlassBorder
import com.example.ui.theme.RankifyGlassSurface
import com.example.ui.theme.RankifyPrimary
import com.example.ui.theme.RankifyPurple
import com.example.viewmodel.RankifyUiState
import com.example.viewmodel.RankifyViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    val publishedMaterials by viewModel.publishedStudyMaterials.collectAsState()
    var isFolderViewOpen by remember { mutableStateOf(false) }

    // Ensure published study materials are loaded for student folder access
    LaunchedEffect(Unit) {
        viewModel.loadPublishedStudyMaterials()
    }

    if (isFolderViewOpen) {
        PracticeQuestionBankFolderView(
            publishedMaterials = publishedMaterials,
            chapters = state.chapters,
            viewModel = viewModel,
            onCloseFolder = { isFolderViewOpen = false }
        )
    } else {
        var selectedSection by remember { mutableIntStateOf(0) } // 0: Question Bank, 1: Bookmarks & Mistakes, 2: Mock Tests
        val sections = listOf("Question Bank", "Bookmarks & Mistakes", "Tests")

        Column(modifier = Modifier.fillMaxSize()) {
            // Compact Entry: Subject-wise Question Bank Folders
            Card(
                onClick = { isFolderViewOpen = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("practice_question_bank_folder_entry")
            ) {
                Row(
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Question Bank Folders",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Question Bank",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RankifyPrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = "PDF FOLDERS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = RankifyPrimary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Physics • Chemistry • Mathematics Chapter Folders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RankifyPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Folders",
                            tint = RankifyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

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
                0 -> QuestionBankSection(state, viewModel, onOpenAiWithPrompt)
                1 -> BookmarksAndMistakesSection(state, viewModel, onOpenAiWithPrompt)
                2 -> CustomTestsSection(state, viewModel)
            }
        }
    }
}

@Composable
fun QuestionBankSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    var subjectFilter by remember { mutableStateOf("All") }
    val subjects = listOf("All", "Physics", "Chemistry", "Mathematics")

    val filteredQuestions = if (subjectFilter == "All") {
        state.questions
    } else {
        state.questions.filter { it.subject == subjectFilter }
    }

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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { sub ->
                    FilterChip(
                        selected = subjectFilter == sub,
                        onClick = { subjectFilter = sub },
                        label = { Text(sub, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        items(filteredQuestions, key = { it.id }) { question ->
            QuestionCard(
                question = question,
                onToggleBookmark = { viewModel.toggleQuestionBookmark(question) },
                onRecordMistake = { category ->
                    viewModel.recordMistake(question, category)
                },
                onRecordAttempt = {
                    viewModel.recordQuestionAttempt(question)
                },
                onAskAiHelp = {
                    onOpenAiWithPrompt(
                        "Ye question samjhaiye step-by-step with formulas: ${question.questionText}",
                        question.subject,
                        question.chapter
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuestionCard(
    question: QuestionEntity,
    onToggleBookmark: () -> Unit,
    onRecordMistake: (String) -> Unit,
    onRecordAttempt: () -> Unit,
    onAskAiHelp: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showSolution by remember { mutableStateOf(false) }
    var showMistakeDialog by remember { mutableStateOf(false) }

    val options = remember(question.optionsListJson) {
        if (question.optionsListJson.contains("|")) {
            question.optionsListJson.split("|")
        } else {
            emptyList()
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RankifyGlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("question_card_${question.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Subject, Chapter & Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getSubjectColor(question.subject).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${question.subject} • ${question.questionType}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = getSubjectColor(question.subject),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = question.chapter,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (question.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (question.isBookmarked) RankifyPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Question Statement
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))
            // MCQ Options
            if (options.isNotEmpty()) {
                options.forEach { option ->
                    val isSelected = selectedOption == option
                    val isCorrect = option == question.correctAnswer

                    val bgColor = when {
                        !isAnswerSubmitted && isSelected -> RankifyPrimary.copy(alpha = 0.1f)
                        isAnswerSubmitted && isCorrect -> Color(0xFFDCFCE7)
                        isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFEE2E2)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    }

                    val borderColor = when {
                        !isAnswerSubmitted && isSelected -> RankifyPrimary
                        isAnswerSubmitted && isCorrect -> Color(0xFF16A34A)
                        isAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFDC2626)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isAnswerSubmitted) {
                                selectedOption = option
                            }
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { showHint = !showHint }) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = Color(0xFFD97706)
                        )
                    }
                    IconButton(onClick = { showSolution = !showSolution }) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Solution",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAskAiHelp) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask AI",
                            tint = RankifyPrimary
                        )
                    }
                }

                if (!isAnswerSubmitted && selectedOption != null) {
                    Button(
                        onClick = {
                            isAnswerSubmitted = true
                            onRecordAttempt()
                            if (selectedOption != question.correctAnswer) {
                                showMistakeDialog = true
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                    ) {
                        Text("Submit Answer", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            AnimatedVisibility(visible = showHint) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "💡 Hint: ${question.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showSolution) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Correct Answer: ${question.correctAnswer}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RankifyPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.solutionExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showMistakeDialog) {
        AlertDialog(
            onDismissRequest = { showMistakeDialog = false },
            title = { Text("Log Mistake to AI Memory", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rankify tracks mistakes so AI can recommend targeted revision. What caused this mistake?", style = MaterialTheme.typography.bodySmall)
                    val categories = listOf(
                        "Concept error",
                        "Formula error",
                        "Calculation error",
                        "Careless mistake",
                        "Didn't know",
                        "Time pressure"
                    )
                    categories.forEach { cat ->
                        OutlinedButton(
                            onClick = {
                                onRecordMistake(cat)
                                showMistakeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { showMistakeDialog = false }) {
                    Text("Skip")
                }
            }
        )
    }
}

@Composable
fun BookmarksAndMistakesSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    onOpenAiWithPrompt: (String, String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saved Bookmarked Questions (${state.bookmarkedQuestions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (state.bookmarkedQuestions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No saved questions yet. Tap bookmark on any question to review later.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(state.bookmarkedQuestions) { q ->
                QuestionCard(
                    question = q,
                    onToggleBookmark = { viewModel.toggleQuestionBookmark(q) },
                    onRecordMistake = { cat -> viewModel.recordMistake(q, cat) },
                    onRecordAttempt = { viewModel.recordQuestionAttempt(q) },
                    onAskAiHelp = {
                        onOpenAiWithPrompt("Solve this saved question: ${q.questionText}", q.subject, q.chapter)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Recorded Mistakes (${state.mistakes.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (state.mistakes.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No mistakes recorded yet. Keep practicing questions and tests!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.mistakes) { m ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = m.mistakeCategory,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${m.subject} • ${m.chapter}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = getSubjectColor(m.subject)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = m.questionSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
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
}

@Composable
fun CustomTestsSection(
    state: RankifyUiState,
    viewModel: RankifyViewModel
) {
    var isTestActive by remember { mutableStateOf(false) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var testFinished by remember { mutableStateOf(false) }
    var lastScore by remember { mutableIntStateOf(0) }

    val testQuestions = state.questions.take(5)

    if (isTestActive && testQuestions.isNotEmpty() && !testFinished) {
        val q = testQuestions[currentQuestionIndex]
        val selected = userAnswers[currentQuestionIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentQuestionIndex + 1} of ${testQuestions.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RankifyPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "⏱️ Active Test",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RankifyPrimary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = getSubjectColor(q.subject).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${q.subject} • ${q.chapter}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = getSubjectColor(q.subject),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        val options = if (q.optionsListJson.contains("|")) q.optionsListJson.split("|") else emptyList()
                        options.forEach { opt ->
                            val isChosen = selected == opt
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) RankifyPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) RankifyPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val map = userAnswers.toMutableMap()
                                        map[currentQuestionIndex] = opt
                                        userAnswers = map
                                    }
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isChosen) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentQuestionIndex > 0) currentQuestionIndex--
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = currentQuestionIndex > 0
                ) {
                    Text("Previous")
                }

                if (currentQuestionIndex < testQuestions.size - 1) {
                    Button(
                        onClick = { currentQuestionIndex++ },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                    ) {
                        Text("Next Question", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    Button(
                        onClick = {
                            var correct = 0
                            var wrong = 0
                            testQuestions.forEachIndexed { idx, question ->
                                if (userAnswers[idx] == question.correctAnswer) {
                                    correct++
                                } else {
                                    wrong++
                                }
                            }
                            val score = correct * 4 // 4 marks per correct
                            val max = testQuestions.size * 4
                            lastScore = score
                            testFinished = true

                            viewModel.submitTest(
                                testTitle = "CBSE Class 12 PCM Sprint Test",
                                subject = "Physics & Chemistry",
                                totalQuestions = testQuestions.size,
                                correctCount = correct,
                                wrongCount = wrong,
                                score = score,
                                maxScore = max,
                                timeTakenMinutes = 12
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Submit Test", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    } else if (testFinished) {
        val maxScore = testQuestions.size * 4
        val scoreFraction = (lastScore.toFloat() / maxScore.coerceAtLeast(1)).coerceIn(0f, 1f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Score Progress Ring
            RankifyProgressRing(
                progress = scoreFraction,
                size = 140.dp,
                strokeWidth = 9.dp,
                startColor = if (scoreFraction >= 0.6f) Color(0xFF10B981) else RankifyPrimary,
                endColor = if (scoreFraction >= 0.6f) Color(0xFF059669) else RankifyPurple,
                centerContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$lastScore",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "out of $maxScore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Test Completed! 🌟",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Test Analysis:",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = RankifyPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Strong: Colligative properties & Matrix inversion\n• Needs Attention: Kirchhoff Loop Rule mesh equations\n• Recommended Action: Revisit formula card and attempt 3 numericals.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    isTestActive = false
                    testFinished = false
                    userAnswers = mutableMapOf()
                    currentQuestionIndex = 0
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
            ) {
                Text("Back to Test Menu", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
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
                        Text(
                            text = "CBSE Class 12 PCM Sprint Test",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "5 Questions • 20 Marks • Timer: 15 Mins • Instant AI Analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isTestActive = true
                                testFinished = false
                                userAnswers = mutableMapOf()
                                currentQuestionIndex = 0
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Test")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Test", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Past Test History (${state.testAttempts.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.testAttempts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, RankifyGlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No tests taken yet. Start the sprint test above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(state.testAttempts) { attempt ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, RankifyGlassBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = attempt.testTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${attempt.score} / ${attempt.maxScore}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RankifyPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Correct: ${attempt.correctCount} • Wrong: ${attempt.wrongCount} • Time: ${attempt.timeTakenMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (attempt.aiAnalysisFeedback.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "AI: ${attempt.aiAnalysisFeedback}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RankifyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
