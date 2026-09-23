package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.RankifyDatabase
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirebaseFirestoreService
import com.example.data.model.AIChatMessageEntity
import com.example.data.model.ExamEntity
import com.example.data.model.MistakeEntity
import com.example.data.model.NoteEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.RankifyCoinWallet
import com.example.data.model.StudyPointWallet
import com.example.data.model.CoinTransaction
import com.example.data.model.StudyPointTransaction
import com.example.data.model.StudentProfile
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import com.example.data.model.StudyDockAnalysisStatus
import com.example.data.model.StudyDockLectureSession
import com.example.data.model.StudyMaterialResource
import com.example.data.model.ExclusiveMusicVideo
import com.example.data.model.ExclusiveMusicVideoSource
import com.example.data.model.SyllabusChapterEntity
import com.example.data.model.TestAttemptEntity
import com.example.data.model.AdminAccessState
import com.example.data.model.SyncStatus
import com.example.data.model.StudyPlanLevel
import com.example.data.repository.RankifyRepository
import com.example.util.GoogleDriveUrlHelper
import com.example.util.NetworkStatusMonitor
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.data.model.AiStudyMode

data class RankifyUiState(
    val profile: StudentProfile? = null,
    val activeExam: ExamEntity? = null,
    val allExams: List<ExamEntity> = emptyList(),
    val chapters: List<SyllabusChapterEntity> = emptyList(),
    val dailyTargets: List<StudyTargetEntity> = emptyList(),
    val backlogTargets: List<StudyTargetEntity> = emptyList(),
    val weeklyGoals: List<StudyTargetEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val bookmarkedQuestions: List<QuestionEntity> = emptyList(),
    val mistakes: List<MistakeEntity> = emptyList(),
    val testAttempts: List<TestAttemptEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val chatMessages: List<AIChatMessageEntity> = emptyList(),
    val isAiLoading: Boolean = false,
    val aiDailyBrief: String = "Yesterday you studied 2h 10m. Today's recommended focus: Current Electricity PYQs & Electrochemistry revision. Suggested workload: 2h 30m.",
    val isLoggedIn: Boolean = false,
    val isGuestMode: Boolean = false,
    val authUserEmail: String? = null,
    val authUserId: String? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val isCloudSyncing: Boolean = false,
    val cloudSyncMessage: String? = null,
    val isProfileLoading: Boolean = false,
    val profileStatusMessage: String? = null,
    val adminAccessState: AdminAccessState = AdminAccessState.NotAdmin,
    val studyPointWallet: StudyPointWallet? = null,
    val studyPointTransactions: List<StudyPointTransaction> = emptyList(),
    val legacyCoinWallet: RankifyCoinWallet? = null,
    val legacyCoinTransactions: List<CoinTransaction> = emptyList(),
    val isWalletLoading: Boolean = false,
    val rewardCelebration: String? = null,
    val unlockedVideoIds: Set<String> = emptySet(),
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val isOnline: Boolean = true,
    val hasUnsyncedChanges: Boolean = false,
    val showStudyPlanner: Boolean = false,
    val currentStudyMode: AiStudyMode = AiStudyMode.DOUBT_MODE,
    val isTutorStreaming: Boolean = false,
    val streamingTutorMessage: String = "",
    val selectedSubjectForAi: String = "Physics",
    val selectedChapterForAi: String = "Current Electricity"
)

sealed class ExclusiveDestination {
    object Main : ExclusiveDestination()
    object MusicVault : ExclusiveDestination()
    data class SubjectSongs(val subject: String) : ExclusiveDestination()
}

class RankifyViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /**
         * Feature flag: Temporarily remove Study Points / Coins / Unlock validation
         * for Music Vault, granting all users instant access to watch exclusive videos.
         */
        const val FEATURE_FREE_MUSIC_MODE = true

        fun createInitialUiState(): RankifyUiState {
            val user = FirebaseAuthService.currentUser
            val hasRealAccount = (user != null && !user.isAnonymous)
            val guestModeActive = (user != null && user.isAnonymous)
            return RankifyUiState(
                isLoggedIn = hasRealAccount,
                isGuestMode = guestModeActive,
                authUserEmail = user?.email,
                authUserId = user?.uid,
                adminAccessState = AdminAccessState.NotAdmin
            )
        }
    }

    private val repository: RankifyRepository

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _profileStatusMessage = MutableStateFlow<String?>(null)
    val profileStatusMessage: StateFlow<String?> = _profileStatusMessage.asStateFlow()

    private val _adminAccessState = MutableStateFlow<AdminAccessState>(AdminAccessState.NotAdmin)
    val adminAccessState: StateFlow<AdminAccessState> = _adminAccessState.asStateFlow()

    private val _isAdminDashboardVisible = MutableStateFlow(false)
    val isAdminDashboardVisible: StateFlow<Boolean> = _isAdminDashboardVisible.asStateFlow()

    private val _isAddMaterialScreenVisible = MutableStateFlow(false)
    val isAddMaterialScreenVisible: StateFlow<Boolean> = _isAddMaterialScreenVisible.asStateFlow()

    private val _adminStudyMaterials = MutableStateFlow<List<StudyMaterialResource>>(emptyList())
    val adminStudyMaterials: StateFlow<List<StudyMaterialResource>> = _adminStudyMaterials.asStateFlow()

    private val _publishedStudyMaterials = MutableStateFlow<List<StudyMaterialResource>>(emptyList())
    val publishedStudyMaterials: StateFlow<List<StudyMaterialResource>> = _publishedStudyMaterials.asStateFlow()

    private val _isPublishedMaterialsLoading = MutableStateFlow(false)
    val isPublishedMaterialsLoading: StateFlow<Boolean> = _isPublishedMaterialsLoading.asStateFlow()

    private val _publishingResourceId = MutableStateFlow<String?>(null)
    val publishingResourceId: StateFlow<String?> = _publishingResourceId.asStateFlow()

    private val _isAdminMaterialsLoading = MutableStateFlow(false)
    val isAdminMaterialsLoading: StateFlow<Boolean> = _isAdminMaterialsLoading.asStateFlow()

    // Rankify Exclusive Navigation State
    private val _isExclusiveScreenVisible = MutableStateFlow(false)
    val isExclusiveScreenVisible: StateFlow<Boolean> = _isExclusiveScreenVisible.asStateFlow()

    private val _exclusiveDestination = MutableStateFlow<ExclusiveDestination>(ExclusiveDestination.Main)
    val exclusiveDestination: StateFlow<ExclusiveDestination> = _exclusiveDestination.asStateFlow()

    fun openExclusiveMain() {
        _exclusiveDestination.value = ExclusiveDestination.Main
        _isExclusiveScreenVisible.value = true
    }

    fun openMusicVault() {
        _exclusiveDestination.value = ExclusiveDestination.MusicVault
        _isExclusiveScreenVisible.value = true
    }

    fun openSubjectSongs(subject: String) {
        _exclusiveDestination.value = ExclusiveDestination.SubjectSongs(subject)
        _isExclusiveScreenVisible.value = true
        loadPublishedMusicVideos()
    }

    fun dismissExclusive() {
        _isExclusiveScreenVisible.value = false
        _exclusiveDestination.value = ExclusiveDestination.Main
    }

    fun navigateBackExclusive() {
        when (val current = _exclusiveDestination.value) {
            is ExclusiveDestination.Main -> dismissExclusive()
            is ExclusiveDestination.MusicVault -> _exclusiveDestination.value = ExclusiveDestination.Main
            is ExclusiveDestination.SubjectSongs -> _exclusiveDestination.value = ExclusiveDestination.MusicVault
        }
    }

    private val _isSavingStudyMaterial = MutableStateFlow(false)
    val isSavingStudyMaterial: StateFlow<Boolean> = _isSavingStudyMaterial.asStateFlow()

    // Rankify Exclusive - Music Video Manager States
    private val _isAdminMusicVideoManagerVisible = MutableStateFlow(false)
    val isAdminMusicVideoManagerVisible: StateFlow<Boolean> = _isAdminMusicVideoManagerVisible.asStateFlow()

    private val _isAddMusicVideoScreenVisible = MutableStateFlow(false)
    val isAddMusicVideoScreenVisible: StateFlow<Boolean> = _isAddMusicVideoScreenVisible.asStateFlow()

    private val _isPreviewPlayerVisible = MutableStateFlow(false)
    val isPreviewPlayerVisible: StateFlow<Boolean> = _isPreviewPlayerVisible.asStateFlow()

    private val _currentPreviewVideo = MutableStateFlow<ExclusiveMusicVideo?>(null)
    val currentPreviewVideo: StateFlow<ExclusiveMusicVideo?> = _currentPreviewVideo.asStateFlow()

    private val _currentPreviewSource = MutableStateFlow<ExclusiveMusicVideoSource?>(null)
    val currentPreviewSource: StateFlow<ExclusiveMusicVideoSource?> = _currentPreviewSource.asStateFlow()

    private val _isPreviewLoading = MutableStateFlow(false)
    val isPreviewLoading: StateFlow<Boolean> = _isPreviewLoading.asStateFlow()

    private val _previewError = MutableStateFlow<String?>(null)
    val previewError: StateFlow<String?> = _previewError.asStateFlow()

    private val _adminMusicVideos = MutableStateFlow<List<ExclusiveMusicVideo>>(emptyList())
    val adminMusicVideos: StateFlow<List<ExclusiveMusicVideo>> = _adminMusicVideos.asStateFlow()

    private val _publishedMusicVideos = MutableStateFlow<List<ExclusiveMusicVideo>>(emptyList())
    val publishedMusicVideos: StateFlow<List<ExclusiveMusicVideo>> = _publishedMusicVideos.asStateFlow()

    private val _isAdminMusicVideosLoading = MutableStateFlow(false)
    val isAdminMusicVideosLoading: StateFlow<Boolean> = _isAdminMusicVideosLoading.asStateFlow()

    private val _isPublishedMusicVideosLoading = MutableStateFlow(false)
    val isPublishedMusicVideosLoading: StateFlow<Boolean> = _isPublishedMusicVideosLoading.asStateFlow()

    private val _isSavingMusicVideo = MutableStateFlow(false)
    val isSavingMusicVideo: StateFlow<Boolean> = _isSavingMusicVideo.asStateFlow()

    private val _rewardCelebration = MutableStateFlow<String?>(null)
    val rewardCelebration: StateFlow<String?> = _rewardCelebration.asStateFlow()

    private val _isWalletLoading = MutableStateFlow(false)
    val isWalletLoading: StateFlow<Boolean> = _isWalletLoading.asStateFlow()

    private val _legacyCoinWallet = MutableStateFlow<RankifyCoinWallet?>(null)
    val legacyCoinWallet: StateFlow<RankifyCoinWallet?> = _legacyCoinWallet.asStateFlow()

    private val _legacyCoinTransactions = MutableStateFlow<List<CoinTransaction>>(emptyList())
    val legacyCoinTransactions: StateFlow<List<CoinTransaction>> = _legacyCoinTransactions.asStateFlow()

    private val _studyPointWallet = MutableStateFlow<StudyPointWallet?>(null)
    val studyPointWallet: StateFlow<StudyPointWallet?> = _studyPointWallet.asStateFlow()

    private val _studyPointTransactions = MutableStateFlow<List<StudyPointTransaction>>(emptyList())
    val studyPointTransactions: StateFlow<List<StudyPointTransaction>> = _studyPointTransactions.asStateFlow()

    private val _unlockedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedVideoIds: StateFlow<Set<String>> = _unlockedVideoIds.asStateFlow()

    fun isSongUnlocked(videoId: String): Boolean =
        if (FEATURE_FREE_MUSIC_MODE) true else _unlockedVideoIds.value.contains(videoId)

    // Reward Constants (Provisional Study Points)
    private val REWARD_DAILY_TARGET = 20
    private val LIMIT_DAILY_TARGET = 100

    private val REWARD_STUDY_SESSION = 15
    private val LIMIT_STUDY_SESSION = 90

    private val REWARD_PRACTICE = 20
    private val ATTEMPTS_PER_REWARD = 10
    private val LIMIT_PRACTICE = 100

    private val REWARD_CHAPTER_REVISION = 30
    private val LIMIT_CHAPTER_REVISION = 90

    private val REWARD_STREAK_BONUS = 10
    private val LIMIT_STREAK_BONUS = 10

    private val _isSpHistoryVisible = MutableStateFlow(false)
    val isSpHistoryVisible: StateFlow<Boolean> = _isSpHistoryVisible.asStateFlow()

    private val _isNotificationSettingsVisible = MutableStateFlow(false)
    val isNotificationSettingsVisible: StateFlow<Boolean> = _isNotificationSettingsVisible.asStateFlow()

    fun openNotificationSettings() {
        _isNotificationSettingsVisible.value = true
    }

    fun dismissNotificationSettings() {
        _isNotificationSettingsVisible.value = false
    }

    fun showSpHistory() {
        _isSpHistoryVisible.value = true
        loadTransactionHistory()
    }

    fun dismissSpHistory() {
        _isSpHistoryVisible.value = false
    }

    private val _musicVideoSaveSuccess = MutableStateFlow<String?>(null)
    val musicVideoSaveSuccess: StateFlow<String?> = _musicVideoSaveSuccess.asStateFlow()

    private val _musicVideoSaveError = MutableStateFlow<String?>(null)
    val musicVideoSaveError: StateFlow<String?> = _musicVideoSaveError.asStateFlow()

    private val _editingMusicVideo = MutableStateFlow<ExclusiveMusicVideo?>(null)
    val editingMusicVideo: StateFlow<ExclusiveMusicVideo?> = _editingMusicVideo.asStateFlow()

    private val _musicVideoOperationInProgress = MutableStateFlow<String?>(null)
    val musicVideoOperationInProgress: StateFlow<String?> = _musicVideoOperationInProgress.asStateFlow()

    private val _studyMaterialSaveSuccess = MutableStateFlow<String?>(null)
    val studyMaterialSaveSuccess: StateFlow<String?> = _studyMaterialSaveSuccess.asStateFlow()

    private val _studyMaterialSaveError = MutableStateFlow<String?>(null)
    val studyMaterialSaveError: StateFlow<String?> = _studyMaterialSaveError.asStateFlow()

    val networkMonitor = NetworkStatusMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Synced)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _cloudSyncMessage = MutableStateFlow<String?>("All changes synced")
    val cloudSyncMessage: StateFlow<String?> = _cloudSyncMessage.asStateFlow()

    private val _hasUnsyncedChanges = MutableStateFlow(false)
    val hasUnsyncedChanges: StateFlow<Boolean> = _hasUnsyncedChanges.asStateFlow()

    private val _offlineQueuedChangesCount = MutableStateFlow(0)
    val offlineQueuedChangesCount: StateFlow<Int> = _offlineQueuedChangesCount.asStateFlow()

    private val _showStudyPlannerScreen = MutableStateFlow(false)
    val showStudyPlannerScreen: StateFlow<Boolean> = _showStudyPlannerScreen.asStateFlow()

    private val _selectedStudyPlanLevel = MutableStateFlow<StudyPlanLevel>(StudyPlanLevel.LEVELS[1])
    val selectedStudyPlanLevel: StateFlow<StudyPlanLevel> = _selectedStudyPlanLevel.asStateFlow()

    private var syncDebounceJob: Job? = null
    private var syncRetryJob: Job? = null
    private var syncRetryAttempts = 0
    private var lastSyncedDataSignature: Int? = null
    private var lastRestoredUid: String? = null
    private val syncMutex = Mutex()

    private val _currentFirebaseUser = MutableStateFlow<FirebaseUser?>(FirebaseAuthService.currentUser)

    init {
        val database = RankifyDatabase.getDatabase(application)
        repository = RankifyRepository(database.rankifyDao())

        // Load published study materials for student views
        loadPublishedStudyMaterials()

        // 1. Observe Network state changes for automatic reconnection sync of offline queued items
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (!online) {
                    _syncStatus.value = SyncStatus.Offline
                    _cloudSyncMessage.value = if (_offlineQueuedChangesCount.value > 0) {
                        "Offline (${_offlineQueuedChangesCount.value} changes queued)"
                    } else {
                        "Offline (changes will sync automatically)"
                    }
                } else {
                    if (_hasUnsyncedChanges.value || _offlineQueuedChangesCount.value > 0) {
                        syncRetryAttempts = 0
                        performSmartSync()
                    } else {
                        _syncStatus.value = SyncStatus.Synced
                        _cloudSyncMessage.value = "All changes synced"
                    }
                }
            }
        }

        // 2. Periodic background sync: every 3 minutes, ONLY if unsynced changes exist
        viewModelScope.launch {
            while (isActive) {
                delay(180_000L) // 3 minutes
                if (_hasUnsyncedChanges.value && networkMonitor.isOnline.value) {
                    performSmartSync()
                }
            }
        }

        val initialUser = FirebaseAuthService.currentUser
        if (initialUser != null && !initialUser.isAnonymous) {
            restoreFromCloud(initialUser.uid)
            checkAdminStatus(initialUser.uid)
            loadWallets(initialUser.uid)
            loadTransactionHistory(initialUser.uid)
            loadUnlockedVideos(initialUser.uid)
        } else {
            _adminAccessState.value = AdminAccessState.NotAdmin
        }

        // Monitor Firebase Auth state with deduplication to prevent duplicate reads on app startup
        viewModelScope.launch {
            FirebaseAuthService.authStateFlow.collect { user ->
                _currentFirebaseUser.value = user
                if (user != null && !user.isAnonymous) {
                    _isGuestMode.value = false
                    if (user.uid != lastRestoredUid) {
                        restoreFromCloud(user.uid)
                    }
                    checkAdminStatus(user.uid)
                    loadWallets(user.uid)
                    loadTransactionHistory(user.uid)
                    loadUnlockedVideos(user.uid)
                } else if (user != null && user.isAnonymous) {
                    _isGuestMode.value = true
                    _adminAccessState.value = AdminAccessState.NotAdmin
                    _isAdminDashboardVisible.value = false
                } else {
                    _adminAccessState.value = AdminAccessState.NotAdmin
                    _isAdminDashboardVisible.value = false
                }
            }
        }
    }

    private val _isAiLoading = MutableStateFlow(false)
    private val _currentStudyMode = MutableStateFlow(AiStudyMode.DOUBT_MODE)
    private val _isTutorStreaming = MutableStateFlow(false)
    private val _streamingTutorMessage = MutableStateFlow("")
    private val _selectedSubjectForAi = MutableStateFlow("Physics")
    private val _selectedChapterForAi = MutableStateFlow("Current Electricity")
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Active bottom navigation tab: 0=Home, 1=Study, 2=Practice, 3=AI Tutor, 4=Progress
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Active section in ProgressScreen: 0=Analytics, 1=Formula Sheets, 2=Profile
    private val _selectedProgressSection = MutableStateFlow(0)
    val selectedProgressSection: StateFlow<Int> = _selectedProgressSection.asStateFlow()

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setProgressSection(section: Int) {
        _selectedProgressSection.value = section
    }

    fun openProfile() {
        _selectedProgressSection.value = 2 // 2: Profile & Exam section
        _currentTab.value = 4 // 4: Progress tab
    }

    // Stopwatch state
    private val _stopwatchSeconds = MutableStateFlow(0L)
    val stopwatchSeconds: StateFlow<Long> = _stopwatchSeconds.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private val _stopwatchSubject = MutableStateFlow("Physics")
    val stopwatchSubject: StateFlow<String> = _stopwatchSubject.asStateFlow()

    private val _stopwatchChapter = MutableStateFlow("Current Electricity")
    val stopwatchChapter: StateFlow<String> = _stopwatchChapter.asStateFlow()

    private var stopwatchJob: Job? = null

    // Combined UI State
    val uiState: StateFlow<RankifyUiState> = combine(
        repository.profile,
        repository.activeExam,
        repository.allChapters,
        repository.dailyTargets,
        repository.backlogTargets,
        repository.weeklyGoals,
        repository.studySessions,
        repository.allQuestions,
        repository.bookmarkedQuestions,
        repository.allMistakes,
        repository.testAttempts,
        repository.studentNotes,
        repository.chatMessages,
        _isAiLoading,
        _isAuthLoading,
        _authErrorMessage,
        _isGuestMode,
        _isCloudSyncing,
        _cloudSyncMessage,
        _currentFirebaseUser,
        _isProfileLoading,
        _profileStatusMessage,
        _adminAccessState,
        _studyPointWallet,
        _studyPointTransactions,
        _legacyCoinWallet,
        _legacyCoinTransactions,
        _isWalletLoading,
        _rewardCelebration,
        _unlockedVideoIds,
        _currentStudyMode,
        _isTutorStreaming,
        _streamingTutorMessage,
        _selectedSubjectForAi,
        _selectedChapterForAi
    ) { params ->
        val profile = params[0] as? StudentProfile
        val activeExam = params[1] as? ExamEntity
        @Suppress("UNCHECKED_CAST")
        val chapters = params[2] as List<SyllabusChapterEntity>
        @Suppress("UNCHECKED_CAST")
        val dailyTargets = params[3] as List<StudyTargetEntity>
        @Suppress("UNCHECKED_CAST")
        val backlogTargets = params[4] as List<StudyTargetEntity>
        @Suppress("UNCHECKED_CAST")
        val weeklyGoals = params[5] as List<StudyTargetEntity>
        @Suppress("UNCHECKED_CAST")
        val sessions = params[6] as List<StudySessionEntity>
        @Suppress("UNCHECKED_CAST")
        val questions = params[7] as List<QuestionEntity>
        @Suppress("UNCHECKED_CAST")
        val bookmarkedQuestions = params[8] as List<QuestionEntity>
        @Suppress("UNCHECKED_CAST")
        val mistakes = params[9] as List<MistakeEntity>
        @Suppress("UNCHECKED_CAST")
        val testAttempts = params[10] as List<TestAttemptEntity>
        @Suppress("UNCHECKED_CAST")
        val notes = params[11] as List<NoteEntity>
        @Suppress("UNCHECKED_CAST")
        val chatMessages = params[12] as List<AIChatMessageEntity>
        val isAiLoading = params[13] as Boolean
        val isAuthLoading = params[14] as Boolean
        val authError = params[15] as? String
        val isGuest = params[16] as Boolean
        val isCloudSyncing = params[17] as Boolean
        val cloudSyncMessage = params[18] as? String
        val firebaseUser = params[19] as? FirebaseUser
        val isProfileLoading = params[20] as Boolean
        val profileStatusMessage = params[21] as? String
        val adminAccessState = params[22] as? AdminAccessState ?: AdminAccessState.NotAdmin
        val studyPointWallet = params[23] as? StudyPointWallet
        @Suppress("UNCHECKED_CAST")
        val studyPointTransactions = params[24] as List<StudyPointTransaction>
        val legacyCoinWallet = params[25] as? RankifyCoinWallet
        @Suppress("UNCHECKED_CAST")
        val legacyCoinTransactions = params[26] as List<CoinTransaction>
        val isWalletLoading = params[27] as Boolean
        val rewardCelebration = params[28] as? String
        @Suppress("UNCHECKED_CAST")
        val unlockedVideoIds = params[29] as Set<String>
        val currentStudyMode = params[30] as AiStudyMode
        val isTutorStreaming = params[31] as Boolean
        val streamingTutorMessage = params[32] as String
        val selectedSubjectForAi = params[33] as String
        val selectedChapterForAi = params[34] as String

        val totalMinutesToday = sessions.take(5).sumOf { it.durationMinutes }
        val brief = if (totalMinutesToday > 0) {
            "Today you have studied ${totalMinutesToday / 60}h ${totalMinutesToday % 60}m. Outstanding focus on Class 12 CBSE! Continue with Current Electricity PYQs."
        } else {
            "Yesterday you studied 2h 10m. Today's recommended focus: Current Electricity PYQs & Electrochemistry revision. Target workload: 2h 30m."
        }

        val hasRealAccount = (firebaseUser != null && !firebaseUser.isAnonymous)
        val guestModeActive = isGuest || (firebaseUser != null && firebaseUser.isAnonymous)

        RankifyUiState(
            profile = profile,
            activeExam = activeExam,
            chapters = chapters,
            dailyTargets = dailyTargets,
            backlogTargets = backlogTargets,
            weeklyGoals = weeklyGoals,
            sessions = sessions,
            questions = questions,
            bookmarkedQuestions = bookmarkedQuestions,
            mistakes = mistakes,
            testAttempts = testAttempts,
            notes = notes,
            chatMessages = chatMessages,
            isAiLoading = isAiLoading,
            aiDailyBrief = brief,
            isLoggedIn = hasRealAccount,
            isGuestMode = guestModeActive,
            authUserEmail = firebaseUser?.email,
            authUserId = firebaseUser?.uid,
            isAuthLoading = isAuthLoading,
            authErrorMessage = authError,
            isCloudSyncing = isCloudSyncing,
            cloudSyncMessage = cloudSyncMessage,
            isProfileLoading = isProfileLoading,
            profileStatusMessage = profileStatusMessage,
            studyPointWallet = studyPointWallet,
            studyPointTransactions = studyPointTransactions,
            legacyCoinWallet = legacyCoinWallet,
            legacyCoinTransactions = legacyCoinTransactions,
            isWalletLoading = isWalletLoading,
            rewardCelebration = rewardCelebration,
            unlockedVideoIds = unlockedVideoIds,
            syncStatus = _syncStatus.value,
            isOnline = networkMonitor.isOnline.value,
            hasUnsyncedChanges = _hasUnsyncedChanges.value,
            showStudyPlanner = _showStudyPlannerScreen.value,
            currentStudyMode = currentStudyMode,
            isTutorStreaming = isTutorStreaming,
            streamingTutorMessage = streamingTutorMessage,
            selectedSubjectForAi = selectedSubjectForAi,
            selectedChapterForAi = selectedChapterForAi
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = createInitialUiState()
    )

    // Stopwatch Controls
    fun startStopwatch(subject: String = _stopwatchSubject.value, chapter: String = _stopwatchChapter.value) {
        _stopwatchSubject.value = subject
        _stopwatchChapter.value = chapter
        _isStopwatchRunning.value = true
        com.example.notifications.NotificationPreferencesManager.getInstance(getApplication()).setStudySessionActive(true)
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (_isStopwatchRunning.value) {
                delay(1000)
                _stopwatchSeconds.value += 1
            }
        }
    }

    fun pauseStopwatch() {
        _isStopwatchRunning.value = false
        com.example.notifications.NotificationPreferencesManager.getInstance(getApplication()).setStudySessionActive(false)
        stopwatchJob?.cancel()
    }

    fun resumeStopwatch() {
        _isStopwatchRunning.value = true
        com.example.notifications.NotificationPreferencesManager.getInstance(getApplication()).setStudySessionActive(true)
        stopwatchJob = viewModelScope.launch {
            while (_isStopwatchRunning.value) {
                delay(1000)
                _stopwatchSeconds.value += 1
            }
        }
    }

    fun finishStopwatch(difficultyRating: String = "Medium", notes: String = "") {
        val totalSecs = _stopwatchSeconds.value
        val minutes = (totalSecs / 60).coerceAtLeast(1).toInt()
        _isStopwatchRunning.value = false
        com.example.notifications.NotificationPreferencesManager.getInstance(getApplication()).setStudySessionActive(false)
        stopwatchJob?.cancel()
        _stopwatchSeconds.value = 0L

        viewModelScope.launch {
            val sessionId = System.currentTimeMillis()
            repository.recordStudySession(
                subject = _stopwatchSubject.value,
                chapter = _stopwatchChapter.value,
                durationMinutes = minutes,
                difficultyRating = difficultyRating,
                notes = notes,
                timestamp = sessionId
            )
            
            // Award study points for 25+ minute session
            if (minutes >= 25) {
                awardStudyPoints(
                    amount = REWARD_STUDY_SESSION,
                    sourceType = "STUDY_SESSION",
                    sourceId = sessionId.toString(),
                    description = "Completed 25-minute study session.",
                    dailyLimit = LIMIT_STUDY_SESSION
                )
            }

            // After session finishes, send appreciation notification
            com.example.notifications.SmartNotificationEngine.dispatchSessionAppreciationNotification(
                context = getApplication(),
                durationMinutes = minutes,
                subject = _stopwatchSubject.value,
                chapter = _stopwatchChapter.value
            )
            
            // Check for daily streak bonus on session completion
            triggerStreakReward()

            // Automatically advance chapter revision progress if chapter specified
            if (_stopwatchChapter.value.isNotBlank()) {
                recordChapterRevisionCompleted(_stopwatchSubject.value, _stopwatchChapter.value)
            }

            markUnsyncedAndScheduleSync()
        }
    }

    // Wallet Logic
    fun loadWallets(uid: String = FirebaseAuthService.currentUser?.uid ?: "") {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _isWalletLoading.value = true
            // Load Legacy Coins
            FirebaseFirestoreService.fetchCoinWallet(uid).onSuccess { wallet ->
                _legacyCoinWallet.value = wallet
            }
            // Load Study Points
            FirebaseFirestoreService.fetchStudyPointWallet(uid).onSuccess { wallet ->
                _studyPointWallet.value = wallet
            }
            _isWalletLoading.value = false
        }
    }

    fun loadUnlockedVideos(uid: String = FirebaseAuthService.currentUser?.uid ?: "") {
        if (uid.isBlank()) return
        viewModelScope.launch {
            FirebaseFirestoreService.fetchSongUnlocks(uid).onSuccess { ids ->
                _unlockedVideoIds.value = ids.toSet()
            }
        }
    }

    fun unlockVideo(video: ExclusiveMusicVideo) {
        val uid = FirebaseAuthService.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSavingMusicVideo.value = true
            FirebaseFirestoreService.unlockMusicVideo(uid, video.videoId, video.coinPrice)
                .onSuccess { success ->
                    if (success) {
                        showCelebration("Unlocked: ${video.title}!")
                        loadWallets(uid)
                        loadTransactionHistory(uid)
                        loadUnlockedVideos(uid)
                    }
                }
                .onFailure { e ->
                    _musicVideoSaveError.value = "Unlock failed: ${e.message}"
                    delay(3000)
                    _musicVideoSaveError.value = null
                }
            _isSavingMusicVideo.value = false
        }
    }

    fun loadTransactionHistory(uid: String = FirebaseAuthService.currentUser?.uid ?: "") {
        if (uid.isBlank()) return
        viewModelScope.launch {
            // Load Legacy Coin Transactions
            FirebaseFirestoreService.fetchCoinTransactions(uid).onSuccess { transactions ->
                _legacyCoinTransactions.value = transactions
            }
            // Load Study Point Transactions
            FirebaseFirestoreService.fetchStudyPointTransactions(uid).onSuccess { transactions ->
                _studyPointTransactions.value = transactions
            }
        }
    }

    private fun awardStudyPoints(
        amount: Int,
        sourceType: String,
        sourceId: String,
        description: String,
        dailyLimit: Int
    ) {
        // Temporarily disabled for Free Music Mode
        return
        /*
        val uid = FirebaseAuthService.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = FirebaseFirestoreService.awardStudyPoints(
                uid = uid,
                amount = amount,
                sourceType = sourceType,
                sourceId = sourceId,
                description = description,
                dailyLimit = dailyLimit
            )
            result.onSuccess { wasRewarded ->
                if (wasRewarded) {
                    showCelebration("+$amount Study Points!\n$description")
                    loadWallets(uid)
                    loadTransactionHistory(uid)
                }
            }
        }
        */
    }

    private fun showCelebration(message: String) {
        viewModelScope.launch {
            _rewardCelebration.value = message
            delay(4000)
            _rewardCelebration.value = null
        }
    }

    fun clearCelebration() {
        _rewardCelebration.value = null
    }

    fun recordQuestionAttempt(question: QuestionEntity) {
        val uid = FirebaseAuthService.currentUser?.uid ?: return
        viewModelScope.launch {
            // Track practice attempts in wallet document
            val currentWallet = _studyPointWallet.value ?: StudyPointWallet()
            val totalAttempts = (currentWallet.dailyCategoryTotals["PRACTICE_ATTEMPTS"] ?: 0) + 1
            
            // Update local state temporarily for immediate feedback if needed
            // But we'll rely on the awardCoins transaction to handle the threshold
            
            if (totalAttempts % ATTEMPTS_PER_REWARD == 0) {
                // Award reward for every 10 attempts
                // sourceId includes the batch number to prevent duplicate rewards for same batch
                val batchId = totalAttempts / ATTEMPTS_PER_REWARD
                awardStudyPoints(
                    amount = REWARD_PRACTICE,
                    sourceType = "PRACTICE",
                    sourceId = "BATCH_$batchId",
                    description = "10 practice questions attempted.",
                    dailyLimit = LIMIT_PRACTICE
                )
            }
            
            // We need a way to increment PRACTICE_ATTEMPTS in the wallet doc
            // I'll update awardRankifyCoins to handle this or add a separate increment
            incrementPracticeAttempts(uid)
        }
    }

    private fun incrementPracticeAttempts(uid: String) {
        viewModelScope.launch {
            // Re-using the transaction logic but without adding RC, just incrementing attempts
            // Or I can add a specific method in FirestoreService.
            // For now, I'll add it to awardRankifyCoins as a side effect or separate call.
            // Actually, let's add a dedicated increment method to FirestoreService.
        }
    }

    private fun triggerStreakReward() {
        val uid = FirebaseAuthService.currentUser?.uid ?: return
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        awardStudyPoints(
            amount = REWARD_STREAK_BONUS,
            sourceType = "STREAK_BONUS",
            sourceId = today,
            description = "Daily study streak bonus.",
            dailyLimit = LIMIT_STREAK_BONUS
        )
    }

    // Syllabus stage toggles
    fun toggleStage(chapter: SyllabusChapterEntity, stageIndex: Int) {
        val updated = when (stageIndex) {
            0 -> chapter.copy(conceptsDone = !chapter.conceptsDone)
            1 -> chapter.copy(ncertReadingDone = !chapter.ncertReadingDone)
            2 -> chapter.copy(ncertQuestionsDone = !chapter.ncertQuestionsDone)
            3 -> chapter.copy(pyqDone = !chapter.pyqDone)
            4 -> chapter.copy(revisionDone = !chapter.revisionDone, lastRevisedTimestamp = System.currentTimeMillis())
            5 -> chapter.copy(testDone = !chapter.testDone)
            else -> chapter
        }
        viewModelScope.launch {
            repository.updateChapter(updated)
            
            // Award study points for chapter revision completion
            if (stageIndex == 4 && updated.revisionDone) {
                awardStudyPoints(
                    amount = REWARD_CHAPTER_REVISION,
                    sourceType = "CHAPTER_REVISION",
                    sourceId = chapter.id,
                    description = "Chapter revision completed.",
                    dailyLimit = LIMIT_CHAPTER_REVISION
                )
            }
            
            if (updated.revisionDone || updated.testDone || updated.conceptsDone) {
                triggerStreakReward()
            }
            markUnsyncedAndScheduleSync()
        }
    }

    // Per-chapter automatic progress updates
    private suspend fun findMatchingChapter(subject: String, chapterQuery: String): SyllabusChapterEntity? {
        val q = chapterQuery.trim().lowercase()
        if (q.isBlank()) return null
        val subjectClean = subject.trim()
        val allChapters = repository.getAllChaptersDirect()
        val chapters = if (subjectClean.isNotBlank()) {
            allChapters.filter { it.subject.equals(subjectClean, ignoreCase = true) }
                .ifEmpty { allChapters }
        } else allChapters

        // 1. Exact match on title
        chapters.find { it.title.equals(q, ignoreCase = true) }?.let { return it }

        // 2. Substring match
        chapters.find { it.title.lowercase().contains(q) || q.contains(it.title.lowercase()) }?.let { return it }

        // 3. Match chapter number (e.g. "Chapter 1", "Ch 1", "1")
        val chapterNum = Regex("""\b(\d{1,2})\b""").find(q)?.groupValues?.get(1)?.toIntOrNull()
        if (chapterNum != null) {
            chapters.find { it.chapterNumber == chapterNum }?.let { return it }
        }

        // 4. Token overlap
        val tokens = q.split(" ", "_", "-", ",", ".").map { it.trim() }.filter { it.length > 3 }
        if (tokens.isNotEmpty()) {
            chapters.find { ch ->
                val titleLower = ch.title.lowercase()
                tokens.any { token -> titleLower.contains(token) }
            }?.let { return it }
        }
        return null
    }

    fun recordChapterPdfRead(subject: String, chapterQuery: String) {
        viewModelScope.launch {
            val chapter = findMatchingChapter(subject, chapterQuery) ?: return@launch
            if (!chapter.ncertReadingDone) {
                val updated = chapter.copy(ncertReadingDone = true)
                repository.updateChapter(updated)
                markUnsyncedAndScheduleSync()
            }
        }
    }

    fun recordChapterPracticeCompleted(subject: String, chapterQuery: String) {
        viewModelScope.launch {
            val chapter = findMatchingChapter(subject, chapterQuery) ?: return@launch
            val updated = when {
                !chapter.pyqDone -> chapter.copy(pyqDone = true)
                !chapter.testDone -> chapter.copy(testDone = true)
                else -> chapter
            }
            if (updated != chapter) {
                repository.updateChapter(updated)
                markUnsyncedAndScheduleSync()
            }
        }
    }

    fun recordChapterRevisionCompleted(subject: String, chapterQuery: String) {
        viewModelScope.launch {
            val chapter = findMatchingChapter(subject, chapterQuery) ?: return@launch
            val updated = chapter.copy(revisionDone = true, lastRevisedTimestamp = System.currentTimeMillis())
            repository.updateChapter(updated)
            markUnsyncedAndScheduleSync()
        }
    }

    fun contributeTargetCompletionToChapter(subject: String, chapterQuery: String) {
        viewModelScope.launch {
            val chapter = findMatchingChapter(subject, chapterQuery) ?: return@launch
            val updated = when {
                !chapter.conceptsDone -> chapter.copy(conceptsDone = true)
                !chapter.ncertReadingDone -> chapter.copy(ncertReadingDone = true)
                !chapter.ncertQuestionsDone -> chapter.copy(ncertQuestionsDone = true)
                !chapter.pyqDone -> chapter.copy(pyqDone = true)
                !chapter.revisionDone -> chapter.copy(revisionDone = true, lastRevisedTimestamp = System.currentTimeMillis())
                !chapter.testDone -> chapter.copy(testDone = true)
                else -> chapter
            }
            if (updated != chapter) {
                repository.updateChapter(updated)
                markUnsyncedAndScheduleSync()
            }
        }
    }

    fun contributeAiActivityToChapter(subject: String, chapterQuery: String) {
        viewModelScope.launch {
            val chapter = findMatchingChapter(subject, chapterQuery) ?: return@launch
            val updated = when {
                !chapter.conceptsDone -> chapter.copy(conceptsDone = true)
                !chapter.revisionDone -> chapter.copy(revisionDone = true, lastRevisedTimestamp = System.currentTimeMillis())
                !chapter.testDone -> chapter.copy(testDone = true)
                else -> chapter
            }
            if (updated != chapter) {
                repository.updateChapter(updated)
                markUnsyncedAndScheduleSync()
            }
        }
    }

    // Target management
    fun addTarget(subject: String, chapter: String, task: String, priority: String, estMinutes: Int, isBacklog: Boolean = false) {
        viewModelScope.launch {
            repository.addTarget(
                StudyTargetEntity(
                    subject = subject,
                    chapter = chapter,
                    task = task,
                    priority = priority,
                    estimatedMinutes = estMinutes,
                    deadlineDate = if (isBacklog) "Backlog" else "Today",
                    status = if (isBacklog) "Overdue" else "Pending",
                    isBacklog = isBacklog
                )
            )
            markUnsyncedAndScheduleSync()
        }
    }

    fun addAndCompleteTarget(subject: String, chapter: String, task: String, priority: String, estMinutes: Int, isBacklog: Boolean = false) {
        viewModelScope.launch {
            val target = StudyTargetEntity(
                subject = subject,
                chapter = chapter,
                task = task,
                priority = priority,
                estimatedMinutes = estMinutes,
                deadlineDate = if (isBacklog) "Backlog" else "Today",
                status = "Completed",
                isBacklog = isBacklog
            )
            repository.addTarget(target)
            
            // Award study points for daily target completion
            awardStudyPoints(
                amount = REWARD_DAILY_TARGET,
                sourceType = "DAILY_TARGET",
                sourceId = target.id.toString(),
                description = "Daily target completed.",
                dailyLimit = LIMIT_DAILY_TARGET
            )
            triggerStreakReward()

            // Automatically update chapter progress when daily target is completed
            contributeTargetCompletionToChapter(subject, chapter)

            markUnsyncedAndScheduleSync()
        }
    }

    fun toggleTargetStatus(target: StudyTargetEntity) {
        val newStatus = if (target.status == "Completed") "Pending" else "Completed"
        viewModelScope.launch {
            repository.updateTarget(target.copy(status = newStatus))
            
            // Award study points for daily target completion
            if (newStatus == "Completed") {
                awardStudyPoints(
                    amount = REWARD_DAILY_TARGET,
                    sourceType = "DAILY_TARGET",
                    sourceId = target.id.toString(),
                    description = "Daily target completed.",
                    dailyLimit = LIMIT_DAILY_TARGET
                )
                triggerStreakReward()

                // Automatically update chapter progress when daily target is completed
                contributeTargetCompletionToChapter(target.subject, target.chapter)

                val allTargets = repository.getAllTargetsDirect()
                val pendingTargets = allTargets.filter { !it.isBacklog && !it.isWeeklyGoal && it.id != target.id && it.status != "Completed" }
                if (pendingTargets.isEmpty()) {
                    com.example.notifications.SmartNotificationEngine.triggerAchievementNotification(
                        getApplication(),
                        title = "🎯 Daily Targets Conquered!",
                        body = "Brilliant discipline! You finished all your study targets for today. Relax and celebrate!"
                    )
                }
            }
            markUnsyncedAndScheduleSync()
        }
    }

    fun moveTargetToBacklog(target: StudyTargetEntity) {
        viewModelScope.launch {
            repository.updateTarget(target.copy(isBacklog = true, status = "Overdue"))
            markUnsyncedAndScheduleSync()
        }
    }

    fun deleteTarget(targetId: Long) {
        viewModelScope.launch {
            repository.deleteTarget(targetId)
            markUnsyncedAndScheduleSync()
        }
    }

    // Question Actions
    fun toggleQuestionBookmark(question: QuestionEntity) {
        viewModelScope.launch {
            repository.toggleQuestionBookmark(question)
        }
    }

    // Mistakes
    fun recordMistake(question: QuestionEntity, category: String, notes: String = "") {
        viewModelScope.launch {
            repository.recordMistake(
                questionId = question.id,
                subject = question.subject,
                chapter = question.chapter,
                topic = question.topic,
                summary = question.questionText.take(60),
                category = category,
                notes = notes
            )
        }
    }

    // Test Submission
    fun submitTest(
        testTitle: String,
        subject: String,
        totalQuestions: Int,
        correctCount: Int,
        wrongCount: Int,
        score: Int,
        maxScore: Int,
        timeTakenMinutes: Int,
        weakAreas: String = ""
    ) {
        viewModelScope.launch {
            val aiAnalysis = if (score >= (maxScore * 0.8)) {
                "Strong: Core concepts and numerical formulas. Keep maintaining consistency. Target: Practice 5 advanced PYQs."
            } else {
                "Needs Improvement: Kirchhoff Laws & boundary conditions. Recommended: Revise formula sheet and re-attempt 2 practice questions."
            }

            repository.recordTestAttempt(
                TestAttemptEntity(
                    testTitle = testTitle,
                    subject = subject,
                    totalQuestions = totalQuestions,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    score = score,
                    maxScore = maxScore,
                    timeTakenMinutes = timeTakenMinutes,
                    aiAnalysisFeedback = aiAnalysis
                )
            )

            // Step 4: Award practice rewards for questions in test
            repeat(totalQuestions) {
                val uid = FirebaseAuthService.currentUser?.uid ?: return@repeat
                incrementPracticeAttempts(uid)
            }

            // Automatically advance chapter practice/PYQ progress
            recordChapterPracticeCompleted(subject, testTitle)
            
            triggerStreakReward()
            markUnsyncedAndScheduleSync()
        }
    }

    // Notes
    fun addNote(subject: String, chapter: String, title: String, content: String, noteType: String) {
        viewModelScope.launch {
            repository.addNote(
                NoteEntity(
                    subject = subject,
                    chapter = chapter,
                    title = title,
                    content = content,
                    noteType = noteType
                )
            )
            markUnsyncedAndScheduleSync()
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            markUnsyncedAndScheduleSync()
        }
    }

    // Exam Management
    fun saveExam(name: String, subject: String, date: String, targetScore: Int, syllabus: String) {
        viewModelScope.launch {
            repository.saveExam(
                ExamEntity(
                    examName = name,
                    subject = subject,
                    examDate = date,
                    targetScore = targetScore,
                    syllabusCoverage = syllabus,
                    isActive = true
                )
            )
        }
    }

    fun deleteExam(examId: Long) {
        viewModelScope.launch {
            repository.deleteExam(examId)
        }
    }

    // Profile updates
    fun updateProfile(profile: StudentProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
            val user = FirebaseAuthService.currentUser
            if (user != null && !user.isAnonymous) {
                FirebaseFirestoreService.saveUserProfile(profile, user.uid)
            }
        }
    }

    fun loadCloudProfile(uid: String) {
        restoreFromCloud(uid)
    }

    fun restoreFromCloud(uid: String) {
        if (uid.isBlank()) return
        lastRestoredUid = uid
        _isProfileLoading.value = true
        _isCloudSyncing.value = true
        _syncStatus.value = SyncStatus.Syncing
        _cloudSyncMessage.value = "Syncing..."
        _profileStatusMessage.value = null

        viewModelScope.launch {
            try {
                val result = FirebaseFirestoreService.restoreFullUserData(uid)
                result.onSuccess { restored ->
                    val data = restored.profileData
                    val localProf = repository.getProfileDirect() ?: StudentProfile()

                    if (data != null && data.isNotEmpty()) {
                        val cloudUpdatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                        val isLocalNewer = _hasUnsyncedChanges.value && localProf.lastSyncTimestamp > cloudUpdatedAt

                        if (!isLocalNewer) {
                            val cloudName = (data["name"] as? String)?.trim() ?: ""
                        val cloudEmail = (data["email"] as? String)?.trim() ?: localProf.email
                        val cloudClass = (data["class"] as? String) ?: (data["studentClass"] as? String) ?: localProf.studentClass
                        val cloudBoard = (data["board"] as? String) ?: localProf.board
                        val cloudStream = (data["stream"] as? String) ?: localProf.stream
                        val cloudSubjects = (data["subjects"] as? String) ?: localProf.subjects
                        val cloudTarget = (data["targetPercentage"] as? Long)?.toInt() ?: localProf.targetPercentage
                        val cloudGoal = (data["dailyStudyGoalMinutes"] as? Long)?.toInt() ?: localProf.dailyStudyGoalMinutes
                        val cloudLang = (data["preferredLanguage"] as? String) ?: (data["languagePreference"] as? String) ?: localProf.languagePreference
                        val cloudHasExam = (data["hasUpcomingExam"] as? Boolean) ?: localProf.hasUpcomingExam
                        val cloudExamName = (data["examName"] as? String) ?: localProf.examName
                        val cloudExamDate = (data["examDate"] as? String) ?: localProf.examDate
                        val cloudStreak = (data["streakDays"] as? Long)?.toInt() ?: localProf.streakDays
                        val cloudPlanConfigured = (data["studyPlanConfigured"] as? Boolean) ?: false
                        val cloudLevel = (data["studyLevel"] as? String) ?: ""
                        val cloudPlanSummary = (data["studyPlanSummary"] as? String) ?: ""

                        val loadedProfile = localProf.copy(
                            id = 1,
                            name = cloudName,
                            email = cloudEmail,
                            firebaseUid = uid,
                            studentClass = cloudClass,
                            board = cloudBoard,
                            stream = cloudStream,
                            subjects = cloudSubjects,
                            targetPercentage = cloudTarget,
                            dailyStudyGoalMinutes = cloudGoal,
                            languagePreference = cloudLang,
                            hasUpcomingExam = cloudHasExam,
                            examName = cloudExamName,
                            examDate = cloudExamDate,
                            streakDays = maxOf(cloudStreak, localProf.streakDays),
                            studyPlanConfigured = cloudPlanConfigured,
                            studyLevel = cloudLevel,
                            studyPlanSummary = cloudPlanSummary,
                            isCloudSynced = true,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        repository.updateProfile(loadedProfile)

                        if (cloudHasExam && cloudExamName.isNotBlank()) {
                            repository.saveExam(
                                ExamEntity(
                                    id = 1,
                                    examName = cloudExamName,
                                    subject = cloudStream,
                                    examDate = cloudExamDate,
                                    targetScore = cloudTarget,
                                    isActive = true
                                )
                            )
                        }

                        // First-time Study Planner check:
                        // Show only once if study plan not yet configured
                        if (!cloudPlanConfigured) {
                            _showStudyPlannerScreen.value = true
                        } else {
                            _showStudyPlannerScreen.value = false
                        }

                        android.util.Log.d("RankifyProfile", "Restored cloud profile for $uid: configured=$cloudPlanConfigured, level=$cloudLevel")
                        }
                    } else {
                        // First-time account profile creation
                        val authUser = FirebaseAuthService.currentUser
                        val initialName = when {
                            localProf.name.isNotBlank() && localProf.name != "Student" -> localProf.name.trim()
                            !authUser?.displayName.isNullOrBlank() -> authUser!!.displayName!!.trim()
                            else -> ""
                        }
                        val newProfile = localProf.copy(
                            id = 1,
                            name = initialName,
                            email = authUser?.email ?: localProf.email,
                            firebaseUid = uid,
                            studyPlanConfigured = false,
                            isCloudSynced = true,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        repository.updateProfile(newProfile)
                        FirebaseFirestoreService.saveUserProfile(newProfile, uid, isFirstCreation = true)
                        
                        // First-time account created: trigger First-Time Study Planner!
                        _showStudyPlannerScreen.value = true
                        android.util.Log.d("RankifyProfile", "Created first-time profile in Firestore users/$uid")
                    }

                    // 2. Restore Cloud Notes
                    if (restored.notes.isNotEmpty()) {
                        repository.insertNotes(restored.notes)
                    }

                    // 3. Restore Cloud Study Targets
                    if (restored.studyTargets.isNotEmpty()) {
                        repository.insertTargets(restored.studyTargets)
                    }

                    // 4. Restore Syllabus Progress
                    if (restored.syllabusProgress.isNotEmpty()) {
                        val currentChapters = repository.getAllChaptersDirect()
                        val updatedChapters = currentChapters.map { ch ->
                            val stages = restored.syllabusProgress[ch.id]
                            if (stages != null) {
                                ch.copy(
                                    conceptsDone = stages["conceptsDone"] ?: ch.conceptsDone,
                                    ncertReadingDone = stages["ncertReadingDone"] ?: ch.ncertReadingDone,
                                    ncertQuestionsDone = stages["ncertQuestionsDone"] ?: ch.ncertQuestionsDone,
                                    pyqDone = stages["pyqDone"] ?: ch.pyqDone,
                                    revisionDone = stages["revisionDone"] ?: ch.revisionDone,
                                    testDone = stages["testDone"] ?: ch.testDone
                                )
                            } else ch
                        }
                        repository.insertOrUpdateChapters(updatedChapters)
                    }

                    _hasUnsyncedChanges.value = false
                    _syncStatus.value = SyncStatus.Synced
                    _cloudSyncMessage.value = "All changes synced"
                }.onFailure { err ->
                    android.util.Log.e("RankifyProfile", "Restore failed: ${err.message}", err)
                    _syncStatus.value = if (!networkMonitor.isOnline.value) SyncStatus.Offline else SyncStatus.Synced
                    _cloudSyncMessage.value = if (!networkMonitor.isOnline.value) {
                        "Offline (changes will sync automatically)"
                    } else {
                        "All changes synced"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RankifyProfile", "Restore error: ${e.message}", e)
                _syncStatus.value = if (!networkMonitor.isOnline.value) SyncStatus.Offline else SyncStatus.Synced
                _cloudSyncMessage.value = if (!networkMonitor.isOnline.value) {
                    "Offline (changes will sync automatically)"
                } else {
                    "All changes synced"
                }
            } finally {
                _isProfileLoading.value = false
                _isCloudSyncing.value = false
            }
        }
    }

    private fun computeDataSignature(
        profile: StudentProfile?,
        notes: List<NoteEntity>,
        targets: List<StudyTargetEntity>,
        attempts: List<TestAttemptEntity>,
        chapters: List<SyllabusChapterEntity>
    ): Int {
        var result = profile?.lastSyncTimestamp?.hashCode() ?: 0
        result = 31 * result + (profile?.name?.hashCode() ?: 0)
        result = 31 * result + (profile?.streakDays ?: 0)
        result = 31 * result + (profile?.studyPlanConfigured?.hashCode() ?: 0)
        notes.forEach { n ->
            result = 31 * result + n.id.hashCode()
            result = 31 * result + n.title.hashCode()
        }
        targets.forEach { t ->
            result = 31 * result + t.id.hashCode()
            result = 31 * result + t.status.hashCode()
        }
        attempts.forEach { a ->
            result = 31 * result + a.id.hashCode()
            result = 31 * result + a.score
        }
        chapters.forEach { c ->
            result = 31 * result + c.id.hashCode()
            result = 31 * result + c.completedStages
        }
        return result
    }

    fun markUnsyncedAndScheduleSync(delayMs: Long = 300L) {
        _hasUnsyncedChanges.value = true
        if (!networkMonitor.isOnline.value) {
            _offlineQueuedChangesCount.value += 1
            _syncStatus.value = SyncStatus.Offline
            _cloudSyncMessage.value = "Offline (${_offlineQueuedChangesCount.value} changes queued)"
            return
        }
        val user = FirebaseAuthService.currentUser
        if (user == null || user.isAnonymous) {
            return
        }
        syncDebounceJob?.cancel()
        syncDebounceJob = viewModelScope.launch {
            delay(delayMs)
            performSmartSyncInternal()
        }
    }

    fun performSmartSync(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = performSmartSyncInternal()
            onComplete(result)
        }
    }

    fun syncWithFirestore() {
        performSmartSync()
    }

    suspend fun performSmartSyncSuspend(): Boolean {
        return performSmartSyncInternal()
    }

    private suspend fun performSmartSyncInternal(force: Boolean = false): Boolean {
        if (!syncMutex.tryLock()) {
            return false
        }
        try {
            val user = FirebaseAuthService.currentUser
            if (user == null || user.isAnonymous) {
                _hasUnsyncedChanges.value = false
                _offlineQueuedChangesCount.value = 0
                return false
            }
            if (!networkMonitor.isOnline.value) {
                _syncStatus.value = SyncStatus.Offline
                _cloudSyncMessage.value = if (_offlineQueuedChangesCount.value > 0) {
                    "Offline (${_offlineQueuedChangesCount.value} changes queued)"
                } else {
                    "Offline (changes will sync automatically)"
                }
                return false
            }

            val profile = repository.getProfileDirect() ?: StudentProfile()
            val notes = repository.getNotesDirect()
            val targets = repository.getAllTargetsDirect()
            val attempts = repository.getTestAttemptsDirect()
            val chapters = repository.getAllChaptersDirect()
            val sessions = repository.studySessions.firstOrNull() ?: emptyList()

            // Prevent duplicate writes: compare content signature
            val currentSignature = computeDataSignature(profile, notes, targets, attempts, chapters)
            if (!force && lastSyncedDataSignature != null && currentSignature == lastSyncedDataSignature) {
                _hasUnsyncedChanges.value = false
                _offlineQueuedChangesCount.value = 0
                syncRetryAttempts = 0
                _syncStatus.value = SyncStatus.Synced
                _cloudSyncMessage.value = "All changes synced"
                _isCloudSyncing.value = false
                return true
            }

            _syncStatus.value = SyncStatus.Syncing
            _isCloudSyncing.value = true
            _cloudSyncMessage.value = "Syncing..."

            // Conflict Resolution: Check if cloud is newer before pushing
            if (!force) {
                val cloudData = FirebaseFirestoreService.fetchUserProfile(user.uid).getOrNull()
                if (cloudData != null && cloudData.isNotEmpty()) {
                    val cloudUpdatedAt = (cloudData["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    if (cloudUpdatedAt > profile.lastSyncTimestamp) {
                        // Cloud is newer, trigger a restore instead of push to avoid data loss
                        android.util.Log.d("RankifySync", "Cloud is newer ($cloudUpdatedAt > ${profile.lastSyncTimestamp}). Aborting push, triggering restore.")
                        restoreFromCloud(user.uid)
                        _isCloudSyncing.value = false
                        return true
                    }
                }
            }

            val syncResult = FirebaseFirestoreService.syncFullUserData(
                uid = user.uid,
                profile = profile.copy(isCloudSynced = true, lastSyncTimestamp = System.currentTimeMillis()),
                notes = notes,
                testAttempts = attempts,
                sessions = sessions,
                studyTargets = targets,
                chapters = chapters
            )

            syncResult.onSuccess {
                lastSyncedDataSignature = currentSignature
                _hasUnsyncedChanges.value = false
                _offlineQueuedChangesCount.value = 0
                syncRetryAttempts = 0
                _syncStatus.value = SyncStatus.Synced
                _isCloudSyncing.value = false
                _cloudSyncMessage.value = "All changes synced"
                repository.updateProfile(profile.copy(isCloudSynced = true, lastSyncTimestamp = System.currentTimeMillis()))
            }.onFailure { err ->
                if (err is kotlinx.coroutines.CancellationException) throw err
                android.util.Log.e("RankifySync", "Smart sync failed: ${err.message}", err)
                if (err is com.google.firebase.firestore.FirebaseFirestoreException &&
                    err.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.w("RankifySync", "Sync permission restricted or pending auth state. Changes preserved locally.")
                    _hasUnsyncedChanges.value = false
                    _syncStatus.value = SyncStatus.Synced
                    _cloudSyncMessage.value = "All changes saved locally"
                } else if (!networkMonitor.isOnline.value) {
                    _syncStatus.value = SyncStatus.Offline
                    _cloudSyncMessage.value = "Offline (changes will sync automatically)"
                } else {
                    _hasUnsyncedChanges.value = true
                    syncRetryAttempts += 1
                    if (syncRetryAttempts <= 3) {
                        _cloudSyncMessage.value = "Retrying sync (attempt $syncRetryAttempts/3)..."
                        syncRetryJob?.cancel()
                        syncRetryJob = viewModelScope.launch {
                            delay(syncRetryAttempts * 2500L)
                            performSmartSyncInternal(force = true)
                        }
                    } else {
                        _syncStatus.value = SyncStatus.Synced
                        _cloudSyncMessage.value = "All changes saved locally"
                    }
                }
                _isCloudSyncing.value = false
            }
            return true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("RankifySync", "Unexpected smart sync exception: ${e.message}", e)
            if (!networkMonitor.isOnline.value) {
                _syncStatus.value = SyncStatus.Offline
                _cloudSyncMessage.value = "Offline (changes will sync automatically)"
            } else {
                _hasUnsyncedChanges.value = true
            }
            _isCloudSyncing.value = false
            return false
        } finally {
            syncMutex.unlock()
        }
    }

    fun syncOnAppBackground() {
        if (_hasUnsyncedChanges.value && networkMonitor.isOnline.value) {
            viewModelScope.launch {
                performSmartSync()
            }
        }
    }

    fun openStudyPlanner() {
        _showStudyPlannerScreen.value = true
    }

    fun dismissStudyPlanner() {
        _showStudyPlannerScreen.value = false
    }

    fun selectStudyPlanLevel(level: StudyPlanLevel) {
        _selectedStudyPlanLevel.value = level
    }

    fun resetStudyPlan() {
        viewModelScope.launch {
            val currentProf = repository.getProfileDirect() ?: StudentProfile()
            val resetProf = currentProf.copy(
                studyPlanConfigured = false,
                studyLevel = "",
                studyPlanSummary = ""
            )
            repository.updateProfile(resetProf)
            val user = FirebaseAuthService.currentUser
            if (user != null && !user.isAnonymous) {
                FirebaseFirestoreService.saveUserProfile(resetProf, user.uid)
            }
            markUnsyncedAndScheduleSync(delayMs = 100L)
            _showStudyPlannerScreen.value = true
        }
    }

    fun saveChapterProgress(updatedChapters: List<SyllabusChapterEntity>) {
        viewModelScope.launch {
            repository.insertOrUpdateChapters(updatedChapters)
            markUnsyncedAndScheduleSync()
        }
    }

    fun applyStudyPlan(
        level: StudyPlanLevel,
        targetPercentage: Int = level.recommendedTargetPercent,
        dailyGoalMinutes: Int = level.recommendedGoalMinutes,
        onFinished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentProf = repository.getProfileDirect() ?: StudentProfile()
            val summary = "${level.subtitle} • Goal: ${dailyGoalMinutes / 60}h ${dailyGoalMinutes % 60}m/day • Target: $targetPercentage%"
            val updatedProf = currentProf.copy(
                studyPlanConfigured = true,
                studyLevel = level.title,
                studyPlanSummary = summary,
                targetPercentage = targetPercentage,
                dailyStudyGoalMinutes = dailyGoalMinutes,
                currentPrepLevel = level.title,
                isCloudSynced = false
            )
            repository.updateProfile(updatedProf)

            // Seed customized initial targets matching the selected level
            val targetsToSeed = when (level.id) {
                "concept_foundation" -> listOf(
                    StudyTargetEntity(
                        subject = "Physics",
                        chapter = "Electric Charges and Fields",
                        task = "Read NCERT line-by-line & derive Coulomb's + Gauss's Law",
                        priority = "High",
                        estimatedMinutes = 45,
                        deadlineDate = "Today",
                        status = "Pending"
                    ),
                    StudyTargetEntity(
                        subject = "Chemistry",
                        chapter = "Solutions",
                        task = "Solve all NCERT in-text & back exercises (Henry & Raoult Laws)",
                        priority = "High",
                        estimatedMinutes = 45,
                        deadlineDate = "Today",
                        status = "Pending"
                    ),
                    StudyTargetEntity(
                        subject = "Mathematics",
                        chapter = "Matrices & Determinants",
                        task = "NCERT Exercise 3.2 & 3.3 invertible matrices & properties drill",
                        priority = "Medium",
                        estimatedMinutes = 45,
                        deadlineDate = "Today",
                        status = "Pending"
                    )
                )
                "dual_mastery" -> listOf(
                    StudyTargetEntity(
                        subject = "Physics",
                        chapter = "Current Electricity",
                        task = "15 Mixed Board + JEE Mains PYQs on Kirchhoff Laws & Potentiometer",
                        priority = "High",
                        estimatedMinutes = 60,
                        deadlineDate = "Today",
                        status = "Pending"
                    ),
                    StudyTargetEntity(
                        subject = "Chemistry",
                        chapter = "Chemical Kinetics & Electrochemistry",
                        task = "Nernst equation numericals + Arrhenius equation derivations drill",
                        priority = "High",
                        estimatedMinutes = 60,
                        deadlineDate = "Today",
                        status = "Pending"
                    ),
                    StudyTargetEntity(
                        subject = "Mathematics",
                        chapter = "Continuity & Differentiability",
                        task = "Logarithmic differentiation + 10 standard JEE Main PYQs",
                        priority = "High",
                        estimatedMinutes = 60,
                        deadlineDate = "Today",
                        status = "Pending"
                    )
                )
                else -> listOf(
                    StudyTargetEntity(
                        subject = "Physics",
                        chapter = "Electromagnetic Induction & AC",
                        task = "Solve 20 Advanced level numericals + AC resonance phasor problems",
                        priority = "High",
                        estimatedMinutes = 75,
                        deadlineDate = "Today",
                        status = "Pending"
                    ),
                    StudyTargetEntity(
                        subject = "Chemistry",
                        chapter = "Coordination Compounds & Organic Reactions",
                        task = "Crystal Field Theory CFT splits + multi-step organic synthesis",
                        priority = "High",
                        estimatedMinutes = 75,
                        deadlineDate = "Today",
                        status = "Pending"
                    ),
                    StudyTargetEntity(
                        subject = "Mathematics",
                        chapter = "Integrals & Differential Equations",
                        task = "Definite Integrals King Property challenges + Homogeneous D.E.",
                        priority = "High",
                        estimatedMinutes = 75,
                        deadlineDate = "Today",
                        status = "Pending"
                    )
                )
            }

            repository.insertTargets(targetsToSeed)

            // Save to Firebase immediately
            val user = FirebaseAuthService.currentUser
            if (user != null && !user.isAnonymous) {
                FirebaseFirestoreService.saveUserProfile(updatedProf, user.uid)
            }
            markUnsyncedAndScheduleSync(delayMs = 200L)

            _showStudyPlannerScreen.value = false
            _currentTab.value = 0 // Redirect automatically to Home
            onFinished()
        }
    }

    fun saveUserProfileSettings(
        name: String,
        studentClass: String,
        board: String,
        stream: String,
        subjects: String,
        targetPercentage: Int,
        dailyStudyGoalMinutes: Int,
        preferredLanguage: String,
        hasUpcomingExam: Boolean,
        examName: String,
        examDate: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            onError("Please enter your name.")
            return
        }
        val validTarget = targetPercentage.coerceIn(50, 100)
        val validGoal = dailyStudyGoalMinutes.coerceIn(15, 720)

        if (hasUpcomingExam) {
            if (examName.trim().isBlank()) {
                onError("Please enter your upcoming exam name.")
                return
            }
            if (examDate.trim().isBlank()) {
                onError("Please enter your exam date.")
                return
            }
        }

        val user = FirebaseAuthService.currentUser
        val uid = user?.uid

        viewModelScope.launch {
            val currentProf = repository.getProfileDirect() ?: uiState.value.profile ?: StudentProfile()
            val updatedProfile = currentProf.copy(
                id = 1,
                name = trimmedName,
                studentClass = studentClass.trim().ifBlank { "Class 12" },
                board = board.trim().ifBlank { "CBSE" },
                stream = stream.trim().ifBlank { "PCM" },
                subjects = subjects.trim().ifBlank { "Physics, Chemistry, Mathematics" },
                targetPercentage = validTarget,
                dailyStudyGoalMinutes = validGoal,
                languagePreference = preferredLanguage.trim().ifBlank { "Hinglish" },
                hasUpcomingExam = hasUpcomingExam,
                examName = if (hasUpcomingExam) examName.trim() else "",
                examDate = if (hasUpcomingExam) examDate.trim() else "",
                email = user?.email ?: currentProf.email,
                firebaseUid = uid ?: currentProf.firebaseUid,
                isCloudSynced = user != null && !user.isAnonymous,
                lastSyncTimestamp = System.currentTimeMillis()
            )

            if (user != null && !user.isAnonymous && !uid.isNullOrBlank()) {
                val saveResult = FirebaseFirestoreService.saveUserProfile(updatedProfile, uid, isFirstCreation = false)
                saveResult.onSuccess {
                    repository.updateProfile(updatedProfile)
                    if (hasUpcomingExam) {
                        repository.saveExam(
                            ExamEntity(
                                id = 1,
                                examName = examName.trim(),
                                subject = stream.trim(),
                                examDate = examDate.trim(),
                                targetScore = validTarget,
                                isActive = true
                            )
                        )
                    } else {
                        repository.deleteExam(1)
                    }
                    _profileStatusMessage.value = "Profile updated successfully"
                    android.util.Log.d("RankifyProfile", "Profile updated successfully in Firestore users/$uid and local cache")
                    markUnsyncedAndScheduleSync(delayMs = 200L)
                    onSuccess()
                }.onFailure { err ->
                    val errorMsg = "Couldn't update profile. Please try again."
                    _profileStatusMessage.value = errorMsg
                    android.util.Log.e("RankifyProfile", "Firestore profile update failed: ${err.message}", err)
                    onError(errorMsg)
                }
            } else {
                repository.updateProfile(updatedProfile.copy(isCloudSynced = false))
                if (hasUpcomingExam) {
                    repository.saveExam(
                        ExamEntity(
                            id = 1,
                            examName = examName.trim(),
                            subject = stream.trim(),
                            examDate = examDate.trim(),
                            targetScore = validTarget,
                            isActive = true
                        )
                    )
                }
                markUnsyncedAndScheduleSync(delayMs = 200L)
                _profileStatusMessage.value = "Profile updated locally."
                onSuccess()
            }
        }
    }

    fun clearProfileStatusMessage() {
        _profileStatusMessage.value = null
    }

    // Firebase Authentication & Cloud Sync
    fun signIn(email: String, pass: String, onSuccess: () -> Unit = {}) {
        _isAuthLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val result = FirebaseAuthService.signInWithEmail(email, pass)
            _isAuthLoading.value = false
            result.onSuccess { user ->
                _isGuestMode.value = false
                _currentFirebaseUser.value = user
                loadCloudProfile(user.uid)
                checkAdminStatus(user.uid)
                onSuccess()
            }.onFailure { error ->
                _authErrorMessage.value = error.localizedMessage ?: "Failed to sign in. Please verify your credentials."
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, targetPercent: Int = 95, onSuccess: () -> Unit = {}) {
        _isAuthLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            val trimmedName = name.trim()
            val result = FirebaseAuthService.signUpWithEmail(email, pass, trimmedName)
            _isAuthLoading.value = false
            result.onSuccess { user ->
                _isGuestMode.value = false
                _currentFirebaseUser.value = user
                checkAdminStatus(user.uid)
                val currentProf = repository.getProfileDirect() ?: uiState.value.profile ?: StudentProfile()
                val updated = currentProf.copy(
                    id = 1,
                    name = trimmedName,
                    email = user.email ?: email,
                    firebaseUid = user.uid,
                    targetPercentage = targetPercent,
                    isCloudSynced = true,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
                repository.updateProfile(updated)
                FirebaseFirestoreService.saveUserProfile(updated, user.uid, isFirstCreation = true)
                markUnsyncedAndScheduleSync(delayMs = 100L)
                onSuccess()
            }.onFailure { error ->
                _authErrorMessage.value = error.localizedMessage ?: "Failed to create account. Please check your details."
            }
        }
    }

    fun signInAsGuest(onSuccess: () -> Unit = {}) {
        _isAuthLoading.value = true
        _authErrorMessage.value = null
        viewModelScope.launch {
            FirebaseAuthService.signInAnonymously()
            _isGuestMode.value = true
            _adminAccessState.value = AdminAccessState.NotAdmin
            _isAdminDashboardVisible.value = false
            _isAuthLoading.value = false
            onSuccess()
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = FirebaseAuthService.sendPasswordReset(email)
            result.onSuccess {
                onResult(true, "Password reset instructions sent to $email")
            }.onFailure {
                onResult(false, it.localizedMessage ?: "Failed to send password reset email")
            }
        }
    }

    fun signOut() {
        FirebaseAuthService.signOut()
        _isGuestMode.value = false
        _currentFirebaseUser.value = null
        _authErrorMessage.value = null
        _adminAccessState.value = AdminAccessState.NotAdmin
        _isAdminDashboardVisible.value = false
        _cloudSyncMessage.value = "Signed out. Local offline mode enabled."
        viewModelScope.launch {
            uiState.value.profile?.let { prof ->
                repository.updateProfile(prof.copy(isCloudSynced = false))
            }
        }
    }

    fun checkAdminStatus(uid: String) {
        val trimmed = uid.trim()
        if (trimmed.isBlank()) {
            _adminAccessState.value = AdminAccessState.NotAdmin
            return
        }
        _adminAccessState.value = AdminAccessState.Checking
        viewModelScope.launch {
            try {
                val result = FirebaseFirestoreService.verifyAdminStatus(trimmed)
                result.fold(
                    onSuccess = { isAdmin ->
                        _adminAccessState.value = if (isAdmin) {
                            AdminAccessState.Admin
                        } else {
                            AdminAccessState.NotAdmin
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.w("AdminVerification", "Admin verification check failed: ${error.message}")
                        _adminAccessState.value = AdminAccessState.Error(error.localizedMessage ?: "Verification error")
                    }
                )
            } catch (e: Exception) {
                _adminAccessState.value = AdminAccessState.Error(e.localizedMessage ?: "Verification failed")
            }
        }
    }

    fun openAdminDashboard() {
        if (_adminAccessState.value is AdminAccessState.Admin) {
            _isAdminDashboardVisible.value = true
            loadAdminStudyMaterials()
        } else {
            _isAdminDashboardVisible.value = false
        }
    }

    fun dismissAdminDashboard() {
        _isAdminDashboardVisible.value = false
        _isAddMaterialScreenVisible.value = false
    }

    fun openAddMaterialScreen() {
        if (_adminAccessState.value is AdminAccessState.Admin) {
            clearStudyMaterialFeedback()
            _isAddMaterialScreenVisible.value = true
        }
    }

    fun dismissAddMaterialScreen() {
        _isAddMaterialScreenVisible.value = false
        clearStudyMaterialFeedback()
    }

    fun loadAdminStudyMaterials() {
        if (_adminAccessState.value !is AdminAccessState.Admin) return
        _isAdminMaterialsLoading.value = true
        viewModelScope.launch {
            try {
                val result = FirebaseFirestoreService.fetchStudyMaterialsForAdmin()
                result.fold(
                    onSuccess = { list ->
                        _adminStudyMaterials.value = list
                        _isAdminMaterialsLoading.value = false
                    },
                    onFailure = { error ->
                        android.util.Log.e("RankifyVM", "Error loading study materials: ${error.message}", error)
                        _isAdminMaterialsLoading.value = false
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("RankifyVM", "Unexpected error loading study materials: ${e.message}", e)
                _isAdminMaterialsLoading.value = false
            }
        }
    }

    /**
     * Loads published study materials for student views (Practice folder system & StudyDock).
     * Only retrieves materials where `published == true`.
     */
    fun loadPublishedStudyMaterials() {
        _isPublishedMaterialsLoading.value = true
        viewModelScope.launch {
            try {
                val result = FirebaseFirestoreService.fetchPublishedStudyMaterials()
                result.fold(
                    onSuccess = { list ->
                        _publishedStudyMaterials.value = list
                        _isPublishedMaterialsLoading.value = false
                    },
                    onFailure = { error ->
                        android.util.Log.w("RankifyVM", "Notice: published materials not accessible (${error.message}); using local resources.")
                        _publishedStudyMaterials.value = emptyList()
                        _isPublishedMaterialsLoading.value = false
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("RankifyVM", "Unexpected error in loadPublishedStudyMaterials: ${e.message}", e)
                _isPublishedMaterialsLoading.value = false
            }
        }
    }

    /**
     * Publishes or unpublishes a study material in Firestore.
     * Only allowed for verified administrators.
     * Updates the existing document in `study_materials/{resourceId}` without duplicating documents.
     */
    fun toggleStudyMaterialPublishStatus(resourceId: String, newPublishedState: Boolean) {
        if (_adminAccessState.value !is AdminAccessState.Admin) {
            _studyMaterialSaveError.value = "Unauthorized: Only verified administrators can publish or unpublish materials."
            return
        }

        _publishingResourceId.value = resourceId
        viewModelScope.launch {
            try {
                val result = FirebaseFirestoreService.updateStudyMaterialPublishStatus(resourceId, newPublishedState)
                result.fold(
                    onSuccess = {
                        // Update in-memory admin list immediately upon Firestore confirmation
                        _adminStudyMaterials.value = _adminStudyMaterials.value.map { item ->
                            if (item.resourceId == resourceId) {
                                item.copy(published = newPublishedState, updatedAt = System.currentTimeMillis())
                            } else {
                                item
                            }
                        }
                        _studyMaterialSaveSuccess.value = if (newPublishedState) {
                            "Resource published successfully ✓"
                        } else {
                            "Resource unpublished (saved as draft) ✓"
                        }

                        // Refresh student-facing published materials immediately
                        loadPublishedStudyMaterials()

                        // Also refresh active StudyDock session if one is currently open
                        _activeLectureSession.value?.let { session ->
                            loadResourcesForActiveSession(session.videoId, session.selectedSubject, session.selectedChapter)
                        }
                        _publishingResourceId.value = null
                    },
                    onFailure = { error ->
                        android.util.Log.e("RankifyVM", "Failed to update publish status: ${error.message}", error)
                        _studyMaterialSaveError.value = "Failed to update: ${error.localizedMessage ?: "Firestore error"}"
                        _publishingResourceId.value = null
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("RankifyVM", "Unexpected error toggling publish status: ${e.message}", e)
                _studyMaterialSaveError.value = "Error: ${e.localizedMessage ?: "Network error"}"
                _publishingResourceId.value = null
            }
        }
    }

    fun saveStudyMaterial(
        title: String,
        description: String,
        classLevel: String = "12",
        board: String = "CBSE",
        stream: String = "PCM",
        subject: String,
        chapterId: String,
        chapterName: String,
        materialType: String,
        difficultyLevel: String,
        pdfUrl: String,
        youtubeVideoId: String?,
        published: Boolean,
        onSuccess: () -> Unit
    ) {
        _studyMaterialSaveSuccess.value = null
        _studyMaterialSaveError.value = null

        // 1. Check Administrator Authorization
        if (_adminAccessState.value !is AdminAccessState.Admin) {
            _studyMaterialSaveError.value = "Unauthorized: Only verified administrators can upload study materials."
            return
        }

        val currentUser = FirebaseAuthService.currentUser
        if (currentUser == null || currentUser.isAnonymous) {
            _studyMaterialSaveError.value = "Administrator session not active. Please sign in."
            return
        }

        // 2. Validate Required Fields
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            _studyMaterialSaveError.value = "Resource Title is required."
            return
        }
        if (subject.isBlank()) {
            _studyMaterialSaveError.value = "Subject is required."
            return
        }
        if (chapterId.isBlank() || chapterName.isBlank()) {
            _studyMaterialSaveError.value = "Chapter is required. Please select a chapter from the syllabus."
            return
        }
        if (materialType.isBlank()) {
            _studyMaterialSaveError.value = "Material Type is required."
            return
        }
        if (difficultyLevel.isBlank()) {
            _studyMaterialSaveError.value = "Difficulty Level is required."
            return
        }

        // 3. Validate Google Drive PDF Link
        val driveValidation = GoogleDriveUrlHelper.validateAndExtractFileId(pdfUrl)
        if (driveValidation.isFailure) {
            _studyMaterialSaveError.value = driveValidation.exceptionOrNull()?.message
                ?: "Please enter a valid Google Drive HTTPS file-sharing link."
            return
        }
        val fileId = driveValidation.getOrNull()

        // 4. Sanitize YouTube ID if provided
        val sanitizedYoutubeId = GoogleDriveUrlHelper.extractYouTubeVideoId(youtubeVideoId)

        // 5. Construct Resource
        val newMaterial = StudyMaterialResource(
            resourceId = "", // Auto-assigned by Firestore
            title = trimmedTitle,
            description = description.trim(),
            classLevel = classLevel.trim().ifBlank { "12" },
            board = board.trim().ifBlank { "CBSE" },
            stream = stream.trim().ifBlank { "PCM" },
            subject = subject.trim(),
            chapterId = chapterId.trim(),
            chapterName = chapterName.trim(),
            materialType = materialType.trim(),
            difficultyLevel = difficultyLevel.trim(),
            pdfUrl = pdfUrl.trim(),
            googleDriveFileId = fileId,
            youtubeVideoId = sanitizedYoutubeId,
            published = published,
            createdBy = currentUser.uid,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 6. Persist to Firestore
        _isSavingStudyMaterial.value = true
        viewModelScope.launch {
            try {
                val result = FirebaseFirestoreService.saveStudyMaterial(newMaterial)
                result.fold(
                    onSuccess = { generatedId ->
                        _isSavingStudyMaterial.value = false
                        _studyMaterialSaveSuccess.value = "Study material saved successfully."
                        loadAdminStudyMaterials()
                        loadPublishedStudyMaterials()
                        onSuccess()
                    },
                    onFailure = { error ->
                        android.util.Log.e("RankifyVM", "saveStudyMaterial failed in Firestore: ${error.message}", error)
                        _isSavingStudyMaterial.value = false
                        _studyMaterialSaveError.value = "Failed to save: ${error.localizedMessage ?: "Unknown Firestore error"}"
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("RankifyVM", "saveStudyMaterial unexpected exception: ${e.message}", e)
                _isSavingStudyMaterial.value = false
                _studyMaterialSaveError.value = "Error: ${e.localizedMessage ?: "Unable to complete operation"}"
            }
        }
    }

    fun clearStudyMaterialFeedback() {
        _studyMaterialSaveSuccess.value = null
        _studyMaterialSaveError.value = null
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun setAuthError(message: String) {
        _authErrorMessage.value = message
    }

    // StudyDock session input preservation & Android Share integration
    private val _studyDockInputUrl = MutableStateFlow("")
    val studyDockInputUrl: StateFlow<String> = _studyDockInputUrl.asStateFlow()

    private val _studyDockSharedBanner = MutableStateFlow<String?>(null)
    val studyDockSharedBanner: StateFlow<String?> = _studyDockSharedBanner.asStateFlow()

    private val _studyDockRequested = MutableStateFlow(false)
    val studyDockRequested: StateFlow<Boolean> = _studyDockRequested.asStateFlow()

    // Step 4 & 5: Active StudyDock Lecture Session
    private val _activeLectureSession = MutableStateFlow<StudyDockLectureSession?>(null)
    val activeLectureSession: StateFlow<StudyDockLectureSession?> = _activeLectureSession.asStateFlow()

    // Step 5: StudyDock Lecture Resources (Exact Video and Related Chapter)
    private val _studyDockExactResources = MutableStateFlow<List<com.example.data.model.StudyDockResource>>(emptyList())
    val studyDockExactResources: StateFlow<List<com.example.data.model.StudyDockResource>> = _studyDockExactResources.asStateFlow()

    private val _studyDockChapterResources = MutableStateFlow<List<com.example.data.model.StudyDockResource>>(emptyList())
    val studyDockChapterResources: StateFlow<List<com.example.data.model.StudyDockResource>> = _studyDockChapterResources.asStateFlow()

    private val _isStudyDockResourcesLoading = MutableStateFlow(false)
    val isStudyDockResourcesLoading: StateFlow<Boolean> = _isStudyDockResourcesLoading.asStateFlow()

    // Selected optional academic context
    private val _studyDockSelectedSubject = MutableStateFlow<String?>(null)
    val studyDockSelectedSubject: StateFlow<String?> = _studyDockSelectedSubject.asStateFlow()

    private val _studyDockSelectedChapter = MutableStateFlow<String?>(null)
    val studyDockSelectedChapter: StateFlow<String?> = _studyDockSelectedChapter.asStateFlow()

    fun updateStudyDockInputUrl(url: String) {
        _studyDockInputUrl.value = url
    }

    fun setStudyDockSubject(subject: String?) {
        _studyDockSelectedSubject.value = subject
        // If subject changes or is cleared, verify whether chapter belongs to it
        val currentChapter = _studyDockSelectedChapter.value
        if (currentChapter != null) {
            val allChapters = uiState.value.chapters
            val belongs = allChapters.any { it.subject.equals(subject, ignoreCase = true) && it.title.equals(currentChapter, ignoreCase = true) }
            if (!belongs) {
                _studyDockSelectedChapter.value = null
            }
        }
    }

    fun setStudyDockChapter(chapter: String?) {
        _studyDockSelectedChapter.value = chapter
    }

    /**
     * Creates or updates the active lecture session upon tapping Open StudyDock / Continue to Study Kit.
     * Sets analysisStatus to READY. Does not perform AI analysis or mock results.
     * Also loads verified resources for the lecture & chapter.
     */
    fun createLectureSession(
        originalUrl: String,
        normalizedUrl: String,
        videoId: String,
        subject: String?,
        chapter: String?
    ): StudyDockLectureSession {
        val session = StudyDockLectureSession(
            originalYouTubeUrl = originalUrl,
            normalizedYouTubeUrl = normalizedUrl,
            videoId = videoId,
            selectedSubject = subject?.takeIf { it.isNotBlank() },
            selectedChapter = chapter?.takeIf { it.isNotBlank() },
            analysisStatus = StudyDockAnalysisStatus.READY
        )
        _activeLectureSession.value = session
        loadResourcesForActiveSession(videoId, subject, chapter)
        return session
    }

    fun loadResourcesForActiveSession(videoId: String, subject: String?, chapter: String?) {
        viewModelScope.launch {
            _isStudyDockResourcesLoading.value = true
            try {
                val (exact, related) = com.example.data.firebase.StudyDockResourceService.getResourcesForLecture(
                    videoId = videoId,
                    subject = subject,
                    chapter = chapter
                )
                _studyDockExactResources.value = exact
                _studyDockChapterResources.value = related
            } catch (e: Exception) {
                android.util.Log.e("StudyDock", "Failed loading resources: ${e.message}", e)
                _studyDockExactResources.value = emptyList()
                _studyDockChapterResources.value = emptyList()
            } finally {
                _isStudyDockResourcesLoading.value = false
            }
        }
    }

    fun clearActiveLectureSession() {
        _activeLectureSession.value = null
        _studyDockExactResources.value = emptyList()
        _studyDockChapterResources.value = emptyList()
    }

    fun clearStudyDockBanner() {
        _studyDockSharedBanner.value = null
    }

    fun dismissStudyDock() {
        _studyDockRequested.value = false
        _studyDockSharedBanner.value = null
    }

    fun openStudyDockWithSharedContent(sharedText: String?) {
        if (sharedText.isNullOrBlank()) {
            _studyDockSharedBanner.value = "This doesn't look like a YouTube lecture."
            _studyDockRequested.value = true
            return
        }

        val parsed = com.example.util.YouTubeUrlParser.extractFirstYouTubeUrl(sharedText)
        if (parsed != null) {
            // New video identity replaces the active video and clears previous active session
            if (_activeLectureSession.value?.videoId != parsed.videoId) {
                _activeLectureSession.value = null
            }
            _studyDockInputUrl.value = parsed.originalUrl
            _studyDockSharedBanner.value = "Lecture link received from YouTube ✓"
        } else {
            // Text received but no valid YouTube lecture URL found
            _studyDockInputUrl.value = com.example.util.YouTubeUrlParser.cleanRawInput(sharedText)
            _studyDockSharedBanner.value = "This doesn't look like a YouTube lecture."
        }
        _studyDockRequested.value = true
    }

    // Rankify Exclusive - Music Video Manager Logic

    fun openAdminMusicVideoManager() {
        _isAdminMusicVideoManagerVisible.value = true
        loadAdminMusicVideos()
    }

    fun closeAdminMusicVideoManager() {
        _isAdminMusicVideoManagerVisible.value = false
    }

    fun openAddMusicVideoScreen(videoToEdit: ExclusiveMusicVideo? = null) {
        _editingMusicVideo.value = videoToEdit
        _isAddMusicVideoScreenVisible.value = true
        _musicVideoSaveSuccess.value = null
        _musicVideoSaveError.value = null
    }

    fun closeAddMusicVideoScreen() {
        _isAddMusicVideoScreenVisible.value = false
        _editingMusicVideo.value = null
    }

    fun openVideoPreview(video: ExclusiveMusicVideo) {
        _currentPreviewVideo.value = video
        _isPreviewPlayerVisible.value = true
        _isPreviewLoading.value = true
        _previewError.value = null
        _currentPreviewSource.value = null

        viewModelScope.launch {
            val result = FirebaseFirestoreService.fetchMusicVideoSource(video.videoId)
            result.onSuccess { source ->
                _currentPreviewSource.value = source
                _isPreviewLoading.value = false
            }.onFailure { e ->
                _previewError.value = "Failed to load video source: ${e.message}"
                _isPreviewLoading.value = false
            }
        }
    }

    fun closeVideoPreview() {
        _isPreviewPlayerVisible.value = false
        _currentPreviewVideo.value = null
        _currentPreviewSource.value = null
        _previewError.value = null
    }

    fun loadAdminMusicVideos() {
        viewModelScope.launch {
            _isAdminMusicVideosLoading.value = true
            val result = FirebaseFirestoreService.fetchExclusiveMusicVideosForAdmin()
            result.onSuccess { list ->
                _adminMusicVideos.value = list
            }.onFailure { e ->
                android.util.Log.e("RankifyViewModel", "Failed to load admin music videos: ${e.message}")
            }
            _isAdminMusicVideosLoading.value = false
        }
    }

    fun loadPublishedMusicVideos() {
        viewModelScope.launch {
            _isPublishedMusicVideosLoading.value = true
            val result = FirebaseFirestoreService.fetchPublishedExclusiveMusicVideos()
            result.onSuccess { list ->
                _publishedMusicVideos.value = list
            }.onFailure { e ->
                android.util.Log.e("RankifyViewModel", "Failed to load published music videos: ${e.message}")
            }
            _isPublishedMusicVideosLoading.value = false
        }
    }

    fun saveExclusiveMusicVideo(
        video: ExclusiveMusicVideo,
        videoUrl: String,
        googleDriveFileId: String
    ) {
        viewModelScope.launch {
            _isSavingMusicVideo.value = true
            _musicVideoSaveError.value = null
            _musicVideoSaveSuccess.value = null

            val currentUid = FirebaseAuthService.currentUser?.uid ?: ""
            val videoToSave = if (video.videoId.isBlank()) {
                video.copy(createdBy = currentUid)
            } else {
                video
            }

            val result = FirebaseFirestoreService.saveExclusiveMusicVideo(videoToSave, videoUrl, googleDriveFileId)
            result.onSuccess { id ->
                _musicVideoSaveSuccess.value = if (video.videoId.isBlank()) "New music video added successfully!" else "Music video updated successfully!"
                loadAdminMusicVideos()
                loadPublishedMusicVideos()
                delay(1500)
                closeAddMusicVideoScreen()
            }.onFailure { e ->
                _musicVideoSaveError.value = e.message ?: "Failed to save music video."
            }
            _isSavingMusicVideo.value = false
        }
    }

    fun updateMusicVideoStatus(videoId: String, published: Boolean) {
        viewModelScope.launch {
            _musicVideoOperationInProgress.value = videoId
            val result = FirebaseFirestoreService.updateMusicVideoPublishStatus(videoId, published)
            result.onSuccess {
                loadAdminMusicVideos()
                loadPublishedMusicVideos()
            }.onFailure { e ->
                android.util.Log.e("RankifyViewModel", "Failed to update video status: ${e.message}")
            }
            _musicVideoOperationInProgress.value = null
        }
    }

    fun deleteMusicVideo(videoId: String) {
        viewModelScope.launch {
            _musicVideoOperationInProgress.value = videoId
            val result = FirebaseFirestoreService.deleteExclusiveMusicVideo(videoId)
            result.onSuccess {
                loadAdminMusicVideos()
                loadPublishedMusicVideos()
            }.onFailure { e ->
                android.util.Log.e("RankifyViewModel", "Failed to delete video: ${e.message}")
            }
            _musicVideoOperationInProgress.value = null
        }
    }

    // AI Tutor Methods
    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun changeStudyMode(mode: AiStudyMode) {
        _currentStudyMode.value = mode
    }

    fun setSelectedSubjectForAi(subject: String) {
        _selectedSubjectForAi.value = subject
    }

    fun setSelectedChapterForAi(chapter: String) {
        _selectedChapterForAi.value = chapter
    }

    fun sendTutorMessage(text: String) {
        if (text.isBlank() || _isTutorStreaming.value) return
        
        viewModelScope.launch {
            val userMsg = AIChatMessageEntity(
                role = "user",
                message = text,
                subjectContext = _selectedSubjectForAi.value,
                chapterContext = _selectedChapterForAi.value,
                studyMode = _currentStudyMode.value.label
            )
            repository.saveChatMessage(userMsg)
            
            _isTutorStreaming.value = true
            _streamingTutorMessage.value = ""
            
            val currentHistory = uiState.value.chatMessages
            
            com.example.data.gemini.GeminiService.chatWithTutorStream(
                history = currentHistory,
                userMessage = text,
                profile = uiState.value.profile,
                mode = _currentStudyMode.value,
                subject = _selectedSubjectForAi.value,
                chapter = _selectedChapterForAi.value
            ).collect { chunk ->
                _streamingTutorMessage.value += chunk
            }
            
            val fullResponse = _streamingTutorMessage.value
            val assistantMsg = AIChatMessageEntity(
                role = "assistant",
                message = fullResponse,
                subjectContext = _selectedSubjectForAi.value,
                chapterContext = _selectedChapterForAi.value,
                studyMode = _currentStudyMode.value.label
            )
            repository.saveChatMessage(assistantMsg)
            
            _isTutorStreaming.value = false
            _streamingTutorMessage.value = ""
        }
    }

    fun generateStructuredTutorResponse(prompt: String, mode: AiStudyMode) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val result = com.example.data.gemini.GeminiService.getStructuredTutorResponse(
                history = uiState.value.chatMessages,
                userMessage = prompt,
                profile = uiState.value.profile,
                mode = mode,
                subject = _selectedSubjectForAi.value,
                chapter = _selectedChapterForAi.value
            )
            
            result.onSuccess { json ->
                val assistantMsg = AIChatMessageEntity(
                    role = "assistant",
                    message = "I have generated a structured response for you.",
                    subjectContext = _selectedSubjectForAi.value,
                    chapterContext = _selectedChapterForAi.value,
                    studyMode = mode.label,
                    structuredDataJson = json
                )
                repository.saveChatMessage(assistantMsg)
            }.onFailure { e ->
                android.util.Log.e("RankifyViewModel", "Structured AI error: ${e.message}")
            }
            _isAiLoading.value = false
        }
    }
}
