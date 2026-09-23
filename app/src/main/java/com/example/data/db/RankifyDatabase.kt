package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudentProfile::class,
        ExamEntity::class,
        SyllabusChapterEntity::class,
        StudyTargetEntity::class,
        StudySessionEntity::class,
        QuestionEntity::class,
        MistakeEntity::class,
        TestAttemptEntity::class,
        NoteEntity::class,
        AIChatMessageEntity::class,
        NotificationHistoryEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class RankifyDatabase : RoomDatabase() {
    abstract fun rankifyDao(): RankifyDao

    companion object {
        @Volatile
        private var INSTANCE: RankifyDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS study_groups")
                db.execSQL("DROP TABLE IF EXISTS group_messages")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE student_profile ADD COLUMN studyPlanConfigured INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE student_profile ADD COLUMN studyLevel TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE student_profile ADD COLUMN studyPlanSummary TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notification_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `notificationId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `personality` TEXT NOT NULL,
                        `deepLink` TEXT NOT NULL,
                        `createdTime` INTEGER NOT NULL,
                        `opened` INTEGER NOT NULL,
                        `dismissed` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): RankifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RankifyDatabase::class.java,
                    "rankify_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_4_5, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    populateInitialData(database.rankifyDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(dao: RankifyDao) {
            dao.saveProfile(DefaultData.defaultProfile)
            dao.insertInitialChapters(DefaultData.initialChapters)
            dao.insertInitialQuestions(DefaultData.initialQuestions)
            DefaultData.initialTargets.forEach { dao.insertTarget(it) }
            DefaultData.initialExams.forEach { dao.insertExam(it) }
            DefaultData.initialNotes.forEach { dao.insertNote(it) }
        }
    }
}
