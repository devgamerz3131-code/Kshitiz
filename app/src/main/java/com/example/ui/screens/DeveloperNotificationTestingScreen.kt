package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.notifications.DebugNotificationLog
import com.example.notifications.NotificationAdaptiveFrequencyEngine
import com.example.notifications.NotificationChannelsManager
import com.example.notifications.NotificationDebugLogger
import com.example.notifications.NotificationDispatcher
import com.example.notifications.NotificationPreferencesManager
import com.example.notifications.SmartNotificationEngine
import com.example.notifications.TestNotificationHelper
import com.example.notifications.TimelineSlotPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperNotificationTestingScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { NotificationPreferencesManager.getInstance(context) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Triggers (17)", "System Status", "Extra Controls", "Skip & Live Logs")

    // Live debug logs
    val logs by NotificationDebugLogger.logs.collectAsState()

    // Notification Permission state on Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            NotificationDebugLogger.logInfo("Permission", "POST_NOTIFICATIONS granted.")
        } else {
            Toast.makeText(context, "Notification permission denied!", Toast.LENGTH_SHORT).show()
            NotificationDebugLogger.logError("Permission", "POST_NOTIFICATIONS denied by user.")
        }
    }

    // Diagnostics state
    var exactAlarmStatus by remember { mutableStateOf("Checking...") }
    var workManagerStatus by remember { mutableStateOf("Checking...") }
    var workerRunningStatus by remember { mutableStateOf("Idle") }
    var scheduledWorkCount by remember { mutableIntStateOf(0) }
    var pendingNotificationsCount by remember { mutableIntStateOf(0) }
    var fcmTokenText by remember { mutableStateOf("Spark Offline Local Mode (WorkManager Active)") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var timelinePreviewList by remember { mutableStateOf<List<TimelineSlotPreview>>(emptyList()) }

    fun refreshDiagnostics() {
        pendingNotificationsCount = NotificationDispatcher.getActiveNotificationsCount(context)

        // Exact Alarm Status
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        exactAlarmStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager?.canScheduleExactAlarms() == true) "Allowed (Exact Alarms OK)" else "Restricted (Exact Alarms Not Allowed)"
        } else {
            "Allowed (Pre-Android 12)"
        }

        // WorkManager status
        scope.launch(Dispatchers.IO) {
            try {
                val workInfos = WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork("RankifySmartNotificationPeriodicWork")
                    .get()
                scheduledWorkCount = workInfos.size
                val running = workInfos.any { it.state == WorkInfo.State.RUNNING }
                workerRunningStatus = if (running) "Running Now" else "Idle (Scheduled every 15m)"
                workManagerStatus = if (workInfos.isNotEmpty()) {
                    val state = workInfos.first().state.name
                    "Periodic Worker: $state (ID: ${workInfos.first().id.toString().take(8)}...)"
                } else {
                    "No Active Periodic Work"
                }
            } catch (e: Exception) {
                workManagerStatus = "WorkManager check failed: ${e.message}"
            }

            // Check FCM / Spark status
            try {
                val clazz = Class.forName("com.google.firebase.messaging.FirebaseMessaging")
                val method = clazz.getMethod("getInstance")
                val instance = method.invoke(null)
                fcmTokenText = "Firebase Messaging Available"
            } catch (e: Exception) {
                fcmTokenText = "Spark Plan Local Mode (WorkManager Periodic Engine Active)"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDiagnostics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Developer Notification Testing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ranki Smart AI Engine • Production Diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshDiagnostics() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Alert Banner if not granted on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "POST_NOTIFICATIONS is disabled. System notifications won't appear.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Allow", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> TriggersTab(
                        context = context,
                        onTrigger = { name, action ->
                            action()
                            refreshDiagnostics()
                            Toast.makeText(context, "Sent: $name", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> SystemStatusTab(
                        context = context,
                        hasPermission = hasNotificationPermission,
                        exactAlarmStatus = exactAlarmStatus,
                        workManagerStatus = workManagerStatus,
                        workerRunningStatus = workerRunningStatus,
                        pendingCount = pendingNotificationsCount,
                        scheduledCount = scheduledWorkCount,
                        lastNotificationTime = prefsManager.getLastNotificationTimestamp(),
                        lastOpenTime = prefsManager.getLastAppOpenTimestamp(),
                        fcmToken = fcmTokenText,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                    2 -> ExtraControlsTab(
                        context = context,
                        onAction = { msg ->
                            refreshDiagnostics()
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onOpenPreview = {
                            timelinePreviewList = NotificationAdaptiveFrequencyEngine.getTimelinePreview(
                                currentSentCount = prefsManager.getDailySentCount(),
                                totalAllowed = prefsManager.getPreferences().maxDailyNotifications
                            )
                            showPreviewDialog = true
                        }
                    )
                    3 -> LiveLogsTab(
                        logs = logs,
                        onClearLogs = { NotificationDebugLogger.clearLogs() }
                    )
                }
            }
        }
    }

    // Preview Dialog
    if (showPreviewDialog) {
        TimelinePreviewDialog(
            previewList = timelinePreviewList,
            onDismiss = { showPreviewDialog = false }
        )
    }
}

// =========================================================================
// TAB 1: ALL 17 REAL NOTIFICATION TRIGGERS
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriggersTab(
    context: Context,
    onTrigger: (String, () -> Unit) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TriggerSectionHeader(
                title = "⚡ Core & Instant Verification",
                subtitle = "Immediately triggers a real Android notification into the system tray."
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestTriggerButton(
                    title = "Send Test Notification",
                    icon = Icons.Default.BugReport,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onTrigger("Send Test Notification") { TestNotificationHelper.sendTestNotification(context) } }
                )
                TestTriggerButton(
                    title = "Generate Random Notification",
                    icon = Icons.Default.Shuffle,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onTrigger("Random Template") { SmartNotificationEngine.generateRandomNotification(context) } }
                )
            }
        }

        item {
            TriggerSectionHeader(
                title = "🎭 Ranki AI Personalities",
                subtitle = "Funny roasts, savage checks, mentor tips, and pure motivation."
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestTriggerButton(
                    title = "Funny Notification",
                    icon = Icons.Default.SentimentSatisfied,
                    color = Color(0xFFE65100),
                    onClick = { onTrigger("Funny Roast") { TestNotificationHelper.sendFunnyNotification(context) } }
                )
                TestTriggerButton(
                    title = "Savage Notification",
                    icon = Icons.Default.Warning,
                    color = Color(0xFFC2185B),
                    onClick = { onTrigger("Savage Reality Check") { TestNotificationHelper.sendSavageNotification(context) } }
                )
                TestTriggerButton(
                    title = "Motivation Notification",
                    icon = Icons.Default.Bolt,
                    color = Color(0xFF2E7D32),
                    onClick = { onTrigger("Motivation Nudge") { TestNotificationHelper.sendMotivationNotification(context) } }
                )
                TestTriggerButton(
                    title = "AI Mentor Notification",
                    icon = Icons.Default.Psychology,
                    color = Color(0xFF1565C0),
                    onClick = { onTrigger("AI Mentor Guidance") { TestNotificationHelper.sendAiMentorNotification(context) } }
                )
            }
        }

        item {
            TriggerSectionHeader(
                title = "📚 Study & Weak Topic Remediation",
                subtitle = "Targeting weakest chapters, spaced repetition, and daily challenges."
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestTriggerButton(
                    title = "Weak Chapter Notification",
                    icon = Icons.Default.TrackChanges,
                    color = Color(0xFFD32F2F),
                    onClick = { onTrigger("Weak Chapter Alert") { TestNotificationHelper.sendWeakChapterNotification(context) } }
                )
                TestTriggerButton(
                    title = "Revision Reminder",
                    icon = Icons.Default.History,
                    color = Color(0xFF00796B),
                    onClick = { onTrigger("Revision Reminder") { TestNotificationHelper.sendRevisionReminder(context) } }
                )
                TestTriggerButton(
                    title = "Formula of the Day",
                    icon = Icons.Default.Calculate,
                    color = Color(0xFF5E35B1),
                    onClick = { onTrigger("Formula of the Day") { TestNotificationHelper.sendFormulaOfDayNotification(context) } }
                )
                TestTriggerButton(
                    title = "Question of the Day",
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    color = Color(0xFF0288D1),
                    onClick = { onTrigger("Question of the Day") { TestNotificationHelper.sendQuestionOfDayNotification(context) } }
                )
            }
        }

        item {
            TriggerSectionHeader(
                title = "🏆 Progress, Streaks & Countdown",
                subtitle = "Celebrating milestones, streak defence, and exam proximity."
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestTriggerButton(
                    title = "Streak Reminder",
                    icon = Icons.Default.Whatshot,
                    color = Color(0xFFFF6F00),
                    onClick = { onTrigger("Streak Reminder") { TestNotificationHelper.sendStreakReminder(context) } }
                )
                TestTriggerButton(
                    title = "Target Reminder",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF388E3C),
                    onClick = { onTrigger("Target Reminder") { TestNotificationHelper.sendTargetReminder(context) } }
                )
                TestTriggerButton(
                    title = "Achievement Notification",
                    icon = Icons.Default.EmojiEvents,
                    color = Color(0xFFFBC02D),
                    onClick = { onTrigger("Achievement Milestone") { TestNotificationHelper.sendAchievementNotification(context) } }
                )
                TestTriggerButton(
                    title = "Exam Countdown",
                    icon = Icons.Default.HourglassTop,
                    color = Color(0xFFC62828),
                    onClick = { onTrigger("Exam Countdown") { TestNotificationHelper.sendExamCountdownNotification(context) } }
                )
                TestTriggerButton(
                    title = "Comeback Reminder",
                    icon = Icons.Default.Replay,
                    color = Color(0xFF7B1FA2),
                    onClick = { onTrigger("Comeback Reminder") { TestNotificationHelper.sendComebackReminder(context) } }
                )
            }
        }

        item {
            TriggerSectionHeader(
                title = "✨ Rankify Exclusives & Social",
                subtitle = "Vault resources, binaural audio, and live leaderboard updates."
            )
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestTriggerButton(
                    title = "Rankify Exclusive",
                    icon = Icons.Default.Diamond,
                    color = Color(0xFF6200EA),
                    onClick = { onTrigger("Rankify Exclusive") { TestNotificationHelper.sendRankifyExclusiveNotification(context) } }
                )
                TestTriggerButton(
                    title = "Song Released",
                    icon = Icons.Default.MusicNote,
                    color = Color(0xFF0091EA),
                    onClick = { onTrigger("Binaural Beats Released") { TestNotificationHelper.sendSongReleasedNotification(context) } }
                )
                TestTriggerButton(
                    title = "Community Update",
                    icon = Icons.Default.People,
                    color = Color(0xFF00897B),
                    onClick = { onTrigger("Community Sprint") { TestNotificationHelper.sendCommunityUpdateNotification(context) } }
                )
            }
        }
    }
}

