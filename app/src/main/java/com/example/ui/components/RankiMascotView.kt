package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notifications.model.NotificationPersonality

/**
 * Ranki Mascot - Personal AI Study Buddy for Rankify.
 * Features:
 * - Animated expressive canvas avatar with antenna, glowing eyes, and mood badge.
 * - Dynamic personality dialogue (Funny, Savage, Motivational, AI Mentor).
 * - Interactive action chips (Roast Me, Motivate Me, Study Hack).
 */
@Composable
fun RankiMascotAvatar(
    personality: NotificationPersonality = NotificationPersonality.MIXED,
    sizeDp: Int = 64,
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ranki_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bounce"
    )

    val eyeBlink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_blink"
    )

    val antennaPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "antenna_glow"
    )

    val (primaryColor, accentColor, glowColor) = when (personality) {
        NotificationPersonality.SAVAGE -> Triple(Color(0xFFE53935), Color(0xFFFF8A80), Color(0xFFFF5252))
        NotificationPersonality.MOTIVATION -> Triple(Color(0xFF43A047), Color(0xFFA5D6A7), Color(0xFF66BB6A))
        NotificationPersonality.AI_MENTOR -> Triple(Color(0xFF1E88E5), Color(0xFF90CAF9), Color(0xFF42A5F5))
        NotificationPersonality.FUNNY -> Triple(Color(0xFFFB8C00), Color(0xFFFFE082), Color(0xFFFFB74D))
        NotificationPersonality.MIXED -> Triple(Color(0xFF7C4DFF), Color(0xFFD1C4E9), Color(0xFFB388FF))
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val w = size.width
            val h = size.height

            // 1. Antenna stalk
            drawLine(
                color = primaryColor,
                start = Offset(w * 0.5f, h * 0.28f),
                end = Offset(w * 0.5f, h * 0.12f),
                strokeWidth = w * 0.05f
            )

            // 2. Antenna orb with pulsating glow
            drawCircle(
                color = glowColor.copy(alpha = 0.4f),
                radius = (w * 0.1f) * antennaPulse,
                center = Offset(w * 0.5f, h * 0.11f)
            )
            drawCircle(
                color = accentColor,
                radius = w * 0.07f,
                center = Offset(w * 0.5f, h * 0.11f)
            )

            // 3. Ear pods / headphone side nodes
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.08f, h * 0.42f),
                size = Size(w * 0.12f, h * 0.26f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.80f, h * 0.42f),
                size = Size(w * 0.12f, h * 0.26f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )

            // 4. Head Outer Chassis (Soft Rounded Capsule)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.85f))
                ),
                topLeft = Offset(w * 0.16f, h * 0.26f),
                size = Size(w * 0.68f, h * 0.58f),
                cornerRadius = CornerRadius(w * 0.24f, w * 0.24f)
            )

            // 5. Visor Screen (Dark glossy screen inside head)
            drawRoundRect(
                color = Color(0xFF121420),
                topLeft = Offset(w * 0.23f, h * 0.35f),
                size = Size(w * 0.54f, h * 0.38f),
                cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
            )

            // 6. Expressive Eyes on the Visor
            val eyeHeight = (w * 0.11f) * eyeBlink
            val eyeWidth = w * 0.11f
            val eyeY = h * 0.48f

            when (personality) {
                NotificationPersonality.SAVAGE -> {
                    // Slightly angled cool squinted eyes / glasses line
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.30f, eyeY - 2f),
                        end = Offset(w * 0.44f, eyeY + 2f),
                        strokeWidth = w * 0.06f
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.56f, eyeY + 2f),
                        end = Offset(w * 0.70f, eyeY - 2f),
                        strokeWidth = w * 0.06f
                    )
                    // Confident smirk
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.45f, h * 0.63f),
                        end = Offset(w * 0.58f, h * 0.61f),
                        strokeWidth = w * 0.035f
                    )
                }
                NotificationPersonality.FUNNY -> {
                    // Playful winking eye (left winking arc, right wide eye)
                    val leftPath = Path().apply {
                        moveTo(w * 0.32f, eyeY)
                        quadraticTo(w * 0.38f, eyeY - 6f, w * 0.44f, eyeY)
                    }
                    drawPath(leftPath, accentColor, style = Stroke(width = w * 0.04f))
                    drawCircle(
                        color = accentColor,
                        radius = eyeWidth * 0.5f,
                        center = Offset(w * 0.62f, eyeY)
                    )
                    // Open happy smile arc
                    val smilePath = Path().apply {
                        moveTo(w * 0.40f, h * 0.61f)
                        quadraticTo(w * 0.50f, h * 0.68f, w * 0.60f, h * 0.61f)
                    }
                    drawPath(smilePath, accentColor, style = Stroke(width = w * 0.04f))
                }
                NotificationPersonality.MOTIVATION -> {
                    // Wide determined eyes with highlight dots
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(w * 0.31f, eyeY - eyeHeight * 0.5f),
                        size = Size(eyeWidth, eyeHeight),
                        cornerRadius = CornerRadius(eyeWidth * 0.3f, eyeWidth * 0.3f)
                    )
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(w * 0.57f, eyeY - eyeHeight * 0.5f),
                        size = Size(eyeWidth, eyeHeight),
                        cornerRadius = CornerRadius(eyeWidth * 0.3f, eyeWidth * 0.3f)
                    )
                    // High cheerful curved smile
                    val smilePath = Path().apply {
                        moveTo(w * 0.42f, h * 0.62f)
                        quadraticTo(w * 0.50f, h * 0.67f, w * 0.58f, h * 0.62f)
                    }
                    drawPath(smilePath, accentColor, style = Stroke(width = w * 0.035f))
                }
                else -> {
                    // Standard Friendly / Intelligent rounded eyes
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(w * 0.32f, eyeY - eyeHeight * 0.5f),
                        size = Size(eyeWidth, eyeHeight),
                        cornerRadius = CornerRadius(eyeWidth * 0.5f, eyeWidth * 0.5f)
                    )
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(w * 0.56f, eyeY - eyeHeight * 0.5f),
                        size = Size(eyeWidth, eyeHeight),
                        cornerRadius = CornerRadius(eyeWidth * 0.5f, eyeWidth * 0.5f)
                    )
                    // Friendly mouth line or talking pulse
                    val mouthWidth = if (isSpeaking) w * 0.22f else w * 0.16f
                    drawLine(
                        color = accentColor,
                        start = Offset(w * 0.50f - mouthWidth * 0.5f, h * 0.63f),
                        end = Offset(w * 0.50f + mouthWidth * 0.5f, h * 0.63f),
                        strokeWidth = w * 0.035f
                    )
                }
            }

            // 7. Cheek blush or circuit nodes
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = w * 0.035f,
                center = Offset(w * 0.27f, h * 0.60f)
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = w * 0.035f,
                center = Offset(w * 0.73f, h * 0.60f)
            )
        }
    }
}

