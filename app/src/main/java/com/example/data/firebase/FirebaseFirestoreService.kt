package com.example.data.firebase

import android.util.Log
import com.example.RankifyApplication
import com.example.data.model.CoinTransaction
import com.example.data.model.ExclusiveMusicVideo
import com.example.data.model.ExclusiveMusicVideoSource
import com.example.data.model.NoteEntity
import com.example.data.model.RankifyCoinWallet
import com.example.data.model.SongUnlock
import com.example.data.model.StudentProfile
import com.example.data.model.StudyMaterialResource
import com.example.data.model.StudyPointTransaction
import com.example.data.model.StudyPointWallet
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity
import com.example.data.model.TestAttemptEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class RestoredUserData(
    val profileData: Map<String, Any>?,
    val notes: List<NoteEntity>,
    val studyTargets: List<StudyTargetEntity>,
    val testAttempts: List<TestAttemptEntity>,
    val syllabusProgress: Map<String, Map<String, Boolean>>
)

object FirebaseFirestoreService {
    private const val TAG = "FirebaseFirestoreService"
    private const val DEFAULT_TIMEOUT_MS = 15000L

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            RankifyApplication.appContext?.let { RankifyApplication.ensureFirebase(it) }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore.getInstance() failed: ${e.message}", e)
            null
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Executes a Firebase request with debug logging, execution time measurement, and timeout safety.
     */
    suspend fun <T> runLoggedRequest(
        operationName: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        block: suspend (FirebaseFirestore) -> Result<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        val firestoreInstance = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException(RankifyApplication.initializationError ?: "Firebase is not initialized.")
        )
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "[FIREBASE REQUEST START] $operationName")
        try {
            val result = withTimeoutOrNull(timeoutMs) {
                block(firestoreInstance)
            } ?: Result.failure(Exception("Operation '$operationName' timed out after ${timeoutMs}ms. Please check your network connection."))

            val elapsed = System.currentTimeMillis() - startTime
            if (result.isSuccess) {
                Log.d(TAG, "[FIREBASE REQUEST SUCCESS] $operationName completed in ${elapsed}ms")
            } else {
                val err = result.exceptionOrNull()
                Log.e(TAG, "[FIREBASE REQUEST FAILURE] $operationName failed after ${elapsed}ms: ${err?.message}", err)
            }
            result
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "[FIREBASE REQUEST ERROR] $operationName threw exception after ${elapsed}ms: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the user's verified coin wallet (Read-only for clients).
     */
    suspend fun fetchCoinWallet(uid: String): Result<RankifyCoinWallet?> =
        runLoggedRequest("fetchCoinWallet(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("users")
                        .document(uid)
                        .collection("coin_wallet")
                        .document("current")
                        .get()
                        .addOnSuccessListener { doc ->
                            continuation.resume(Result.success(doc.toObject(RankifyCoinWallet::class.java)))
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches the user's provisional study points wallet.
     */
    suspend fun fetchStudyPointWallet(uid: String): Result<StudyPointWallet?> =
        runLoggedRequest("fetchStudyPointWallet(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("users")
                        .document(uid)
                        .collection("study_points")
                        .document("current")
                        .get()
                        .addOnSuccessListener { doc ->
                            continuation.resume(Result.success(doc.toObject(StudyPointWallet::class.java)))
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches recent coin transactions.
     */
    suspend fun fetchCoinTransactions(uid: String): Result<List<CoinTransaction>> =
        runLoggedRequest("fetchCoinTransactions(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("users")
                        .document(uid)
                        .collection("coin_transactions")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val transactions = snapshot.documents.mapNotNull { it.toObject(CoinTransaction::class.java) }
                            continuation.resume(Result.success(transactions))
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches recent study point transactions.
     */
    suspend fun fetchStudyPointTransactions(uid: String): Result<List<StudyPointTransaction>> =
        runLoggedRequest("fetchStudyPointTransactions(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("users")
                        .document(uid)
                        .collection("study_point_transactions")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val transactions = snapshot.documents.mapNotNull { it.toObject(StudyPointTransaction::class.java) }
                            continuation.resume(Result.success(transactions))
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Awards PROVISIONAL study points with transaction check and daily limits.
     * Note: This is client-side logic and not tamper-proof without trusted validation.
     */
    suspend fun awardStudyPoints(
        uid: String,
        amount: Int,
        sourceType: String,
        sourceId: String,
        description: String,
        dailyLimit: Int
    ): Result<Boolean> = runLoggedRequest("awardStudyPoints(uid=$uid, type=$sourceType, amt=$amount)") { firestoreInstance ->
        suspendCancellableCoroutine { continuation ->
            val today = getTodayDateString()
            val transactionDocId = "REWARD_${sourceType}_${sourceId}"
            val walletRef = firestoreInstance.collection("users").document(uid)
                .collection("study_points").document("current")
            val transRef = firestoreInstance.collection("users").document(uid)
                .collection("study_point_transactions").document(transactionDocId)

            firestoreInstance.runTransaction { transaction ->
                // 1. Check if already rewarded
                val existingTrans = transaction.get(transRef)
                if (existingTrans.exists()) {
                    return@runTransaction false // Already rewarded
                }

                // 2. Read wallet and check limits
                val walletSnap = transaction.get(walletRef)
                val wallet = walletSnap.toObject(StudyPointWallet::class.java) ?: StudyPointWallet()
                
                val currentDailyTotals = if (wallet.lastRewardDate == today) {
                    wallet.dailyCategoryTotals
                } else {
                    emptyMap()
                }

                val categoryTotal = currentDailyTotals[sourceType] ?: 0
                if (categoryTotal + amount > dailyLimit) {
                    return@runTransaction false // Limit reached
                }

                // 3. Update wallet
                val newDailyTotals = currentDailyTotals.toMutableMap()
                newDailyTotals[sourceType] = categoryTotal + amount

                val updatedWallet = hashMapOf<String, Any>(
                    "balance" to wallet.balance + amount,
                    "totalEarned" to wallet.totalEarned + amount,
                    "totalSpent" to wallet.totalSpent, // Required by rules
                    "lastRewardDate" to today,
                    "dailyCategoryTotals" to newDailyTotals,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastTransactionId" to transactionDocId,
                    "lastSourceType" to sourceType,
                    "lastUnlockVideoId" to ""
                )
                transaction.set(walletRef, updatedWallet, SetOptions.merge())

                // 4. Create transaction record
                val pointTrans = hashMapOf<String, Any>(
                    "transactionId" to transactionDocId,
                    "type" to "EARN", // Required by rules
                    "amount" to amount,
                    "sourceType" to sourceType,
                    "sourceId" to sourceId,
                    "description" to description,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(transRef, pointTrans)

                true
            }.addOnSuccessListener { result ->
                continuation.resume(Result.success(result))
            }.addOnFailureListener { e ->
                android.util.Log.e("RankifyFirestore", "awardStudyPoints FAILED: ${e.message}", e)
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Increments the practice attempt counter in the study points document.
     */
    suspend fun incrementPracticeAttempts(uid: String): Result<Unit> =
        runLoggedRequest("incrementPracticeAttempts(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                val walletRef = firestoreInstance.collection("users").document(uid)
                    .collection("study_points").document("current")
                
                firestoreInstance.runTransaction { transaction ->
                    val walletSnap = transaction.get(walletRef)
                    val wallet = walletSnap.toObject(StudyPointWallet::class.java) ?: StudyPointWallet()
                    
                    val today = getTodayDateString()
                    val currentDailyTotals = if (wallet.lastRewardDate == today) {
                        wallet.dailyCategoryTotals.toMutableMap()
                    } else {
                        mutableMapOf()
                    }

                    val currentAttempts = currentDailyTotals["PRACTICE_ATTEMPTS"] ?: 0
                    currentDailyTotals["PRACTICE_ATTEMPTS"] = currentAttempts + 1

                    val data = mapOf(
                        "dailyCategoryTotals" to currentDailyTotals,
                        "lastRewardDate" to today,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastSourceType" to "PRACTICE_ATTEMPT",
                        "lastTransactionId" to "ATTEMPT_${System.currentTimeMillis()}",
                        "lastUnlockVideoId" to "",
                        "totalEarned" to wallet.totalEarned,
                        "totalSpent" to wallet.totalSpent
                    )
                    transaction.set(walletRef, data, SetOptions.merge())
                    null
                }.addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }.addOnFailureListener { e ->
                    android.util.Log.e("RankifyFirestore", "incrementPracticeAttempts FAILED: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }

    suspend fun saveUserProfile(
        profile: StudentProfile,
        uid: String,
        isFirstCreation: Boolean = false
    ): Result<Unit> = runLoggedRequest("saveUserProfile(uid=$uid)") { firestoreInstance ->
        suspendCancellableCoroutine { continuation ->
            try {
                val prepLevel = if (profile.currentPrepLevel.isNotBlank()) profile.currentPrepLevel else profile.studyLevel
                val data = hashMapOf<String, Any>(
                    "name" to profile.name,
                    "email" to profile.email,
                    "class" to profile.studentClass,
                    "studentClass" to profile.studentClass,
                    "board" to profile.board,
                    "stream" to profile.stream,
                    "subjects" to profile.subjects,
                    "targetPercentage" to profile.targetPercentage,
                    "dailyStudyGoalMinutes" to profile.dailyStudyGoalMinutes,
                    "preferredLanguage" to profile.languagePreference,
                    "languagePreference" to profile.languagePreference,
                    "hasUpcomingExam" to profile.hasUpcomingExam,
                    "examName" to profile.examName,
                    "examDate" to profile.examDate,
                    "currentPrepLevel" to prepLevel,
                    "streakDays" to profile.streakDays,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val userRef = firestoreInstance.collection("users").document(uid)
                val batch = firestoreInstance.batch()
                batch.set(userRef, data, SetOptions.merge())

                val planDocRef = userRef.collection("study_plan").document("current")
                val planData = hashMapOf<String, Any>(
                    "studyPlanConfigured" to profile.studyPlanConfigured,
                    "studyLevel" to profile.studyLevel,
                    "studyPlanSummary" to profile.studyPlanSummary,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(planDocRef, planData, SetOptions.merge())

                batch.commit()
                    .addOnSuccessListener {
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun fetchUserProfile(uid: String): Result<Map<String, Any>?> =
        runLoggedRequest("fetchUserProfile(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            continuation.resume(Result.success(documentSnapshot.data))
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    suspend fun syncFullUserData(
        uid: String,
        profile: StudentProfile,
        notes: List<NoteEntity>,
        testAttempts: List<TestAttemptEntity>,
        sessions: List<StudySessionEntity>,
        studyTargets: List<StudyTargetEntity> = emptyList(),
        chapters: List<SyllabusChapterEntity> = emptyList()
    ): Result<String> = runLoggedRequest("syncFullUserData(uid=$uid)") { firestoreInstance ->
        suspendCancellableCoroutine { continuation ->
            try {
                val totalStudyMins = sessions.sumOf { it.durationMinutes }
                val avgScore = if (testAttempts.isNotEmpty()) {
                    testAttempts.sumOf { it.score } * 100 / testAttempts.sumOf { it.maxScore }.coerceAtLeast(1)
                } else 0

                val prepLevel = if (profile.currentPrepLevel.isNotBlank()) profile.currentPrepLevel else profile.studyLevel
                val summary = hashMapOf<String, Any>(
                    "name" to profile.name,
                    "email" to profile.email,
                    "class" to profile.studentClass,
                    "studentClass" to profile.studentClass,
                    "board" to profile.board,
                    "stream" to profile.stream,
                    "subjects" to profile.subjects,
                    "targetPercentage" to profile.targetPercentage,
                    "dailyStudyGoalMinutes" to profile.dailyStudyGoalMinutes,
                    "preferredLanguage" to profile.languagePreference,
                    "languagePreference" to profile.languagePreference,
                    "hasUpcomingExam" to profile.hasUpcomingExam,
                    "examName" to profile.examName,
                    "examDate" to profile.examDate,
                    "currentPrepLevel" to prepLevel,
                    "streakDays" to profile.streakDays,
                    "totalStudyMinutes" to totalStudyMins,
                    "totalTestsAttempted" to testAttempts.size,
                    "averageTestAccuracy" to avgScore,
                    "notesCount" to notes.size,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val batch = firestoreInstance.batch()
                val userRef = firestoreInstance.collection("users").document(uid)
                batch.set(userRef, summary, SetOptions.merge())

                // Extended study plan configuration in subcollection
                val planDocRef = userRef.collection("study_plan").document("current")
                val planData = hashMapOf<String, Any>(
                    "studyPlanConfigured" to profile.studyPlanConfigured,
                    "studyLevel" to profile.studyLevel,
                    "studyPlanSummary" to profile.studyPlanSummary,
                    "targetsCount" to studyTargets.size,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(planDocRef, planData, SetOptions.merge())

                // Sync latest notes (up to 25)
                notes.take(25).forEach { note ->
                    val noteRef = userRef.collection("notes").document(note.id.toString())
                    val noteData = hashMapOf<String, Any>(
                        "id" to note.id,
                        "subject" to note.subject,
                        "chapter" to note.chapter,
                        "title" to note.title,
                        "content" to note.content,
                        "noteType" to note.noteType,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(noteRef, noteData, SetOptions.merge())
                }

                // Sync study targets (up to 25)
                studyTargets.take(25).forEach { target ->
                    val targetRef = userRef.collection("study_targets").document(target.id.toString())
                    val targetData = hashMapOf<String, Any>(
                        "id" to target.id,
                        "subject" to target.subject,
                        "chapter" to target.chapter,
                        "task" to target.task,
                        "priority" to target.priority,
                        "estimatedMinutes" to target.estimatedMinutes,
                        "deadlineDate" to target.deadlineDate,
                        "status" to target.status,
                        "isBacklog" to target.isBacklog,
                        "isWeeklyGoal" to target.isWeeklyGoal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(targetRef, targetData, SetOptions.merge())
                }

                // Sync syllabus chapter progress separately for every user
                if (chapters.isNotEmpty()) {
                    val progressMap = hashMapOf<String, Any>()
                    chapters.forEach { ch ->
                        val stagesData = hashMapOf(
                            "conceptsDone" to ch.conceptsDone,
                            "ncertReadingDone" to ch.ncertReadingDone,
                            "ncertQuestionsDone" to ch.ncertQuestionsDone,
                            "pyqDone" to ch.pyqDone,
                            "revisionDone" to ch.revisionDone,
                            "testDone" to ch.testDone
                        )
                        progressMap[ch.id] = stagesData

                        // Store per-chapter document in chapters_progress collection
                        val chDocRef = userRef.collection("chapters_progress").document(ch.id)
                        val chDocData = hashMapOf<String, Any>(
                            "id" to ch.id,
                            "subject" to ch.subject,
                            "chapterNumber" to ch.chapterNumber,
                            "title" to ch.title,
                            "completionPercentage" to ch.completionPercentage,
                            "completedStages" to ch.completedStages,
                            "status" to when {
                                ch.completionPercentage == 0 -> "Not Started"
                                ch.completionPercentage >= 100 -> "Completed"
                                else -> "In Progress"
                            },
                            "conceptsDone" to ch.conceptsDone,
                            "ncertReadingDone" to ch.ncertReadingDone,
                            "ncertQuestionsDone" to ch.ncertQuestionsDone,
                            "pyqDone" to ch.pyqDone,
                            "revisionDone" to ch.revisionDone,
                            "testDone" to ch.testDone,
                            "lastRevisedTimestamp" to ch.lastRevisedTimestamp,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        batch.set(chDocRef, chDocData, SetOptions.merge())
                    }
                    val progressRef = userRef.collection("syllabus_progress").document("current")
                    batch.set(progressRef, hashMapOf("stages" to progressMap, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                }

                batch.commit()
                    .addOnSuccessListener {
                        continuation.resume(Result.success("All changes synced"))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun restoreFullUserData(uid: String): Result<RestoredUserData> =
        runLoggedRequest("restoreFullUserData(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    val userRef = firestoreInstance.collection("users").document(uid)
                    userRef.get().addOnSuccessListener { userDoc ->
                        val profileData = userDoc.data?.toMutableMap() ?: mutableMapOf()

                        userRef.collection("study_plan").document("current").get()
                            .addOnCompleteListener { planTask ->
                                if (planTask.isSuccessful && planTask.result?.exists() == true) {
                                    val planData = planTask.result?.data
                                    if (planData != null) {
                                        if (!profileData.containsKey("studyPlanConfigured")) {
                                            planData["studyPlanConfigured"]?.let { profileData["studyPlanConfigured"] = it }
                                        }
                                        if (!profileData.containsKey("studyLevel")) {
                                            planData["studyLevel"]?.let { profileData["studyLevel"] = it }
                                        }
                                        if (!profileData.containsKey("studyPlanSummary")) {
                                            planData["studyPlanSummary"]?.let { profileData["studyPlanSummary"] = it }
                                        }
                                    }
                                }

                                userRef.collection("notes").limit(50).get()
                                    .addOnSuccessListener { notesSnapshot ->
                                        val notesList = notesSnapshot.documents.mapNotNull { doc ->
                                            try {
                                                val id = (doc.get("id") as? Long) ?: (doc.id.toLongOrNull() ?: System.currentTimeMillis())
                                                val subject = (doc.getString("subject")) ?: "Physics"
                                                val chapter = (doc.getString("chapter")) ?: ""
                                                val title = (doc.getString("title")) ?: "Note"
                                                val content = (doc.getString("content")) ?: ""
                                                val noteType = (doc.getString("noteType")) ?: "Personal"
                                                NoteEntity(
                                                    id = id,
                                                    subject = subject,
                                                    chapter = chapter,
                                                    title = title,
                                                    content = content,
                                                    noteType = noteType
                                                )
                                            } catch (e: Exception) { null }
                                        }

                                        userRef.collection("study_targets").limit(50).get()
                                            .addOnSuccessListener { targetsSnapshot ->
                                                val targetsList = targetsSnapshot.documents.mapNotNull { doc ->
                                                    try {
                                                        val id = (doc.get("id") as? Long) ?: (doc.id.toLongOrNull() ?: 0L)
                                                        val subject = (doc.getString("subject")) ?: "Physics"
                                                        val chapter = (doc.getString("chapter")) ?: ""
                                                        val task = (doc.getString("task")) ?: ""
                                                        val priority = (doc.getString("priority")) ?: "Medium"
                                                        val estMins = (doc.get("estimatedMinutes") as? Long)?.toInt() ?: 45
                                                        val deadline = (doc.getString("deadlineDate")) ?: "Today"
                                                        val status = (doc.getString("status")) ?: "Pending"
                                                        val isBacklog = (doc.getBoolean("isBacklog")) ?: false
                                                        val isWeekly = (doc.getBoolean("isWeeklyGoal")) ?: false
                                                        StudyTargetEntity(
                                                            id = id,
                                                            subject = subject,
                                                            chapter = chapter,
                                                            task = task,
                                                            priority = priority,
                                                            estimatedMinutes = estMins,
                                                            deadlineDate = deadline,
                                                            status = status,
                                                            isBacklog = isBacklog,
                                                            isWeeklyGoal = isWeekly
                                                        )
                                                    } catch (e: Exception) { null }
                                                }

                                                userRef.collection("syllabus_progress").document("current").get()
                                                    .addOnSuccessListener { progressDoc ->
                                                        val rawStages = progressDoc.get("stages") as? Map<String, Any>
                                                        val progressMap = mutableMapOf<String, Map<String, Boolean>>()
                                                        rawStages?.forEach { (chapterId, stagesObj) ->
                                                            val stagesMap = stagesObj as? Map<String, Any>
                                                            if (stagesMap != null) {
                                                                val boolMap = mutableMapOf<String, Boolean>()
                                                                stagesMap.forEach { (k, v) ->
                                                                    if (v is Boolean) boolMap[k] = v
                                                                }
                                                                progressMap[chapterId] = boolMap
                                                            }
                                                        }

                                                        continuation.resume(
                                                            Result.success(
                                                                RestoredUserData(
                                                                    profileData = profileData,
                                                                    notes = notesList,
                                                                    studyTargets = targetsList,
                                                                    testAttempts = emptyList(),
                                                                    syllabusProgress = progressMap
                                                                )
                                                            )
                                                        )
                                                    }
                                                    .addOnFailureListener {
                                                        continuation.resume(
                                                            Result.success(
                                                                RestoredUserData(
                                                                    profileData = profileData,
                                                                    notes = notesList,
                                                                    studyTargets = targetsList,
                                                                    testAttempts = emptyList(),
                                                                    syllabusProgress = emptyMap()
                                                                )
                                                            )
                                                        )
                                                    }
                                            }
                                            .addOnFailureListener {
                                                continuation.resume(
                                                    Result.success(
                                                        RestoredUserData(
                                                            profileData = profileData,
                                                            notes = notesList,
                                                            studyTargets = emptyList(),
                                                            testAttempts = emptyList(),
                                                            syllabusProgress = emptyMap()
                                                        )
                                                    )
                                                )
                                            }
                                    }
                                    .addOnFailureListener {
                                        continuation.resume(
                                            Result.success(
                                                RestoredUserData(
                                                    profileData = profileData,
                                                    notes = emptyList(),
                                                    studyTargets = emptyList(),
                                                    testAttempts = emptyList(),
                                                    syllabusProgress = emptyMap()
                                                )
                                            )
                                        )
                                    }
                            }
                    }.addOnFailureListener { e ->
                        continuation.resume(Result.failure(e))
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Checks whether the user is a verified administrator by inspecting
     * Firestore collection `system_admins/{uid}`.
     * Returns true ONLY if:
     * 1. The document exists in system_admins/{uid}.
     * 2. Its `role` field equals "admin".
     */
    suspend fun verifyAdminStatus(uid: String): Result<Boolean> {
        val trimmedUid = uid.trim()
        if (trimmedUid.isBlank()) {
            return Result.success(false)
        }
        return runLoggedRequest("verifyAdminStatus(uid=$trimmedUid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("system_admins").document(trimmedUid)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                val role = documentSnapshot.getString("role")?.trim()
                                val isAdmin = (role == "admin")
                                continuation.resume(Result.success(isAdmin))
                            } else {
                                continuation.resume(Result.success(false))
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "verifyAdminStatus failed for $trimmedUid: ${exception.message}")
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    /**
     * Saves study material metadata to Firestore collection `study_materials/{resourceId}`.
     * Uses server timestamps for createdAt and updatedAt.
     */
    suspend fun saveStudyMaterial(material: StudyMaterialResource): Result<String> =
        runLoggedRequest("saveStudyMaterial(id=${material.resourceId})") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    val docRef = if (material.resourceId.isNotBlank()) {
                        firestoreInstance.collection("study_materials").document(material.resourceId)
                    } else {
                        firestoreInstance.collection("study_materials").document()
                    }
                    val autoId = docRef.id
                    val data = hashMapOf<String, Any?>(
                        "resourceId" to autoId,
                        "title" to material.title.trim(),
                        "description" to material.description.trim(),
                        "classLevel" to material.classLevel,
                        "board" to material.board,
                        "stream" to material.stream,
                        "subject" to material.subject,
                        "chapterId" to material.chapterId,
                        "chapterName" to material.chapterName,
                        "materialType" to material.materialType,
                        "difficultyLevel" to material.difficultyLevel,
                        "pdfUrl" to material.pdfUrl.trim(),
                        "googleDriveFileId" to material.googleDriveFileId,
                        "youtubeVideoId" to material.youtubeVideoId,
                        "published" to material.published,
                        "createdBy" to material.createdBy,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    docRef.set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Study material saved successfully with ID: $autoId")
                            continuation.resume(Result.success(autoId))
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "saveStudyMaterial failed: ${exception.message}", exception)
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "saveStudyMaterial unexpected error: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches all study materials for the administrator view.
     * Maps document fields safely into [StudyMaterialResource].
     */
    suspend fun fetchStudyMaterialsForAdmin(): Result<List<StudyMaterialResource>> =
        runLoggedRequest("fetchStudyMaterialsForAdmin") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("study_materials")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val id = doc.getString("resourceId") ?: doc.id
                                    val title = doc.getString("title") ?: ""
                                    val description = doc.getString("description") ?: ""
                                    val classLevel = doc.getString("classLevel") ?: "12"
                                    val board = doc.getString("board") ?: "CBSE"
                                    val stream = doc.getString("stream") ?: "PCM"
                                    val subject = doc.getString("subject") ?: "Physics"
                                    val chapterId = doc.getString("chapterId") ?: ""
                                    val chapterName = doc.getString("chapterName") ?: ""
                                    val materialType = doc.getString("materialType") ?: "Important Questions"
                                    val difficultyLevel = doc.getString("difficultyLevel") ?: "Moderate"
                                    val pdfUrl = doc.getString("pdfUrl") ?: ""
                                    val googleDriveFileId = doc.getString("googleDriveFileId")
                                    val youtubeVideoId = doc.getString("youtubeVideoId")
                                    val published = doc.getBoolean("published") ?: false
                                    val createdBy = doc.getString("createdBy") ?: ""
                                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()

                                    StudyMaterialResource(
                                        resourceId = id,
                                        title = title,
                                        description = description,
                                        classLevel = classLevel,
                                        board = board,
                                        stream = stream,
                                        subject = subject,
                                        chapterId = chapterId,
                                        chapterName = chapterName,
                                        materialType = materialType,
                                        difficultyLevel = difficultyLevel,
                                        pdfUrl = pdfUrl,
                                        googleDriveFileId = googleDriveFileId,
                                        youtubeVideoId = youtubeVideoId,
                                        published = published,
                                        createdBy = createdBy,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to parse study material doc ${doc.id}: ${e.message}")
                                    null
                                }
                            }.sortedByDescending { it.createdAt }
                            continuation.resume(Result.success(list))
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "fetchStudyMaterialsForAdmin failed: ${exception.message}", exception)
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "fetchStudyMaterialsForAdmin unexpected error: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Updates the publish status of a study material in collection `study_materials/{resourceId}`.
     * Only succeeds if the authenticated user has verified administrator privileges in firestore.rules.
     */
    suspend fun updateStudyMaterialPublishStatus(resourceId: String, published: Boolean): Result<Unit> =
        runLoggedRequest("updateStudyMaterialPublishStatus(id=$resourceId, published=$published)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("study_materials")
                        .document(resourceId)
                        .update(
                            mapOf(
                                "published" to published,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "Study material $resourceId published status updated to: $published")
                            continuation.resume(Result.success(Unit))
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to update publish status for $resourceId: ${exception.message}", exception)
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "updateStudyMaterialPublishStatus unexpected error: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches all published study materials for student views (Practice and StudyDock).
     * Only retrieves documents where `published == true`.
     */
    suspend fun fetchPublishedStudyMaterials(): Result<List<StudyMaterialResource>> =
        runLoggedRequest("fetchPublishedStudyMaterials") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("study_materials")
                        .whereEqualTo("published", true)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val id = doc.getString("resourceId") ?: doc.id
                                    val title = doc.getString("title") ?: ""
                                    val description = doc.getString("description") ?: ""
                                    val classLevel = doc.getString("classLevel") ?: "12"
                                    val board = doc.getString("board") ?: "CBSE"
                                    val stream = doc.getString("stream") ?: "PCM"
                                    val subject = doc.getString("subject") ?: "Physics"
                                    val chapterId = doc.getString("chapterId") ?: ""
                                    val chapterName = doc.getString("chapterName") ?: ""
                                    val materialType = doc.getString("materialType") ?: "Important Questions"
                                    val difficultyLevel = doc.getString("difficultyLevel") ?: "Moderate"
                                    val pdfUrl = doc.getString("pdfUrl") ?: ""
                                    val googleDriveFileId = doc.getString("googleDriveFileId")
                                    val youtubeVideoId = doc.getString("youtubeVideoId")
                                    val published = doc.getBoolean("published") ?: false
                                    val createdBy = doc.getString("createdBy") ?: ""
                                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()

                                    StudyMaterialResource(
                                        resourceId = id,
                                        title = title,
                                        description = description,
                                        classLevel = classLevel,
                                        board = board,
                                        stream = stream,
                                        subject = subject,
                                        chapterId = chapterId,
                                        chapterName = chapterName,
                                        materialType = materialType,
                                        difficultyLevel = difficultyLevel,
                                        pdfUrl = pdfUrl,
                                        googleDriveFileId = googleDriveFileId,
                                        youtubeVideoId = youtubeVideoId,
                                        published = published,
                                        createdBy = createdBy,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to parse published study material doc ${doc.id}: ${e.message}")
                                    null
                                }
                            }.sortedByDescending { it.createdAt }
                            continuation.resume(Result.success(list))
                        }
                        .addOnFailureListener { exception ->
                            if (exception.message?.contains("PERMISSION_DENIED") == true) {
                                Log.w(TAG, "fetchPublishedStudyMaterials permission denied; returning empty list.")
                                continuation.resume(Result.success(emptyList()))
                            } else {
                                Log.e(TAG, "fetchPublishedStudyMaterials failed: ${exception.message}", exception)
                                continuation.resume(Result.failure(exception))
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "fetchPublishedStudyMaterials unexpected error: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Saves exclusive music video metadata and source separately to Firestore.
     * Metadata -> exclusive_music_videos/{videoId} (publicly readable catalog)
     * Source -> exclusive_music_video_sources/{videoId} (administrator only)
     */
    suspend fun saveExclusiveMusicVideo(
        video: ExclusiveMusicVideo,
        videoUrl: String,
        googleDriveFileId: String
    ): Result<String> = runLoggedRequest("saveExclusiveMusicVideo(id=${video.videoId})") { firestoreInstance ->
        suspendCancellableCoroutine { continuation ->
            try {
                val batch = firestoreInstance.batch()
                val catalogRef = if (video.videoId.isNotBlank()) {
                    firestoreInstance.collection("exclusive_music_videos").document(video.videoId)
                } else {
                    firestoreInstance.collection("exclusive_music_videos").document()
                }
                val autoId = catalogRef.id
                val sourceRef = firestoreInstance.collection("exclusive_music_video_sources").document(autoId)

                val catalogData = hashMapOf<String, Any?>(
                    "videoId" to autoId,
                    "title" to video.title.trim(),
                    "subject" to video.subject,
                    "chapterId" to video.chapterId,
                    "chapterName" to video.chapterName,
                    "description" to video.description.trim(),
                    "thumbnailUrl" to video.thumbnailUrl.trim(),
                    "coinPrice" to video.coinPrice,
                    "durationSeconds" to video.durationSeconds,
                    "published" to video.published,
                    "createdBy" to video.createdBy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                if (video.videoId.isBlank()) {
                    catalogData["createdAt"] = FieldValue.serverTimestamp()
                }

                val sourceData = hashMapOf<String, Any?>(
                    "videoId" to autoId,
                    "videoUrl" to videoUrl.trim(),
                    "googleDriveFileId" to googleDriveFileId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                batch.set(catalogRef, catalogData, SetOptions.merge())
                batch.set(sourceRef, sourceData, SetOptions.merge())

                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Exclusive music video saved successfully with ID: $autoId")
                        continuation.resume(Result.success(autoId))
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "saveExclusiveMusicVideo failed: ${exception.message}", exception)
                        continuation.resume(Result.failure(exception))
                    }
            } catch (e: Exception) {
                Log.e(TAG, "saveExclusiveMusicVideo unexpected error: ${e.message}", e)
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Fetches all exclusive music videos for the administrator view.
     */
    suspend fun fetchExclusiveMusicVideosForAdmin(): Result<List<ExclusiveMusicVideo>> =
        runLoggedRequest("fetchExclusiveMusicVideosForAdmin") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("exclusive_music_videos")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(ExclusiveMusicVideo::class.java)?.copy(videoId = doc.id)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to parse music video doc ${doc.id}: ${e.message}")
                                    null
                                }
                            }.sortedByDescending { it.createdAt }
                            continuation.resume(Result.success(list))
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "fetchExclusiveMusicVideosForAdmin failed: ${exception.message}", exception)
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "fetchExclusiveMusicVideosForAdmin unexpected error: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Updates the publish status of a music video.
     */
    suspend fun updateMusicVideoPublishStatus(videoId: String, published: Boolean): Result<Unit> =
        runLoggedRequest("updateMusicVideoPublishStatus(id=$videoId, published=$published)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("exclusive_music_videos")
                        .document(videoId)
                        .update(
                            mapOf(
                                "published" to published,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .addOnSuccessListener {
                            continuation.resume(Result.success(Unit))
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Deletes a music video record (catalog and source metadata).
     */
    suspend fun deleteExclusiveMusicVideo(videoId: String): Result<Unit> =
        runLoggedRequest("deleteExclusiveMusicVideo(id=$videoId)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    val batch = firestoreInstance.batch()
                    batch.delete(firestoreInstance.collection("exclusive_music_videos").document(videoId))
                    batch.delete(firestoreInstance.collection("exclusive_music_video_sources").document(videoId))
                    batch.commit()
                        .addOnSuccessListener {
                            continuation.resume(Result.success(Unit))
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches the video source for a specific video.
     * Admin only access.
     */
    suspend fun fetchMusicVideoSource(videoId: String): Result<ExclusiveMusicVideoSource> =
        runLoggedRequest("fetchMusicVideoSource(id=$videoId)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("exclusive_music_video_sources")
                        .document(videoId)
                        .get()
                        .addOnSuccessListener { doc ->
                            val source = doc.toObject(ExclusiveMusicVideoSource::class.java)
                            if (source != null) {
                                continuation.resume(Result.success(source))
                            } else {
                                continuation.resume(Result.failure(Exception("Video source not found")))
                            }
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches published music videos for student catalog.
     */
    suspend fun fetchPublishedExclusiveMusicVideos(): Result<List<ExclusiveMusicVideo>> =
        runLoggedRequest("fetchPublishedExclusiveMusicVideos") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("exclusive_music_videos")
                        .whereEqualTo("published", true)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(ExclusiveMusicVideo::class.java)?.copy(videoId = doc.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }.sortedByDescending { it.createdAt }
                            continuation.resume(Result.success(list))
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Fetches all music videos unlocked by the user.
     */
    suspend fun fetchSongUnlocks(uid: String): Result<List<String>> =
        runLoggedRequest("fetchSongUnlocks(uid=$uid)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                try {
                    firestoreInstance.collection("users")
                        .document(uid)
                        .collection("song_unlocks")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val unlockedIds = snapshot.documents.map { it.id }
                            continuation.resume(Result.success(unlockedIds))
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Unlocks a music video using Study Points.
     * Uses a Firestore transaction for atomicity.
     */
    suspend fun unlockMusicVideo(uid: String, videoId: String, price: Int): Result<Boolean> =
        runLoggedRequest("unlockMusicVideo(uid=$uid, videoId=$videoId)") { firestoreInstance ->
            suspendCancellableCoroutine { continuation ->
                val walletRef = firestoreInstance.collection("users").document(uid)
                    .collection("study_points").document("current")
                val videoRef = firestoreInstance.collection("exclusive_music_videos").document(videoId)
                val unlockRef = firestoreInstance.collection("users").document(uid)
                    .collection("song_unlocks").document(videoId)
                val transactionId = "SPEND_UNLOCK_${videoId}"
                val transRef = firestoreInstance.collection("users").document(uid)
                    .collection("study_point_transactions").document(transactionId)

                firestoreInstance.runTransaction { transaction ->
                    // 1. Check if already unlocked
                    val existingUnlock = transaction.get(unlockRef)
                    if (existingUnlock.exists()) {
                        return@runTransaction false
                    }

                    // 2. Read video to verify price and published status
                    val videoSnap = transaction.get(videoRef)
                    if (!videoSnap.exists() || !(videoSnap.getBoolean("published") ?: false)) {
                        throw Exception("Video not found or not published")
                    }
                    val actualPrice = videoSnap.getLong("coinPrice")?.toInt() ?: price

                    // 3. Read wallet and check balance
                    val walletSnap = transaction.get(walletRef)
                    val wallet = walletSnap.toObject(StudyPointWallet::class.java) ?: StudyPointWallet()
                    
                    if (wallet.balance < actualPrice) {
                        throw Exception("Insufficient Study Points")
                    }

                    // 4. Update wallet
                    val updatedWallet = hashMapOf<String, Any>(
                        "balance" to wallet.balance - actualPrice,
                        "totalSpent" to wallet.totalSpent + actualPrice,
                        "totalEarned" to wallet.totalEarned, // Required by rules
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastTransactionId" to transactionId,
                        "lastSourceType" to "UNLOCK_SONG",
                        "lastUnlockVideoId" to videoId
                    )
                    transaction.set(walletRef, updatedWallet, SetOptions.merge())

                    // 5. Create unlock record
                    val unlockData = hashMapOf<String, Any>(
                        "videoId" to videoId,
                        "unlockedAt" to FieldValue.serverTimestamp(),
                        "pointsSpent" to actualPrice,
                        "transactionId" to transactionId
                    )
                    transaction.set(unlockRef, unlockData)

                    // 6. Create spending transaction
                    val pointTrans = hashMapOf<String, Any>(
                        "transactionId" to transactionId,
                        "type" to "SPEND",
                        "amount" to -actualPrice,
                        "sourceType" to "UNLOCK_SONG",
                        "sourceId" to videoId,
                        "description" to "Unlocked: ${videoSnap.getString("title")}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    transaction.set(transRef, pointTrans)

                    true
                }.addOnSuccessListener { result ->
                    continuation.resume(Result.success(result))
                }.addOnFailureListener { e ->
                    android.util.Log.e("RankifyFirestore", "unlockMusicVideo FAILED: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
            }
        }
}