@Composable
private fun TriggerSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TestTriggerButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

// =========================================================================
// TAB 2: SYSTEM STATUS & DIAGNOSTICS
// =========================================================================
@Composable
private fun SystemStatusTab(
    context: Context,
    hasPermission: Boolean,
    exactAlarmStatus: String,
    workManagerStatus: String,
    workerRunningStatus: String,
    pendingCount: Int,
    scheduledCount: Int,
    lastNotificationTime: Long,
    lastOpenTime: Long,
    fcmToken: String,
    onRequestPermission: () -> Unit
) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "System Diagnostics Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Permission Status
                    StatusRow(
                        label = "Notification Permission",
                        value = if (hasPermission) "Granted" else "Denied (Tap to Request)",
                        isGood = hasPermission,
                        actionLabel = if (!hasPermission) "Request" else null,
                        onAction = onRequestPermission
                    )

                    // 2. Exact Alarm Status
                    StatusRow(
                        label = "Exact Alarm Status",
                        value = exactAlarmStatus,
                        isGood = !exactAlarmStatus.startsWith("Restricted")
                    )

                    // 3. WorkManager Status
                    StatusRow(
                        label = "WorkManager Status",
                        value = workManagerStatus,
                        isGood = workManagerStatus.contains("ENQUEUED") || workManagerStatus.contains("Periodic")
                    )

                    // 4. Worker Running Status
                    StatusRow(
                        label = "Worker Execution State",
                        value = workerRunningStatus,
                        isGood = true
                    )

                    // 5. Pending Notifications
                    StatusRow(
                        label = "Active Tray Notifications",
                        value = "$pendingCount active",
                        isGood = true
                    )

                    // 6. Scheduled Notifications Count
                    StatusRow(
                        label = "Scheduled Work Requests",
                        value = "$scheduledCount requests registered",
                        isGood = scheduledCount > 0
                    )

                    // 7. Last Notification Time
                    val lastNotifStr = if (lastNotificationTime > 0) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastNotificationTime))
                    } else "None yet"
                    StatusRow(
                        label = "Last Notification Time",
                        value = lastNotifStr,
                        isGood = true
                    )

                    // 8. Next Notification Time
                    StatusRow(
                        label = "Next Evaluation Window",
                        value = "Periodic background evaluation (~15m interval)",
                        isGood = true
                    )

                    // 9. Last Open Time
                    val lastOpenStr = if (lastOpenTime > 0) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastOpenTime))
                    } else "None yet"
                    StatusRow(
                        label = "Last App Open Time",
                        value = lastOpenStr,
                        isGood = true
                    )

                    // 10. FCM Token Status
                    StatusRow(
                        label = "FCM Status",
                        value = fcmToken,
                        isGood = true
                    )
                }
            }
        }

        // Notification Channels Inspection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Android Notification Channels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real system channels registered on device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channels = notificationManager?.notificationChannels ?: emptyList()
                        if (channels.isEmpty()) {
                            Text("No channels created yet. Tap any test trigger to initialize.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            channels.forEach { channel ->
                                val importanceStr = when (channel.importance) {
                                    NotificationManager.IMPORTANCE_HIGH -> "HIGH (Heads-up banner + Sound)"
                                    NotificationManager.IMPORTANCE_DEFAULT -> "DEFAULT (Sound)"
                                    NotificationManager.IMPORTANCE_LOW -> "LOW (Silent in tray)"
                                    NotificationManager.IMPORTANCE_NONE -> "DISABLED / BLOCKED"
                                    else -> "Importance ${channel.importance}"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = channel.name.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(text = "ID: ${channel.id} • $importanceStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (channel.importance > NotificationManager.IMPORTANCE_NONE) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Pre-Android 8.0: Channels not applicable.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isGood: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isGood) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, fontSize = 12.sp)
            }
        }
    }
}

