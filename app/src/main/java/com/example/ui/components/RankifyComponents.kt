package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import com.example.data.model.SyncStatus
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.FirebaseAuthService
import com.example.data.model.ExamEntity
import com.example.ui.theme.ChemistryColor
import com.example.ui.theme.MathsColor
import com.example.ui.theme.PhysicsColor
import com.example.ui.theme.RankifyAccent
import com.example.ui.theme.RankifyGlassBorder
import com.example.ui.theme.RankifyGlassSurface
import com.example.ui.theme.RankifyGradient
import com.example.ui.theme.RankifyLightSurfaceVariant
import com.example.ui.theme.RankifyPrimary
import com.example.ui.theme.RankifyPrimaryLight
import com.example.ui.theme.RankifyPurple
import com.example.ui.theme.RankifyPurpleLight
import com.example.ui.theme.RankifySoftGradient

/**
 * Premium Modern Progress Ring with smooth animation, gradient/subject stroke and round caps.
 */
@Composable
fun RankifyProgressRing(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    strokeWidth: Dp = 7.dp,
    trackColor: Color = Color(0xFFE2E8F0).copy(alpha = 0.65f),
    startColor: Color = RankifyPrimary,
    endColor: Color = RankifyPurple,
    centerContent: @Composable (() -> Unit)? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 220),
        label = "progressRingAnimation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Animated Gradient Arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(startColor, endColor, startColor),
                        center = Offset(this.size.width / 2f, this.size.height / 2f)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        centerContent?.invoke()
    }
}

/**
 * Compact Progress Ring for chapter items and subject summaries.
 */
@Composable
fun MiniProgressRing(
    percentage: Int,
    color: Color = RankifyPrimary,
    size: Dp = 38.dp,
    strokeWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    RankifyProgressRing(
        progress = percentage / 100f,
        size = size,
        strokeWidth = strokeWidth,
        startColor = color,
        endColor = color,
        trackColor = color.copy(alpha = 0.15f),
        modifier = modifier,
        centerContent = {
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (percentage == 100) 8.sp else 9.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = color
            )
        }
    )
}

/**
 * Glassmorphic Card Container with rounded corners (18-24dp), soft indigo/purple border glow.
 */
@Composable
fun RankifyGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderStroke: BorderStroke? = BorderStroke(1.dp, RankifyGlassBorder),
    backgroundColor: Color = RankifyGlassSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable { onClick() }
    } else {
        modifier
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = cardModifier
    ) {
        content()
    }
}

