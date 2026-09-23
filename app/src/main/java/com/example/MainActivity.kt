package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.FirebaseAuthService
import com.example.notifications.NotificationActionReceiver
import com.example.notifications.NotificationDispatcher
import com.example.notifications.NotificationPreferencesManager
import com.example.notifications.model.NotificationDeepLinks
import com.example.ui.components.RankifyBottomBar
import com.example.ui.components.RankifyTopBar
import com.example.ui.components.RewardCelebrationPopup
import com.example.ui.screens.AddStudyMaterialScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AddMusicVideoScreen
import com.example.ui.screens.AdminMusicVideoManagerScreen
import com.example.ui.screens.RankifyVideoPlayerScreen
import com.example.ui.screens.AiHubScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationSettingsScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.StudyDockScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.StudyPlannerSetupScreen
import com.example.ui.screens.RewardHistoryDialog
import com.example.ui.screens.RankifyExclusiveScreen
import com.example.ui.screens.RankifyMusicVaultScreen
import com.example.ui.screens.SubjectSongsScreen
import com.example.ui.theme.RankifyTheme
import com.example.viewmodel.ExclusiveDestination
import com.example.viewmodel.RankifyViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: RankifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RankifyApplication.ensureFirebase(this)
        enableEdgeToEdge()
        handleShareIntent(intent)
        handleNotificationDeepLink(intent)
        setContent {
            RankifyTheme {
                RankifyApp(viewModel = mainViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NotificationPreferencesManager.getInstance(this).recordAppOpened()
    }

    override fun onStop() {
        super.onStop()
        // Sync when app goes to background or closes
        mainViewModel.performSmartSync()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
        handleNotificationDeepLink(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedCharSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            val sharedText = sharedCharSequence?.toString()
                ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()

            mainViewModel.openStudyDockWithSharedContent(sharedText)
        }
    }

    private fun handleNotificationDeepLink(intent: Intent?) {
        if (intent == null) return
        val deepLink = intent.getStringExtra(NotificationDispatcher.EXTRA_DEEP_LINK) ?: return
        val notifId = intent.getStringExtra(NotificationDispatcher.EXTRA_NOTIFICATION_ID)

        if (!notifId.isNullOrBlank()) {
            val receiverIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_OPENED
                putExtra(NotificationDispatcher.EXTRA_NOTIFICATION_ID, notifId)
            }
            sendBroadcast(receiverIntent)
        }

        when (deepLink) {
            NotificationDeepLinks.WEAK_CHAPTER -> mainViewModel.setTab(2)
            NotificationDeepLinks.TODAY_TARGET -> mainViewModel.setTab(1)
            NotificationDeepLinks.REVISION -> mainViewModel.setTab(1)
            NotificationDeepLinks.EXAM_PLANNER -> mainViewModel.openProfile()
            NotificationDeepLinks.SYLLABUS_TRACKER -> mainViewModel.setTab(1)
            NotificationDeepLinks.AI_TUTOR -> mainViewModel.setTab(3)
            NotificationDeepLinks.STUDY_SESSION -> mainViewModel.openStudyDockWithSharedContent("")
            NotificationDeepLinks.PRACTICE -> mainViewModel.setTab(2)
            NotificationDeepLinks.NOTIFICATION_SETTINGS -> mainViewModel.openNotificationSettings()
            NotificationDeepLinks.NOTIFICATION_TESTING -> mainViewModel.openNotificationSettings()
            NotificationDeepLinks.EXCLUSIVE -> mainViewModel.openExclusiveMain()
            NotificationDeepLinks.MUSIC_VAULT -> mainViewModel.openMusicVault()
            NotificationDeepLinks.COMMUNITY -> mainViewModel.setTab(0)
            NotificationDeepLinks.STREAK -> mainViewModel.openProfile()
            NotificationDeepLinks.LEADERBOARD -> mainViewModel.setTab(0)
            NotificationDeepLinks.FORMULAS -> mainViewModel.setTab(2)
            else -> mainViewModel.setTab(0)
        }
    }
}