// =========================================================================
// TAB 3: EXTRA BUTTONS & CONTROLS
// =========================================================================
@Composable
private fun ExtraControlsTab(
    context: Context,
    onAction: (String) -> Unit,
    onOpenPreview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Operational Controls & Scheduler Override",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Direct administrative controls to test background workers, reset quotas, and cancel alerts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ControlActionCard(
                title = "Cancel All Notifications",
                description = "Instantly clears all active notifications from the system tray and cancels pending workers.",
                buttonText = "Cancel All",
                icon = Icons.Default.Cancel,
                isDestructive = true,
                onClick = {
                    NotificationDispatcher.cancelAllNotifications(context)
                    onAction("Cancelled all system notifications.")
                }
            )
        }

        item {
            ControlActionCard(
                title = "Reschedule Notifications",
                description = "Re-registers the periodic WorkManager task and updates sync schedules.",
                buttonText = "Reschedule",
                icon = Icons.Default.Sync,
                onClick = {
                    SmartNotificationEngine.startPeriodicScheduling(context)
                    onAction("WorkManager periodic schedule refreshed.")
                }
            )
        }

        item {
            ControlActionCard(
                title = "Force Daily Scheduler",
                description = "Evaluates student learning context and triggers an immediate smart notification if conditions allow.",
                buttonText = "Run Daily Scheduler",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    SmartNotificationEngine.forceDailyScheduler(context)
                    onAction("Forced daily scheduler evaluation. Check Live Logs tab.")
                }
            )
        }

        item {
            ControlActionCard(
                title = "Force Weekly Scheduler",
                description = "Forces the weekly spaced-repetition and exam strategy planner alert.",
                buttonText = "Run Weekly Scheduler",
                icon = Icons.Default.Schedule,
                onClick = {
                    SmartNotificationEngine.forceWeeklyScheduler(context)
                    onAction("Weekly planning notification dispatched.")
                }
            )
        }

        item {
            ControlActionCard(
                title = "Reset Notification History",
                description = "Clears daily sent count, 14-day anti-repetition cache, and cooldown counters for clean testing.",
                buttonText = "Reset History",
                icon = Icons.Default.ClearAll,
                isDestructive = true,
                onClick = {
                    SmartNotificationEngine.resetNotificationHistory(context)
                    onAction("Notification history & limits reset to zero.")
                }
            )
        }

        item {
            ControlActionCard(
                title = "Generate Random Notification",
                description = "Draws a random template from the 415 template library and immediately displays it.",
                buttonText = "Generate Random",
                icon = Icons.Default.AutoAwesome,
                onClick = {
                    SmartNotificationEngine.generateRandomNotification(context)
                    onAction("Random notification dispatched.")
                }
            )
        }

        item {
            ControlActionCard(
                title = "Preview Today's Notifications",
                description = "Inspect the full 24-hour timeline of planned notification slots according to student study schedule.",
                buttonText = "Open Preview",
                icon = Icons.Default.Visibility,
                onClick = onOpenPreview
            )
        }
    }
}

