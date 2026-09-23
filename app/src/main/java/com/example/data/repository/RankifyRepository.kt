package com.example.data.repository

import com.example.data.db.RankifyDao
import com.example.data.model.AIChatMessageEntity
import com.example.data.model.ExamEntity
import com.example.data.model.MistakeEntity
import com.example.data.model.NoteEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.StudentProfile
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity
import com.example.data.model.TestAttemptEntity
import kotlinx.coroutines.flow.Flow

class RankifyRepository(private val dao: RankifyDao) {

    val profile: Flow<StudentProfile?> = dao.getProfile()
    val activeExam: Flow<ExamEntity?> = dao.getActiveExam()
    val allExams: Flow<List<ExamEntity>> = dao.getAllExams()
    val allChapters: Flow<List<SyllabusChapterEntity>> = dao.getAllChapters()
    val dailyTargets: Flow<List<StudyTargetEntity>> = dao.getDailyTargets()
    val backlogTargets: Flow<List<StudyTargetEntity>> = dao.getBacklogTargets()
    val weeklyGoals: Flow<List<StudyTargetEntity>> = dao.getWeeklyGoals()
    val studySessions: Flow<List<StudySessionEntity>> = dao.getAllSessions()
    val allQuestions: Flow<List<QuestionEntity>> = dao.getAllQuestions()
    val bookmarkedQuestions: Flow<List<QuestionEntity>> = dao.getBookmarkedQuestions()
    val allMistakes: Flow<List<MistakeEntity>> = dao.getAllMistakes()
    val testAttempts: Flow<List<TestAttemptEntity>> = dao.getAllTestAttempts()
    val studentNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val chatMessages: Flow<List<AIChatMessageEntity>> = dao.getAllChatMessages()

    fun getChaptersBySubject(subject: String): Flow<List<SyllabusChapterEntity>> =
        dao.getChaptersBySubject(subject)

    suspend fun updateProfile(profile: StudentProfile) {
        dao.saveProfile(profile)
    }

    suspend fun getProfileDirect(): StudentProfile? {
        return dao.getProfileDirect()
    }

    suspend fun saveExam(exam: ExamEntity) {
        dao.insertExam(exam)
    }

    suspend fun deleteExam(id: Long) {
        dao.deleteExam(id)
    }

    suspend fun updateChapter(chapter: SyllabusChapterEntity) {
        dao.updateChapter(chapter)
    }

    suspend fun ensureChaptersSeeded(defaultChapters: List<SyllabusChapterEntity>) {
        val count = dao.getChapterCount()
        if (count < defaultChapters.size) {
            dao.insertInitialChapters(defaultChapters)
        }
    }

    suspend fun addTarget(target: StudyTargetEntity) {
        dao.insertTarget(target)
    }

    suspend fun updateTarget(target: StudyTargetEntity) {
        dao.updateTarget(target)
    }

    suspend fun deleteTarget(id: Long) {
        dao.deleteTarget(id)
    }

    suspend fun recordStudySession(
        subject: String,
        chapter: String,
        durationMinutes: Int,
        difficultyRating: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        dao.insertSession(
            StudySessionEntity(
                subject = subject,
                chapter = chapter,
                durationMinutes = durationMinutes,
                difficultyRating = difficultyRating,
                notes = notes,
                timestamp = timestamp
            )
        )
    }

    suspend fun toggleQuestionBookmark(question: QuestionEntity) {
        dao.updateQuestion(question.copy(isBookmarked = !question.isBookmarked))
    }

    suspend fun recordMistake(
        questionId: Long,
        subject: String,
        chapter: String,
        topic: String,
        summary: String,
        category: String,
        notes: String = ""
    ) {
        dao.insertMistake(
            MistakeEntity(
                questionId = questionId,
                subject = subject,
                chapter = chapter,
                topic = topic,
                questionSummary = summary,
                mistakeCategory = category,
                studentNotes = notes
            )
        )
    }

    suspend fun recordTestAttempt(attempt: TestAttemptEntity) {
        dao.insertTestAttempt(attempt)
    }

    suspend fun addNote(note: NoteEntity) {
        dao.insertNote(note)
    }

    suspend fun deleteNote(id: Long) {
        dao.deleteNote(id)
    }

    suspend fun getAllTargetsDirect(): List<StudyTargetEntity> = dao.getAllTargetsDirect()

    suspend fun insertTargets(targets: List<StudyTargetEntity>) = dao.insertTargets(targets)

    suspend fun getAllChaptersDirect(): List<SyllabusChapterEntity> = dao.getChaptersDirect()

    suspend fun insertOrUpdateChapters(chapters: List<SyllabusChapterEntity>) = dao.insertOrUpdateChapters(chapters)

    suspend fun getNotesDirect(): List<NoteEntity> = dao.getNotesDirect()

    suspend fun insertNotes(notes: List<NoteEntity>) = dao.insertNotes(notes)

    suspend fun getTestAttemptsDirect(): List<TestAttemptEntity> = dao.getTestAttemptsDirect()
    suspend fun insertTestAttempts(attempts: List<TestAttemptEntity>) = dao.insertTestAttempts(attempts)

    suspend fun saveChatMessage(message: AIChatMessageEntity) {
        dao.insertChatMessage(message)
    }

    suspend fun clearChatHistory() {
        dao.clearChatHistory()
    }
}