@Composable
fun RankifyApp(viewModel: RankifyViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val studyDockInputUrl by viewModel.studyDockInputUrl.collectAsState()
    val studyDockSharedBanner by viewModel.studyDockSharedBanner.collectAsState()
    val studyDockRequested by viewModel.studyDockRequested.collectAsState()
    val isAdminDashboardVisible by viewModel.isAdminDashboardVisible.collectAsState()
    val isAddMaterialScreenVisible by viewModel.isAddMaterialScreenVisible.collectAsState()
    val isAdminMusicVideoManagerVisible by viewModel.isAdminMusicVideoManagerVisible.collectAsState()
    val isAddMusicVideoScreenVisible by viewModel.isAddMusicVideoScreenVisible.collectAsState()
    val isPreviewPlayerVisible by viewModel.isPreviewPlayerVisible.collectAsState()
    val currentPreviewVideo by viewModel.currentPreviewVideo.collectAsState()
    val currentPreviewSource by viewModel.currentPreviewSource.collectAsState()
    val isPreviewLoading by viewModel.isPreviewLoading.collectAsState()
    val previewError by viewModel.previewError.collectAsState()
    val editingMusicVideo by viewModel.editingMusicVideo.collectAsState()
    val isExclusiveVisible by viewModel.isExclusiveScreenVisible.collectAsState()
    val exclusiveDestination by viewModel.exclusiveDestination.collectAsState()
    val rewardCelebration by viewModel.rewardCelebration.collectAsState()
    val isSpHistoryVisible by viewModel.isSpHistoryVisible.collectAsState()
    val studyPointTransactions by viewModel.studyPointTransactions.collectAsState()
    val showStudyPlannerScreen by viewModel.showStudyPlannerScreen.collectAsState()
    val isNotificationSettingsVisible by viewModel.isNotificationSettingsVisible.collectAsState()

    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Sync when app goes to background or pauses
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewModel.performSmartSync()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showAuthScreen by remember { mutableStateOf(false) }
    var showStudyDockScreen by remember { mutableStateOf(false) }

    // Synchronize StudyDock screen visibility with explicit requests (e.g. from Android Share)
    val isStudyDockVisible = showStudyDockScreen || studyDockRequested

    var aiPromptToPass by remember { mutableStateOf("") }
    var aiSubjectToPass by remember { mutableStateOf("Physics") }
    var aiChapterToPass by remember { mutableStateOf("Current Electricity") }

    val currentAuthUser = FirebaseAuthService.currentUser
    val isUserLoggedIn = (currentAuthUser != null && !currentAuthUser.isAnonymous) || state.isLoggedIn
    val isGuestMode = (currentAuthUser != null && currentAuthUser.isAnonymous) || state.isGuestMode

    // System back returns to Home if on StudyDock or other tab
    BackHandler(enabled = isAdminDashboardVisible) {
        if (isAddMaterialScreenVisible) {
            viewModel.dismissAddMaterialScreen()
        } else {
            viewModel.dismissAdminDashboard()
        }
    }

    BackHandler(enabled = isStudyDockVisible && !isAdminDashboardVisible && !isPreviewPlayerVisible) {
        showStudyDockScreen = false
        viewModel.dismissStudyDock()
    }

    BackHandler(enabled = isPreviewPlayerVisible) {
        viewModel.closeVideoPreview()
    }

    BackHandler(enabled = currentTab != 0 && !showAuthScreen && !isStudyDockVisible && !isAdminDashboardVisible && !isExclusiveVisible && !isPreviewPlayerVisible) {
        viewModel.setTab(0)
    }

    BackHandler(enabled = isExclusiveVisible && !isStudyDockVisible && !isAdminDashboardVisible) {
        viewModel.navigateBackExclusive()
    }

    BackHandler(enabled = isNotificationSettingsVisible) {
        viewModel.dismissNotificationSettings()
    }

    // If user is neither logged in with Firebase nor selected guest mode, present authentication first
    if (!isUserLoggedIn && !isGuestMode) {
        AuthScreen(
            state = state,
            viewModel = viewModel,
            onAuthSuccess = { /* Automatically transitions to main app */ },
            onContinueAsGuest = {
                viewModel.signInAsGuest()
            }
        )
        return
    }

    // Modal view for Auth / Account Management when opened while logged out
    if (showAuthScreen) {
        BackHandler {
            showAuthScreen = false
        }
        AuthScreen(
            state = state,
            viewModel = viewModel,
            onAuthSuccess = { showAuthScreen = false },
            onContinueAsGuest = { showAuthScreen = false },
            onDismiss = { showAuthScreen = false }
        )
        return
    }

    // First-Time AI Study Planner Screen
    // Shown once after account creation or first login, or if user explicitly resets their plan
    if (showStudyPlannerScreen) {
        BackHandler {
            viewModel.dismissStudyPlanner()
        }
        StudyPlannerSetupScreen(
            viewModel = viewModel,
            onFinished = {
                viewModel.dismissStudyPlanner()
            }
        )
        return
    }

    // Smart Notification Settings Screen (Overlay)
    if (isNotificationSettingsVisible) {
        NotificationSettingsScreen(
            onNavigateBack = {
                viewModel.dismissNotificationSettings()
            }
        )
        return
    }

    // Reward Celebration Overlay
    rewardCelebration?.let { message ->
        RewardCelebrationPopup(
            message = message,
            onDismiss = { viewModel.clearCelebration() }
        )
    }

    // Reward History Dialog
    if (isSpHistoryVisible) {
        RewardHistoryDialog(
            spTransactions = studyPointTransactions,
            onDismiss = { viewModel.dismissSpHistory() }
        )
    }

    // Video Preview Player (Overlay)
    if (isPreviewPlayerVisible && currentPreviewVideo != null) {
        RankifyVideoPlayerScreen(
            video = currentPreviewVideo!!,
            source = currentPreviewSource,
            isLoading = isPreviewLoading,
            error = previewError,
            onNavigateBack = {
                viewModel.closeVideoPreview()
            }
        )
        return
    }

    // Full screen StudyDock navigation (Back returns to Home, bottom navigation remains untouched)
    if (isStudyDockVisible) {
        StudyDockScreen(
            initialUrl = studyDockInputUrl,
            initialSharedBanner = studyDockSharedBanner,
            onUrlChanged = { newUrl ->
                viewModel.updateStudyDockInputUrl(newUrl)
            },
            viewModel = viewModel,
            onNavigateBack = {
                showStudyDockScreen = false
                viewModel.dismissStudyDock()
            }
        )
        return
    }

    // Rankify Exclusive Section (Overlay Navigation)
    if (isExclusiveVisible) {
        when (val dest = exclusiveDestination) {
            is ExclusiveDestination.Main -> RankifyExclusiveScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateBackExclusive() }
            )
            is ExclusiveDestination.MusicVault -> RankifyMusicVaultScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateBackExclusive() }
            )
            is ExclusiveDestination.SubjectSongs -> SubjectSongsScreen(
                subject = dest.subject,
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateBackExclusive() }
            )
        }
        return
    }

    // Dedicated Private Admin Dashboard Navigation
    // Protected both in ViewModel and within the screen itself against unauthorized access
    if (isAdminDashboardVisible) {
        if (isAddMaterialScreenVisible) {
            AddStudyMaterialScreen(
                viewModel = viewModel,
                adminAccessState = state.adminAccessState,
                onNavigateBack = {
                    viewModel.dismissAddMaterialScreen()
                }
            )
        } else if (isAddMusicVideoScreenVisible) {
            AddMusicVideoScreen(
                viewModel = viewModel,
                adminAccessState = state.adminAccessState,
                videoToEdit = editingMusicVideo,
                onNavigateBack = {
                    viewModel.closeAddMusicVideoScreen()
                }
            )
        } else if (isAdminMusicVideoManagerVisible) {
            AdminMusicVideoManagerScreen(
                viewModel = viewModel,
                adminAccessState = state.adminAccessState,
                onNavigateBack = {
                    viewModel.closeAdminMusicVideoManager()
                }
            )
        } else {
            AdminDashboardScreen(
                adminAccessState = state.adminAccessState,
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.dismissAdminDashboard()
                },
                onOpenAddMaterial = {
                    viewModel.openAddMaterialScreen()
                },
                onOpenMusicVideoManager = {
                    viewModel.openAdminMusicVideoManager()
                }
            )
        }
        return
    }

    val studentDisplayName = state.profile?.name?.takeIf { it.isNotBlank() } ?: "Student"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            RankifyTopBar(
                streakDays = state.profile?.streakDays ?: 5,
                studentName = studentDisplayName,
                activeExam = state.activeExam,
                isLoggedIn = isUserLoggedIn,
                isGuestMode = isGuestMode,
                userEmail = state.authUserEmail ?: currentAuthUser?.email,
                syncStatus = state.syncStatus,
                isOnline = state.isOnline,
                canNavigateBack = currentTab != 0,
                onBackClick = {
                    viewModel.setTab(0)
                },
                onAccountClick = {
                    val authUser = FirebaseAuthService.currentUser
                    val isAuthenticated = (authUser != null && !authUser.isAnonymous) || state.isLoggedIn
                    if (isAuthenticated) {
                        viewModel.openProfile()
                    } else {
                        showAuthScreen = true
                    }
                },
                onExamClick = {
                    viewModel.openProfile()
                },
                onSyncClick = {
                    viewModel.performSmartSync()
                },
                onNotificationsClick = {
                    viewModel.openNotificationSettings()
                }
            )
        },
        bottomBar = {
            RankifyBottomBar(
                currentTab = currentTab,
                onTabSelected = { tabIndex ->
                    viewModel.setTab(tabIndex)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    state = state,
                    viewModel = viewModel,
                    onNavigateTab = { index -> viewModel.setTab(index) },
                    onOpenAiWithPrompt = { prompt, subject, chapter ->
                        aiPromptToPass = prompt
                        aiSubjectToPass = subject
                        aiChapterToPass = chapter
                        viewModel.setTab(3)
                    },
                    onOpenStudyDock = {
                        showStudyDockScreen = true
                    }
                )
                1 -> StudyScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenAiWithPrompt = { prompt, subject, chapter ->
                        aiPromptToPass = prompt
                        aiSubjectToPass = subject
                        aiChapterToPass = chapter
                        viewModel.setTab(3)
                    }
                )
                2 -> PracticeScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenAiWithPrompt = { prompt, subject, chapter ->
                        aiPromptToPass = prompt
                        aiSubjectToPass = subject
                        aiChapterToPass = chapter
                        viewModel.setTab(3)
                    }
                )
                3 -> AiHubScreen(
                    state = state,
                    viewModel = viewModel,
                    initialPrompt = aiPromptToPass,
                    initialSubject = aiSubjectToPass,
                    initialChapter = aiChapterToPass
                )
                4 -> ProgressScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenAiWithPrompt = { prompt, subject, chapter ->
                        aiPromptToPass = prompt
                        aiSubjectToPass = subject
                        aiChapterToPass = chapter
                        viewModel.setTab(3)
                    }
                )
            }
        }
    }
}