@Composable
private fun ControlActionCard(
    title: String,
    description: String,
    buttonText: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buttonText, fontSize = 13.sp)
            }
        }
    }
}

// =========================================================================
// TAB 4: LIVE SKIP & EVALUATION LOGS
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveLogsTab(
    logs: List<DebugNotificationLog>,
    onClearLogs: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "SKIPPED" -> logs.filter { it.type == DebugNotificationLog.LogType.SKIPPED }
            "DISPATCHED" -> logs.filter { it.type == DebugNotificationLog.LogType.DISPATCHED }
            "SCHEDULED" -> logs.filter { it.type == DebugNotificationLog.LogType.SCHEDULED }
            else -> logs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Evaluation Logs (${filteredLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Explains why notifications were dispatched or skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClearLogs) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "SKIPPED", "DISPATCHED", "SCHEDULED").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No logs recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap 'Force Daily Scheduler' or any trigger to populate logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogItemView(log)
                }
            }
        }
    }
}

@Composable
private fun LogItemView(log: DebugNotificationLog) {
    val (badgeBg, badgeFg, badgeText) = when (log.type) {
        DebugNotificationLog.LogType.DISPATCHED -> Triple(Color(0xFF1B5E20), Color(0xFF81C784), "DISPATCHED")
        DebugNotificationLog.LogType.SKIPPED -> Triple(Color(0xFFE65100), Color(0xFFFFB74D), "SKIPPED")
        DebugNotificationLog.LogType.SCHEDULED -> Triple(Color(0xFF4A148C), Color(0xFFBA68C8), "SCHEDULED")
        DebugNotificationLog.LogType.ERROR -> Triple(Color(0xFFB71C1C), Color(0xFFEF9A9A), "ERROR")
        DebugNotificationLog.LogType.INFO -> Triple(Color(0xFF0D47A1), Color(0xFF64B5F6), "INFO")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
            .background(Color(0xFF252525), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeFg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = log.formattedTime,
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = log.details,
            color = Color(0xFFDCDCDC),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// =========================================================================
// TIMELINE PREVIEW DIALOG
// =========================================================================
@Composable
private fun TimelinePreviewDialog(
    previewList: List<TimelineSlotPreview>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Column {
                Text("Preview Today's Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("24-Hour Student Notification Timeline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(previewList) { slot ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (slot.status) {
                                "Active Window" -> MaterialTheme.colorScheme.primaryContainer
                                "Completed" -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = slot.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = slot.timeRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = slot.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Status: ${slot.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (slot.status == "Active Window") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    )
}
