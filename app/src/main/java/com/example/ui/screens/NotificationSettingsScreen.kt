package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.RankifyDatabase
import com.example.data.model.NotificationHistoryEntity
import com.example.notifications.NotificationPreferencesManager
import com.example.notifications.NotificationScheduler
import com.example.notifications.SmartNotificationEngine
import com.example.notifications.model.NotificationIntelligenceScore
import com.example.notifications.model.NotificationPersonality
import com.example.notifications.model.NotificationPreferences
import com.example.ui.components.RankiMascotAvatar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { NotificationPreferencesManager.getInstance(context) }
    var prefs by remember { mutableStateOf(prefsManager.getPreferences()) }
    var intelligenceScore by remember {
        mutableStateOf(
            NotificationIntelligenceScore(
                totalScore = 72,
                consistencyScore = 18,
                studyTimeScore = 17,
                targetDisciplineScore = 19,
                examProximityScore = 18,
                recommendedTiming = "Peak Focus recommended at 6:00 PM",
                recommendedTone = "Disciplined Mastery",
                description = "Calculating live intelligence score..."
            )
        )
    }

    var showHistorySheet by remember { mutableStateOf(false) }
    var showDeveloperTestingScreen by remember { mutableStateOf(false) }
    val db = remember { RankifyDatabase.getDatabase(context) }
    val historyList by db.rankifyDao().getAllNotificationHistory().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        intelligenceScore = SmartNotificationEngine.getIntelligenceScore(context)
    }

    if (showDeveloperTestingScreen) {
        DeveloperNotificationTestingScreen(
            onNavigateBack = { showDeveloperTestingScreen = false }
        )
        return
    }

    fun updatePrefs(newPrefs: NotificationPreferences) {
        prefs = newPrefs
        prefsManager.savePreferences(newPrefs)
        if (newPrefs.masterEnabled) {
            NotificationScheduler.schedulePeriodicWork(context)
        } else {
            NotificationScheduler.cancelAll(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Notifications",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Personal AI Study Buddy",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notification_settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("notification_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View History"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("notification_settings_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Intelligence Score Card
            item {
                NotificationIntelligenceScoreCard(score = intelligenceScore)
            }

            // 2. Master Enable Card
            item {
                MasterNotificationCard(
                    enabled = prefs.masterEnabled,
                    onToggle = { updatePrefs(prefs.copy(masterEnabled = it)) }
                )
            }

            // 3. Personality Selector (Only visible if master enabled)
            if (prefs.masterEnabled) {
                item {
                    PersonalitySelectorSection(
                        selectedPersonality = prefs.personality,
                        onSelectPersonality = { updatePrefs(prefs.copy(personality = it)) },
                        onTestNotification = {
                            SmartNotificationEngine.sendTestNotification(context, prefs.personality)
                            Toast.makeText(
                                context,
                                "Test notification sent in ${prefs.personality.displayName} tone!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                // 4. Smart Reminders Categories
                item {
                    Text(
                        text = "Intelligent Reminder Schedules",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.WbSunny,
                        iconTint = Color(0xFFE65100),
                        title = "Morning Kickstart (7:00 – 9:00 AM)",
                        subtitle = "Motivates you to start early. Only sent if no study has started.",
                        checked = prefs.morningReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(morningReminderEnabled = it)) },
                        testTag = "toggle_morning_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.School,
                        iconTint = Color(0xFF1976D2),
                        title = "After-School Sprint (2:00 – 5:00 PM)",
                        subtitle = "Timed for school return to finish homework and start revision.",
                        checked = prefs.afterSchoolReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(afterSchoolReminderEnabled = it)) },
                        testTag = "toggle_after_school_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.Timer,
                        iconTint = Color(0xFF673AB7),
                        title = "Evening Focus (6:00 – 8:00 PM)",
                        subtitle = "Reminds you to finish pending daily goals. Only if target incomplete.",
                        checked = prefs.eveningReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(eveningReminderEnabled = it)) },
                        testTag = "toggle_evening_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.Bedtime,
                        iconTint = Color(0xFF3F51B5),
                        title = "Night Check-in (9:00 – 10:00 PM)",
                        subtitle = "Last chance review before sleep. Only sent if targets are pending.",
                        checked = prefs.nightReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(nightReminderEnabled = it)) },
                        testTag = "toggle_night_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFD32F2F),
                        title = "Weak Chapter Radar",
                        subtitle = "Detects your weakest chapters (Current Electricity, Matrices, etc.) and prompts targeted drills.",
                        checked = prefs.weakChapterReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(weakChapterReminderEnabled = it)) },
                        testTag = "toggle_weak_chapter_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = Color(0xFF00897B),
                        title = "Active Recall & Revision",
                        subtitle = "Detects chapters unrevised for days to beat the forgetting curve.",
                        checked = prefs.revisionReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(revisionReminderEnabled = it)) },
                        testTag = "toggle_revision_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.Timer,
                        iconTint = Color(0xFFC2185B),
                        title = "Exam Countdown Alerts",
                        subtitle = "Alerts at 30, 15, 7, 5, 3, 2, 1 days and exam morning.",
                        checked = prefs.examReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(examReminderEnabled = it)) },
                        testTag = "toggle_exam_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFFF5722),
                        title = "Streak & Achievement Celebrations",
                        subtitle = "Warns before streak breaks and celebrates milestones (3, 7, 15, 30, 50, 100 days).",
                        checked = prefs.streakReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(streakReminderEnabled = it)) },
                        testTag = "toggle_streak_reminder"
                    )
                }

                item {
                    ReminderToggleItem(
                        icon = Icons.Default.History,
                        iconTint = Color(0xFF5D4037),
                        title = "Comeback Inactivity Alerts",
                        subtitle = "Gentle encouragement if app is not opened for 2, 5, or 10 days.",
                        checked = prefs.inactivityReminderEnabled,
                        onCheckedChange = { updatePrefs(prefs.copy(inactivityReminderEnabled = it)) },
                        testTag = "toggle_inactivity_reminder"
                    )
                }

                // 5. Smart Rules Summary Card
                item {
                    SmartRulesPolicyCard()
                }

                // 6. Developer Tools -> Notification Testing
                item {
                    DeveloperToolsCard(
                        onOpenNotificationTesting = { showDeveloperTestingScreen = true }
                    )
                }
            }
        }
    }

    // Bottom sheet for Notification History
    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState
        ) {
            NotificationHistorySheetContent(
                historyList = historyList,
                onClose = { showHistorySheet = false },
                onClearHistory = {
                    coroutineScope.launch {
                        db.rankifyDao().clearNotificationHistory()
                        Toast.makeText(context, "Notification history cleared", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationIntelligenceScoreCard(
    score: NotificationIntelligenceScore,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("intelligence_score_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notification Intelligence",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "${score.totalScore}/100",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { score.totalScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = score.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Score Factors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreFactorChip(label = "Consistency", value = "${score.consistencyScore}/25")
                ScoreFactorChip(label = "Study Time", value = "${score.studyTimeScore}/25")
                ScoreFactorChip(label = "Targets", value = "${score.targetDisciplineScore}/25")
                ScoreFactorChip(label = "Exam Focus", value = "${score.examProximityScore}/25")
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = score.recommendedTiming,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ScoreFactorChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MasterNotificationCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("master_notification_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Enable Smart Notifications",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = if (enabled) "AI buddy notifications active" else "All study alerts paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("master_notification_switch"),
                thumbContent = {
                    if (enabled) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun PersonalitySelectorSection(
    selectedPersonality: NotificationPersonality,
    onSelectPersonality: (NotificationPersonality) -> Unit,
    onTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notification Personality",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 4.dp)
            )

            OutlinedButton(
                onClick = onTestNotification,
                modifier = Modifier.testTag("send_test_notification_btn"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Send Test", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ranki Mascot Live Preview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankiMascotAvatar(
                    personality = selectedPersonality,
                    sizeDp = 52,
                    isSpeaking = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ranki says (${selectedPersonality.displayName}):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (selectedPersonality) {
                            NotificationPersonality.SAVAGE -> "\"Screen time 6 ghante, padhai 15 minute? Aise banoge topper? Kholo kitaab!\""
                            NotificationPersonality.FUNNY -> "\"WiFi 5G chal raha hai par dimaag syllabus se 2G pe bhi nahi jud raha? Fix it! 😂\""
                            NotificationPersonality.MOTIVATION -> "\"Pressure is a privilege. You have the heart of a champion. Let's finish this chapter!\""
                            NotificationPersonality.AI_MENTOR -> "\"Active recall protocol: solve 3 derivations without looking at the solution key.\""
                            NotificationPersonality.MIXED -> "\"Dynamic AI mode: I'll adapt my tone to your real-time study velocity and streaks!\""
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        NotificationPersonality.entries.forEach { personality ->
            val isSelected = personality == selectedPersonality
            PersonalityCard(
                personality = personality,
                isSelected = isSelected,
                onClick = { onSelectPersonality(personality) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PersonalityCard(
    personality: NotificationPersonality,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("personality_card_${personality.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = personality.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = personality.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (personality == NotificationPersonality.MIXED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = personality.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ReminderToggleItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SmartRulesPolicyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_rules_policy_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Rules & Student Protection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val rules = listOf(
                "Maximum 3 notifications per day to prevent spam.",
                "Never disturbs during an active study session or mock test.",
                "Strict Silent Hours: Never notifies after 10:15 PM or before 7:00 AM.",
                "If today's target is conquered, remaining reminders are canceled & celebration sent.",
                "7-Day Anti-Repetition Rule: No repeated notification templates within a week."
            )

            rules.forEach { rule ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = rule,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationHistorySheetContent(
    historyList: List<NotificationHistoryEntity>,
    onClose: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("notification_history_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Notification History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${historyList.size} past notifications logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                if (historyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearHistory,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No notifications sent yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = sdf.format(Date(item.createdTime)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${item.category} • ${item.personality}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = if (item.opened) "Opened ✓" else if (item.dismissed) "Dismissed" else "Delivered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.opened) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperToolsCard(
    onOpenNotificationTesting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("developer_tools_card")
            .clickable { onOpenNotificationTesting() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Developer Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Notification Testing & Diagnostics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Send real test notifications for all 17 types (Funny, Savage, AI Mentor, Weak Chapters, etc.), inspect Android channels, exact alarms, WorkManager status, and read real-time skip logs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpenNotificationTesting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_notification_testing_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notification Testing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