@Composable
fun RankifyTopBar(
    streakDays: Int,
    studentName: String = "Aryan Sharma",
    activeExam: ExamEntity? = null,
    isLoggedIn: Boolean = false,
    isGuestMode: Boolean = false,
    userEmail: String? = null,
    syncStatus: SyncStatus = SyncStatus.Synced,
    isOnline: Boolean = true,
    canNavigateBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onExamClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..22 -> "Good evening"
        else -> "Good night"
    }
    val currentAuthUser = FirebaseAuthService.currentUser
    val firstName = studentName.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Ranker"
    val effectiveLoggedIn = (currentAuthUser != null && !currentAuthUser.isAnonymous) || isLoggedIn

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, RankifyGlassBorder),
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canNavigateBack) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("topbar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RankifySoftGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rankify_official_logo),
                        contentDescription = "Rankify Official Logo",
                        modifier = Modifier.size(26.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rankify",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RankifyPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "PRO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = RankifyPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "$greeting, $firstName",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = RankifyPurpleLight
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Cloud Sync Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (syncStatus) {
                        is SyncStatus.Syncing -> Color(0xFFEFF6FF)
                        is SyncStatus.Offline -> Color(0xFFFFF7ED)
                        is SyncStatus.Synced -> Color(0xFFF0FDF4)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (syncStatus) {
                            is SyncStatus.Syncing -> Color(0xFFBFDBFE)
                            is SyncStatus.Offline -> Color(0xFFFED7AA)
                            is SyncStatus.Synced -> Color(0xFFBBF7D0)
                        }
                    ),
                    modifier = Modifier
                        .clickable { onSyncClick() }
                        .testTag("topbar_sync_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (syncStatus) {
                            is SyncStatus.Syncing -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Syncing...",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Syncing",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                            is SyncStatus.Offline -> {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFFC2410C)
                                )
                            }
                            is SyncStatus.Synced -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "All changes synced",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Synced",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }

                // Streak Pill with soft amber gradient
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$streakDays d",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF9A3412)
                        )
                    }
                }

                // Account / Profile Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (effectiveLoggedIn) Color(0xFFF0FDF4) else RankifyPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (effectiveLoggedIn) Color(0xFFBBF7D0) else RankifyPrimary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .clickable { onAccountClick() }
                        .testTag("topbar_account_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Student Profile",
                            tint = if (effectiveLoggedIn) Color(0xFF16A34A) else RankifyPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (effectiveLoggedIn) firstName else "Login",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (effectiveLoggedIn) Color(0xFF15803D) else RankifyPrimary
                        )
                    }
                }

                // Notifications Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RankifyPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clickable { onNotificationsClick() }
                        .testTag("topbar_notifications_button")
                ) {
                    Box(
                        modifier = Modifier.padding(7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Smart Notifications",
                            tint = RankifyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full Sync Status Banner showing explicit state:
 * - Syncing...
 * - All changes synced
 * - Offline (changes will sync automatically)
 */
@Composable
fun RankifySyncStatusBar(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    onSyncClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSyncClick() }
            .testTag("rankify_sync_status_bar"),
        color = when (syncStatus) {
            is SyncStatus.Syncing -> Color(0xFFEFF6FF)
            is SyncStatus.Offline -> Color(0xFFFFF7ED)
            is SyncStatus.Synced -> Color(0xFFF0FDF4)
        },
        border = BorderStroke(
            1.dp,
            when (syncStatus) {
                is SyncStatus.Syncing -> Color(0xFFBFDBFE)
                is SyncStatus.Offline -> Color(0xFFFED7AA)
                is SyncStatus.Synced -> Color(0xFFBBF7D0)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val (icon, color, text) = when (syncStatus) {
                is SyncStatus.Syncing -> Triple(Icons.Default.Sync, Color(0xFF2563EB), "Syncing...")
                is SyncStatus.Offline -> Triple(Icons.Default.CloudOff, Color(0xFFC2410C), "Offline (changes will sync automatically)")
                is SyncStatus.Synced -> Triple(Icons.Default.CheckCircle, Color(0xFF15803D), "All changes synced")
            }
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
        }
    }
}

@Composable
fun RankifyBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, RankifyGlassBorder),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            val items = listOf(
                NavigationItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
                NavigationItem("Study", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "tab_study"),
                NavigationItem("Practice", Icons.Filled.Quiz, Icons.Outlined.Quiz, "tab_practice"),
                NavigationItem("Ask AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "tab_ai"),
                NavigationItem("Progress", Icons.Filled.Insights, Icons.Outlined.Insights, "tab_progress")
            )

            items.forEachIndexed { index, item ->
                val isSelected = currentTab == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = RankifyPrimary.copy(alpha = 0.14f),
                        selectedIconColor = RankifyPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        selectedTextColor = RankifyPrimary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}

private data class NavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AiAskSearchBar(
    onAsk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RankifyGlassBorder),
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(RankifyPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Ask",
                    tint = RankifyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        text = "Ask AI... 'Formula kab use hoga?'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_search_input")
            )
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onAsk(text)
                        text = ""
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                modifier = Modifier.testTag("ai_search_submit_button")
            ) {
                Text("Ask", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun StopwatchLiveBanner(
    isRunning: Boolean,
    elapsedSeconds: Long,
    subject: String,
    chapter: String,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit,
    onOpenStopwatch: () -> Unit
) {
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) RankifyPrimary else MaterialTheme.colorScheme.surface
        ),
        border = if (!isRunning) BorderStroke(1.dp, RankifyGlassBorder) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenStopwatch() }
            .testTag("stopwatch_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color.White.copy(alpha = 0.22f) else RankifyPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Stopwatch",
                        tint = if (isRunning) Color.White else RankifyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isRunning) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$subject • $chapter",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRunning) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPauseResume) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Resume",
                        tint = if (isRunning) Color.White else RankifyPrimary
                    )
                }
                IconButton(onClick = onFinish) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Finish",
                        tint = if (isRunning) Color.White else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

