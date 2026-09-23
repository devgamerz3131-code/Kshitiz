package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AIChatMessageEntity
import com.example.data.model.ExamEntity
import com.example.data.model.MistakeEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NotificationHistoryEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.StudentProfile
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity
import com.example.data.model.TestAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RankifyDao {

    // Student Profile
    @Query("SELECT * FROM student_profile WHERE id = 1")
    fun getProfile(): Flow<StudentProfile?>

    @Query("SELECT * FROM student_profile WHERE id = 1")
    suspend fun getProfileDirect(): StudentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: StudentProfile)

    // Exams
    @Query("SELECT * FROM exams ORDER BY id DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY id DESC")
    suspend fun getAllExamsDirect(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE isActive = 1 LIMIT 1")
    fun getActiveExam(): Flow<ExamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: Long)

    // Syllabus Chapters
    @Query("SELECT * FROM syllabus_chapters ORDER BY subject ASC, chapterNumber ASC")
    fun getAllChapters(): Flow<List<SyllabusChapterEntity>>

    @Query("SELECT COUNT(*) FROM syllabus_chapters")
    suspend fun getChapterCount(): Int

    @Query("SELECT * FROM syllabus_chapters WHERE subject = :subject ORDER BY chapterNumber ASC")
    fun getChaptersBySubject(subject: String): Flow<List<SyllabusChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialChapters(chapters: List<SyllabusChapterEntity>)

    @Query("SELECT * FROM syllabus_chapters ORDER BY subject ASC, chapterNumber ASC")
    suspend fun getChaptersDirect(): List<SyllabusChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChapters(chapters: List<SyllabusChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: SyllabusChapterEntity)

    // Targets & Backlog
    @Query("SELECT * FROM study_targets WHERE isBacklog = 0 AND isWeeklyGoal = 0 ORDER BY priority DESC, id DESC")
    fun getDailyTargets(): Flow<List<StudyTargetEntity>>

    @Query("SELECT * FROM study_targets WHERE isBacklog = 1 ORDER BY priority DESC, id DESC")
    fun getBacklogTargets(): Flow<List<StudyTargetEntity>>

    @Query("SELECT * FROM study_targets WHERE isWeeklyGoal = 1 ORDER BY id DESC")
    fun getWeeklyGoals(): Flow<List<StudyTargetEntity>>

    @Query("SELECT * FROM study_targets ORDER BY id ASC")
    suspend fun getAllTargetsDirect(): List<StudyTargetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: StudyTargetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<StudyTargetEntity>)

    @Update
    suspend fun updateTarget(target: StudyTargetEntity)

    @Query("DELETE FROM study_targets WHERE id = :id")
    suspend fun deleteTarget(id: Long)

    // Study Sessions
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    suspend fun getSessionsSince(timestamp: Long): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    // Practice Questions
    @Query("SELECT * FROM questions ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE subject = :subject ORDER BY id ASC")
    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE isBookmarked = 1 ORDER BY id DESC")
    fun getBookmarkedQuestions(): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    // Mistakes
    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeEntity): Long

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: Long)

    // Test Attempts
    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    fun getAllTestAttempts(): Flow<List<TestAttemptEntity>>

    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    suspend fun getTestAttemptsDirect(): List<TestAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestAttempt(attempt: TestAttemptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestAttempts(attempts: List<TestAttemptEntity>)

    // Notes
    @Query("SELECT * FROM student_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM student_notes ORDER BY timestamp DESC")
    suspend fun getNotesDirect(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM student_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // AI Chat Messages
    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<AIChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: AIChatMessageEntity): Long

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearChatHistory()

    // Notification History
    @Query("SELECT * FROM notification_history ORDER BY createdTime DESC")
    fun getAllNotificationHistory(): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT * FROM notification_history ORDER BY createdTime DESC LIMIT :limit")
    suspend fun getRecentNotificationHistoryDirect(limit: Int = 20): List<NotificationHistoryEntity>

    @Query("SELECT * FROM notification_history WHERE createdTime >= :timestamp ORDER BY createdTime DESC")
    suspend fun getNotificationsSince(timestamp: Long): List<NotificationHistoryEntity>

    @Query("SELECT COUNT(*) FROM notification_history WHERE createdTime >= :startOfDayTimestamp")
    suspend fun getTodayNotificationsCount(startOfDayTimestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationHistory(entity: NotificationHistoryEntity): Long

    @Query("UPDATE notification_history SET opened = 1 WHERE notificationId = :notificationId")
    suspend fun updateNotificationOpened(notificationId: String)

    @Query("UPDATE notification_history SET dismissed = 1 WHERE notificationId = :notificationId")
    suspend fun updateNotificationDismissed(notificationId: String)

    @Query("SELECT * FROM notification_history WHERE isSynced = 0 LIMIT 50")
    suspend fun getUnsyncedNotifications(): List<NotificationHistoryEntity>

    @Query("UPDATE notification_history SET isSynced = 1 WHERE notificationId = :notificationId")
    suspend fun markNotificationSynced(notificationId: String)

    @Query("DELETE FROM notification_history")
    suspend fun clearNotificationHistory()
}