/**
 * Interactive Ranki Study Buddy Card for the Home Screen and Study Hub.
 * Lets the student interact with Ranki, request dynamic roasts, motivation,
 * or AI study tips on the fly!
 */
@Composable
fun RankiStudyBuddyCard(
    streakDays: Int,
    dailyGoalMinutes: Int = 180,
    studiedMinutesToday: Int = 0,
    onOpenNotificationSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentMood by remember { mutableStateOf(NotificationPersonality.MIXED) }
    var quoteIndex by remember { mutableIntStateOf(0) }

    val quotes = remember(currentMood, streakDays, studiedMinutesToday) {
        getRankiQuotes(currentMood, streakDays, studiedMinutesToday, dailyGoalMinutes)
    }

    val activeQuote = quotes.getOrElse(quoteIndex % quotes.size) { quotes.first() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ranki_study_buddy_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Avatar + Title & Mood Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankiMascotAvatar(
                    personality = currentMood,
                    sizeDp = 52,
                    isSpeaking = true,
                    modifier = Modifier.clickable {
                        // Cycle mood on avatar tap
                        currentMood = when (currentMood) {
                            NotificationPersonality.FUNNY -> NotificationPersonality.SAVAGE
                            NotificationPersonality.SAVAGE -> NotificationPersonality.MOTIVATION
                            NotificationPersonality.MOTIVATION -> NotificationPersonality.AI_MENTOR
                            NotificationPersonality.AI_MENTOR -> NotificationPersonality.MIXED
                            NotificationPersonality.MIXED -> NotificationPersonality.FUNNY
                        }
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ranki",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "AI Mascot",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Mood: ${currentMood.displayName} • Tap avatar to change vibe",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                }

                IconButton(
                    onClick = { quoteIndex++ },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ranki_refresh_quote_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Quote",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speech Bubble
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { quoteIndex++ }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "💬",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedContent(
                        targetState = activeQuote,
                        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                        label = "ranki_quote_anim"
                    ) { quote ->
                        Text(
                            text = quote,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Chips: Roast Me, Motivate Me, Study Tip, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickVibeChip(
                    label = "Roast Me 😈",
                    selected = currentMood == NotificationPersonality.SAVAGE,
                    onClick = {
                        currentMood = NotificationPersonality.SAVAGE
                        quoteIndex++
                    }
                )

                QuickVibeChip(
                    label = "Motivate 🚀",
                    selected = currentMood == NotificationPersonality.MOTIVATION,
                    onClick = {
                        currentMood = NotificationPersonality.MOTIVATION
                        quoteIndex++
                    }
                )

                QuickVibeChip(
                    label = "Funny 😂",
                    selected = currentMood == NotificationPersonality.FUNNY,
                    onClick = {
                        currentMood = NotificationPersonality.FUNNY
                        quoteIndex++
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Notification Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickVibeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun getRankiQuotes(
    mood: NotificationPersonality,
    streak: Int,
    studiedMins: Int,
    goalMins: Int
): List<String> {
    return when (mood) {
        NotificationPersonality.SAVAGE -> listOf(
            "Screen time 6 hours, study time $studiedMins mins? Aise banoge topper? Phone chhod kar kitaab kholo!",
            "Tumhara competitor 6 AM se ek chapter nipta chuka hai. Aur tum abhi reels dekh rahe ho? 😏",
            "Physics wahi padi hai jahan kal chhod ke soye the. Aaj cover karoge ya bahana banaoge?",
            "Snooze button dabane se board exam ka cutoff kam nahi hoga. Table par baitho!",
            "Streak $streak days ki hai, aalas kiya toh aaj raat zero ho jaayegi. Don't test me!"
        )
        NotificationPersonality.MOTIVATION -> listOf(
            "Every single question you solve today is a victory brick in your dream college fortress! 💪",
            "Champions don't wait to feel motivated. They sit down, focus, and conquer the chapter. Let's do this!",
            "You have shown up $streak days in a row! Unshakeable discipline is being forged in you right now. 🌟",
            "Your future self will look back at tonight and be deeply grateful that you didn't give up. Fly high!",
            "Turn fatigue into power. Even 25 focused minutes right now locks today's unstoppable momentum!"
        )
        NotificationPersonality.FUNNY -> listOf(
            "Book tumhara kab se wait kar rahi hai... usne tumhara kya bigaada hai? 😂 Ek chhota target karein?",
            "Badaam khane se derivations yaad nahi hoti! Active recall karne se hoti hain. Kholo study session!",
            "WiFi 5G chal raha hai aur brain ka syllabus se 2G connection bhi nahi jud raha? Fix it mitra!",
            "Alarm ko harane ka alag hi maza hai. Aaj pehla derivation subah hi finish kar do 😂",
            "Sharma ji ke ladke ko pasine chhoot rahe hain tumhari $streak din ki streak dekh kar!"
        )
        NotificationPersonality.AI_MENTOR -> listOf(
            "Feynman technique alert: If you can't explain the concept in simple words, you haven't mastered it yet. 🧠",
            "Spaced repetition law: Reviewing class notes within 4 hours stops 80% of Ebbinghaus memory loss.",
            "Active recall beats passive reading by 300%. Close the book and write the derivation from memory!",
            "Interleaved practice trick: Mix 5 Physics numericals with 5 Chemistry reactions for 45% sharper recall.",
            "Circadian focus optimization: Your prefrontal cortex logic circuits are in peak state right now. Start solving!"
        )
        NotificationPersonality.MIXED -> listOf(
            "Hey! Ranki here. $studiedMins/$goalMins mins locked today. Ek quick 25-min Pomodoro sprint lagayein?",
            "Streak check: $streak days on fire! 🔥 Keep the spark alive with today's pending chapter.",
            "Remember: Excellence is not an accident; it is daily consistency repeated until it looks effortless.",
            "Brain battery at 100%. Books on table. Eliminate distractions and let's conquer today's target!",
            "Ranki is watching your progress bar. Give me a reason to celebrate with you tonight! 🏆"
        )
    }
}