data class GreetingTimeInfo(
    val greeting: String,
    val periodName: String,
    val icon: ImageVector,
    val iconTint: Color,
    val containerBg: Color,
    val motivationalQuote: String
)

fun getTimeGreetingInfo(): GreetingTimeInfo {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> GreetingTimeInfo(
            greeting = "Good morning",
            periodName = "Morning Study Block",
            icon = Icons.Default.WbSunny,
            iconTint = Color(0xFFD97706),
            containerBg = Color(0xFFFEF3C7),
            motivationalQuote = "Fresh mind, highest focus! Perfect time to tackle challenging numericals and concepts."
        )
        in 12..16 -> GreetingTimeInfo(
            greeting = "Good afternoon",
            periodName = "Afternoon Practice Session",
            icon = Icons.Default.LightMode,
            iconTint = Color(0xFFEA580C),
            containerBg = Color(0xFFFFEDD5),
            motivationalQuote = "Keep the momentum running! Test your problem solving with timed practice questions."
        )
        in 17..22 -> GreetingTimeInfo(
            greeting = "Good evening",
            periodName = "Evening Revision Time",
            icon = Icons.Default.NightsStay,
            iconTint = Color(0xFF4F46E5),
            containerBg = Color(0xFFEEF2FF),
            motivationalQuote = "Consolidate today's learning! Review formulas, notes, and clear pending backlogs."
        )
        else -> GreetingTimeInfo(
            greeting = "Good night",
            periodName = "Late Night Study Hours",
            icon = Icons.Default.Bedtime,
            iconTint = Color(0xFF7C3AED),
            containerBg = Color(0xFFF3E8FF),
            motivationalQuote = "Quiet night hours! Revise quick summary sheets and wrap up your daily target."
        )
    }
}

@Composable
fun StudentGreetingCard(
    studentName: String,
    targetPercentage: Int = 95,
    streakDays: Int = 5,
    activeExam: ExamEntity? = null,
    onCardClick: () -> Unit = {}
) {
    val info = remember { getTimeGreetingInfo() }
    val trimmedName = studentName.trim().ifBlank { "Class 12 Ranker" }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, RankifyGlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("student_greeting_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            info.containerBg.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(info.containerBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = info.icon,
                            contentDescription = info.greeting,
                            tint = info.iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${info.greeting},",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$trimmedName 👋",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = info.containerBg,
                    border = BorderStroke(1.dp, info.iconTint.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target $targetPercentage%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = info.iconTint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = info.motivationalQuote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (activeExam != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = RankifyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Focus: ${activeExam.examName} (${activeExam.subject}) • Target ${activeExam.targetScore}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = RankifyPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ExamCountdownCard(
    exam: ExamEntity,
    onExamClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExamClick() }
            .testTag("exam_countdown_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RankifyPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "CBSE TARGET",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = exam.examName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${exam.subject} • Target: ${exam.targetScore}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "32",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = RankifyPrimary
                    )
                )
                Text(
                    text = "Days Left",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

fun getSubjectColor(subject: String): Color {
    return when (subject) {
        "Physics" -> PhysicsColor
        "Chemistry" -> ChemistryColor
        "Mathematics" -> MathsColor
        else -> RankifyPrimary
    }
}
