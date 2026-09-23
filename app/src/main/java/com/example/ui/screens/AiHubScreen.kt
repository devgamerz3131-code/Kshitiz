package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.gemini.GeminiImageResult
import com.example.data.gemini.GeminiService
import com.example.data.gemini.LiveVoiceMessage
import com.example.data.gemini.VoiceSpeechManager
import com.example.ui.theme.RankifyAccent
import com.example.ui.theme.RankifyGlassBorder
import com.example.ui.theme.RankifyGlassSurface
import com.example.ui.theme.RankifyPrimary
import com.example.ui.theme.RankifyPurple
import com.example.ui.theme.RankifyPurpleLight
import com.example.viewmodel.RankifyUiState
import com.example.viewmodel.RankifyViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private const val URL_CHATGPT = "https://chatgpt.com/"
private const val URL_GEMINI = "https://gemini.google.com/app"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiHubScreen(
    state: RankifyUiState,
    viewModel: RankifyViewModel,
    initialPrompt: String = "",
    initialSubject: String = "Physics",
    initialChapter: String = "Current Electricity"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 0: AI Tutor, 1: Voice AI, 2: Image Studio
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    val tutorMessages = state.chatMessages
    val isTutorStreaming = state.isTutorStreaming
    val streamingTutorMessage = state.streamingTutorMessage
    val currentStudyMode = state.currentStudyMode
    
    val chatListState = rememberLazyListState()
    
    LaunchedEffect(tutorMessages.size, isTutorStreaming, streamingTutorMessage) {
        if (tutorMessages.isNotEmpty() || streamingTutorMessage.isNotBlank()) {
            chatListState.animateScrollToItem(
                if (isTutorStreaming) tutorMessages.size else tutorMessages.size.coerceAtLeast(1) - 1
            )
        }
    }
    var isVoiceSessionActive by remember { mutableStateOf(false) }
    var lastGenerateClickTime by remember { mutableLongStateOf(0L) }

    // TTS & Voice Speech Manager
    val voiceSpeechManager = remember { VoiceSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            voiceSpeechManager.stopSpeaking()
            voiceSpeechManager.stopListening()
            voiceSpeechManager.release()
            isVoiceSessionActive = false
        }
    }
    val isTtsSpeaking by voiceSpeechManager.isTtsSpeaking.collectAsState()
    val isSpeechListening by voiceSpeechManager.isListening.collectAsState()
    val liveSpokenTranscript by voiceSpeechManager.spokenTextLive.collectAsState()

    // Context from other screens
    var contextText by remember { mutableStateOf(initialPrompt) }
    var hasCopied by remember { mutableStateOf(false) }

    LaunchedEffect(initialPrompt) {
        if (initialPrompt.isNotBlank()) {
            contextText = initialPrompt
            hasCopied = false
        }
    }

    // --- Voice Tab State ---
    val voiceMessages = remember {
        mutableStateListOf(
            LiveVoiceMessage(
                role = "assistant",
                text = "Hello! I am your Rankify AI Voice Coach. Tap the mic to ask any Class 12 CBSE PCM doubt or request a concept summary."
            )
        )
    }
    var typedVoiceInput by remember { mutableStateOf("") }
    var isVoiceApiLoading by remember { mutableStateOf(false) }
    var voiceApiError by remember { mutableStateOf<String?>(null) }
    val voiceListState = rememberLazyListState()

    fun sendVoiceMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || isVoiceApiLoading) return

        voiceSpeechManager.stopSpeaking()
        val userMsg = LiveVoiceMessage(role = "user", text = trimmed)
        voiceMessages.add(userMsg)
        typedVoiceInput = ""
        isVoiceApiLoading = true
        voiceApiError = null

        scope.launch {
            voiceListState.animateScrollToItem((voiceMessages.size - 1).coerceAtLeast(0))
            val result = GeminiService.getLiveVoiceResponse(
                history = voiceMessages.toList(),
                userMessage = trimmed,
                subjectContext = initialSubject,
                chapterContext = initialChapter
            )
            isVoiceApiLoading = false
            result.fold(
                onSuccess = { reply ->
                    viewModel.contributeAiActivityToChapter(initialSubject, initialChapter)
                    val assistantMsg = LiveVoiceMessage(role = "assistant", text = reply)
                    voiceMessages.add(assistantMsg)
                    voiceSpeechManager.speak(reply)
                    voiceListState.animateScrollToItem(voiceMessages.size - 1)
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    val friendlyVoiceError = if (rawMsg.contains("temporarily unavailable", ignoreCase = true) ||
                        rawMsg.contains("streaming", ignoreCase = true) ||
                        rawMsg.contains("models/", ignoreCase = true) ||
                        rawMsg.contains("HTTP", ignoreCase = true)) {
                        "Voice AI is temporarily unavailable."
                    } else if (rawMsg.isNotBlank() && !rawMsg.contains("{")) {
                        rawMsg
                    } else {
                        "Voice AI is temporarily unavailable."
                    }
                    voiceApiError = friendlyVoiceError
                    Toast.makeText(context, friendlyVoiceError, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Android Speech Recognizer System Intent Launcher (universal fallback)
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                sendVoiceMessage(spoken)
            }
        }
    }

    // Microphone Permission Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your Class 12 PCM question...")
                }
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                // In-app recognition fallback
                voiceSpeechManager.startListening(
                    onResult = { recognizedText -> sendVoiceMessage(recognizedText) },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                )
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice conversation.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerVoiceInput() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your Class 12 PCM question...")
                }
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                voiceSpeechManager.startListening(
                    onResult = { recognizedText -> sendVoiceMessage(recognizedText) },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                )
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // --- Image Tab State (Create & Edit) ---
    var imagePrompt by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }
    var baseImageForEdit by remember { mutableStateOf<Bitmap?>(null) }
    var isImageGenerating by remember { mutableStateOf(false) }
    var lastGeneratedImageResult by remember { mutableStateOf<GeminiImageResult?>(null) }
    var imageGenerationError by remember { mutableStateOf<String?>(null) }

    // Photo picker to select an existing image for editing
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    baseImageForEdit = bitmap
                    Toast.makeText(context, "Image loaded for editing! Add your prompt below.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateOrEditImage() {
        val now = System.currentTimeMillis()
        if (now - lastGenerateClickTime < 1500L) {
            // Debounce rapid repeated clicks to prevent duplicate API requests and quota drain
            return
        }
        val trimmed = imagePrompt.trim()
        if (trimmed.isBlank() || isImageGenerating) return

        lastGenerateClickTime = now
        isImageGenerating = true
        imageGenerationError = null

        scope.launch {
            val result = GeminiService.generateOrEditImage(
                prompt = trimmed,
                baseImage = baseImageForEdit,
                aspectRatio = selectedAspectRatio
            )
            isImageGenerating = false
            result.fold(
                onSuccess = { imageResult ->
                    viewModel.contributeAiActivityToChapter(initialSubject, initialChapter)
                    lastGeneratedImageResult = imageResult
                    Toast.makeText(
                        context,
                        if (baseImageForEdit != null) "Image edited successfully!" else "Image generated!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    val friendlyMsg = if (rawMsg.contains("quota", ignoreCase = true) ||
                        rawMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                        "Daily AI image quota exhausted. Please try again later."
                    } else if (rawMsg.isNotBlank() && !rawMsg.contains("{") && !rawMsg.contains("HTTP")) {
                        rawMsg
                    } else {
                        "Unable to generate image right now. Please try again later."
                    }
                    imageGenerationError = friendlyMsg
                    Toast.makeText(context, friendlyMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Helper functions for clipboard and external urls
    fun copyToClipboard(text: String, label: String = "Study Context") {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        hasCopied = true
        Toast.makeText(context, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Tabs for Ask AI (Ask Doubt is default)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = RankifyPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = RankifyPrimary,
                    height = 3.dp
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Ask Doubt",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ask Doubt",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("tab_ask_doubt")
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Rankify AI Beta",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI (Beta)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("tab_ai_beta")
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: PREPARE DOUBT & EXTERNAL LAUNCHERS
                    AskDoubtTabContent(
                        contextText = contextText,
                        onContextTextChange = {
                            contextText = it
                            hasCopied = false
                        },
                        hasCopied = hasCopied,
                        onCopy = { text, label -> copyToClipboard(text, label) },
                        onOpenUrl = { url -> openExternalUrl(url) },
                        initialSubject = initialSubject,
                        initialChapter = initialChapter
                    )
                }
                1 -> {
                    // TAB 1: RANKIFY AI BETA (COMING SOON)
                    AiTutorBetaTabContent()
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 0: VOICE AI SCREEN (Powered by gemini-3.8-live)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceAiTabContent(
    voiceMessages: List<LiveVoiceMessage>,
    isTtsSpeaking: Boolean,
    isListening: Boolean,
    liveSpokenTranscript: String,
    isVoiceApiLoading: Boolean,
    typedVoiceInput: String,
    onTypedVoiceInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onTriggerMic: () -> Unit,
    onStopSpeaking: () -> Unit,
    onReplayAudio: (String) -> Unit,
    onClearConversation: () -> Unit,
    voiceListState: androidx.compose.foundation.lazy.LazyListState,
    initialSubject: String,
    initialChapter: String,
    isSessionActive: Boolean,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    errorMessage: String?
) {
    if (!isSessionActive) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice AI",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Voice AI Study Coach",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Interactive, real-time voice assistance for Class 12 CBSE PCM ($initialSubject • $initialChapter). Speak your questions and get instant spoken guidance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onStartSession,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(52.dp)
                    .testTag("start_voice_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Voice",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status & Live Voice Visualizer Bar
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, RankifyGlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                RankifyPrimary.copy(alpha = 0.08f),
                                RankifyPurple.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTtsSpeaking) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Live Voice Session",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RankifyPurple.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Online",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = RankifyPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = when {
                                    isTtsSpeaking -> "AI Speaking... (Tap Stop to pause)"
                                    isListening -> "Listening to your voice..."
                                    isVoiceApiLoading -> "Connecting to Voice AI..."
                                    else -> "Ready for your question ($initialSubject • $initialChapter)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isTtsSpeaking || isListening) RankifyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isTtsSpeaking) {
                            IconButton(
                                onClick = onStopSpeaking,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Speaking",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        } else if (voiceMessages.size > 1) {
                            IconButton(
                                onClick = onClearConversation,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Conversation",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onEndSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("end_voice_button")
                        ) {
                            Text("End", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Waveform indicator when voice is active
                AnimatedVisibility(visible = isTtsSpeaking || isListening) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RankifyPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(6) { index ->
                                val barHeight = remember(index) { (12 + (index * 4) % 18).dp }
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (index % 2 == 0) RankifyPrimary else RankifyPurple)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTtsSpeaking) "Streaming voice response..." else "Transcribing speech: $liveSpokenTranscript",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = RankifyPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Spoken Prompts Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickPrompts = listOf(
                "Explain Lenz's Law clearly",
                "How to balance redox reactions?",
                "Derive electric field of dipole",
                "Give me 1 quick practice question"
            )
            quickPrompts.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, RankifyGlassBorder),
                    modifier = Modifier.clickable { onSendMessage(prompt) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = RankifyPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Conversation List
        LazyColumn(
            state = voiceListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(voiceMessages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) RankifyPrimary else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isUser) null else BorderStroke(1.dp, RankifyGlassBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth(0.88f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isUser) Icons.Default.Mic else Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = if (isUser) Color.White.copy(alpha = 0.8f) else RankifyPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isUser) "You (Spoken)" else "Rankify Voice Coach",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isUser) Color.White.copy(alpha = 0.9f) else RankifyPurple
                                    )
                                }

                                if (!isUser) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { onReplayAudio(msg.text) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak",
                                                tint = RankifyPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (isVoiceApiLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, RankifyGlassBorder),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = RankifyPrimary
                                )
                                Text(
                                    text = "gemini-3.8-live is generating voice reply...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Input Row: Mic Trigger & Text Fallback
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Mic Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(if (isListening) pulseScale else 1.0f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (isListening) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            else listOf(RankifyPrimary, RankifyPurple)
                        )
                    )
                    .clickable { onTriggerMic() }
                    .testTag("voice_ai_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Speak into mic",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Typed Voice Input Field
            OutlinedTextField(
                value = typedVoiceInput,
                onValueChange = onTypedVoiceInputChange,
                placeholder = { Text("Tap mic or type doubt...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_ai_input_field"),
                shape = RoundedCornerShape(18.dp),
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RankifyPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendMessage(typedVoiceInput) }),
                trailingIcon = {
                    if (typedVoiceInput.isNotBlank()) {
                        IconButton(
                            onClick = { onSendMessage(typedVoiceInput) },
                            modifier = Modifier.testTag("voice_ai_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = RankifyPrimary
                            )
                        }
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: CREATE & EDIT IMAGES (Powered by gemini-3.1-flash-image-preview)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEditImageTabContent(
    prompt: String,
    onPromptChange: (String) -> Unit,
    aspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    baseImage: Bitmap?,
    onClearBaseImage: () -> Unit,
    onPickGalleryImage: () -> Unit,
    isGenerating: Boolean,
    lastResult: GeminiImageResult?,
    errorMessage: String?,
    onGenerateOrEdit: () -> Unit,
    onUseAsBaseForEdit: (Bitmap) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, RankifyGlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Image Studio",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Create & Edit Images",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Powered by gemini-3.1-flash-image-preview",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = RankifyPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Generate clear textbook ray diagrams, chemical structures, physical setups, or load an existing image to edit with natural language instructions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        // Active Base Image Banner (if user is in Edit Mode)
        if (baseImage != null) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyPurple.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = RankifyPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Image Editing Mode Active",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = RankifyPurple
                            )
                        }
                        IconButton(
                            onClick = onClearBaseImage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Base Image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            bitmap = baseImage.asImageBitmap(),
                            contentDescription = "Base image to edit",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "Base image selected for editing",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your text prompt below will transform this image using gemini-3.1-flash-image-preview.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Prompt Input & Options Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, RankifyGlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (baseImage != null) "Describe Your Image Edit" else "Describe Image or Diagram to Create",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (baseImage != null)
                        "Specify what changes to make (e.g. 'Add ray labels and arrows', 'Highlight the focal length in yellow', 'Convert to dark chalkboard style')."
                    else
                        "e.g. 'Ray optics diagram of a convex lens with object between F and 2F showing real, inverted, magnified image formation.'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    placeholder = {
                        Text(
                            if (baseImage != null) "Describe modifications to the base image..."
                            else "Enter detailed visual prompt for gemini-3.1-flash-image-preview..."
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_image_prompt_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RankifyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Aspect Ratio Selector
                Text(
                    text = "Aspect Ratio",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ratios = listOf("1:1", "16:9", "4:3", "9:16")
                    ratios.forEach { ratio ->
                        FilterChip(
                            selected = aspectRatio == ratio,
                            onClick = { onAspectRatioChange(ratio) },
                            label = { Text(ratio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RankifyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Generate & Load Base Image
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickGalleryImage,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick Image",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (baseImage != null) "Change Image" else "Load to Edit",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onGenerateOrEdit,
                        enabled = prompt.isNotBlank() && !isGenerating,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("generate_image_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(
                                imageVector = if (baseImage != null) Icons.Default.Edit else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (baseImage != null) "Edit Image" else "Generate Image",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Quick Suggestion Chips for PCM Diagrams
        Column {
            Text(
                text = "Preset Class 12 PCM Diagram Prompts",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "Convex lens ray diagram with object at 2F",
                    "Galvanic cell with salt bridge & electrodes",
                    "Electric dipole field lines diagram",
                    "AC generator coil and slip rings",
                    "Wheatstone bridge balanced circuit",
                    "Benzene resonance structures"
                )
                presets.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, RankifyGlassBorder),
                        modifier = Modifier.clickable { onPromptChange(preset) }
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Result Card (Generated / Edited Image)
        if (lastResult?.bitmap != null) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Generated Result",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RankifyPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "gemini-3.1-flash-image",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = RankifyPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = lastResult.bitmap.asImageBitmap(),
                        contentDescription = "Generated Diagram",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Fit
                    )

                    if (lastResult.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = lastResult.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onUseAsBaseForEdit(lastResult.bitmap) },
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit this Image",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit this Image with New Prompt",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: PREPARE DOUBT & EXTERNAL LAUNCHERS
// -------------------------------------------------------------------------------------------------
@Composable
private fun AskDoubtTabContent(
    contextText: String,
    onContextTextChange: (String) -> Unit,
    hasCopied: Boolean,
    onCopy: (String, String) -> Unit,
    onOpenUrl: (String) -> Unit,
    initialSubject: String,
    initialChapter: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Context / Doubt Input Box
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, RankifyGlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = RankifyPrimary, modifier = Modifier.size(20.dp))
                        Text(text = "Your Doubt / Question", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (initialSubject.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RankifyPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$initialSubject • $initialChapter",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = RankifyPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contextText,
                    onValueChange = onContextTextChange,
                    placeholder = {
                        Text("e.g. Explain Lenz's Law with practical examples and CBSE board numerical tips...")
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_hub_query_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RankifyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (contextText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onCopy(contextText, "Question Context") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasCopied) Color(0xFF16A34A) else RankifyPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_context_button")
                        ) {
                            Icon(
                                imageVector = if (hasCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (hasCopied) "Copied!" else "Copy Prompt")
                        }

                        OutlinedButton(
                            onClick = { onContextTextChange("") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        // Section Title
        Text(
            text = "Official AI Companion Apps",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // ChatGPT Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFF10A37F).copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (contextText.isNotBlank() && !hasCopied) onCopy(contextText, "ChatGPT Prompt")
                    onOpenUrl(URL_CHATGPT)
                }
                .testTag("ask_chatgpt_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10A37F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = "ChatGPT", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(text = "Ask ChatGPT", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "OpenAI • chatgpt.com", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF10A37F))
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF10A37F))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Best for conversational step-by-step problem breakdown, numerical problem solving, and voice conversations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Gemini Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, RankifyPrimary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (contextText.isNotBlank() && !hasCopied) onCopy(contextText, "Gemini Prompt")
                    onOpenUrl(URL_GEMINI)
                }
                .testTag("ask_gemini_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(RankifyPrimary, RankifyPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = "Gemini", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(text = "Ask Gemini", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Google • gemini.google.com", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = RankifyPrimary)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = RankifyPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Best for multimodal diagram reading, camera scan of NCERT questions, real-time web grounding, and deep reasoning.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Privacy Guarantee Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, RankifyGlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Privacy",
                    tint = RankifyPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Privacy Protected: Rankify handles AI requests securely. Spoken queries and image prompts are processed directly with official Google Gemini endpoints.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
