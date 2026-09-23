package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StudyDockAnalysisStatus
import com.example.util.YouTubeUrlParser
import com.example.viewmodel.RankifyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyDockSessionTest {

    private lateinit var app: Application
    private lateinit var viewModel: RankifyViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = RankifyViewModel(app)
    }

    @Test
    fun test1_createLectureSessionWithSubjectAndChapter() {
        // TEST 1: Open StudyDock from Home, enter valid URL, select Physics and Current Electricity, tap Analyze Lecture
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull("URL must parse correctly", parsed)

        viewModel.setStudyDockSubject("Physics")
        viewModel.setStudyDockChapter("Current Electricity")

        val session = viewModel.createLectureSession(
            originalUrl = parsed!!.originalUrl,
            normalizedUrl = parsed.normalizedUrl,
            videoId = parsed.videoId,
            subject = viewModel.studyDockSelectedSubject.value,
            chapter = viewModel.studyDockSelectedChapter.value
        )

        assertNotNull(session)
        assertEquals("dQw4w9WgXcQ", session.videoId)
        assertEquals("Physics", session.selectedSubject)
        assertEquals("Current Electricity", session.selectedChapter)
        assertEquals(StudyDockAnalysisStatus.READY, session.analysisStatus)
        // Ensure lecture title, duration, AI detected values, and future study content are initially null/empty
        assertNull(session.lectureTitle)
        assertNull(session.thumbnailUrl)
        assertNull(session.durationSeconds)
        assertNull(session.detectedSubject)
        assertNull(session.detectedChapter)
        assertNull(session.futureContent)
    }

    @Test
    fun test2_createLectureSessionWithoutSubjectOrChapter() {
        // TEST 2: Open StudyDock without selecting any subject or chapter. Enter valid URL.
        val url = "https://youtu.be/kxy1234abcd"
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull(parsed)

        viewModel.setStudyDockSubject(null)
        viewModel.setStudyDockChapter(null)

        val session = viewModel.createLectureSession(
            originalUrl = parsed!!.originalUrl,
            normalizedUrl = parsed.normalizedUrl,
            videoId = parsed.videoId,
            subject = null,
            chapter = null
        )

        assertNotNull(session)
        assertEquals("kxy1234abcd", session.videoId)
        assertNull(session.selectedSubject)
        assertNull(session.selectedChapter)
        assertEquals(StudyDockAnalysisStatus.READY, session.analysisStatus)
    }

    @Test
    fun test3_subjectChangeClearsIncompatibleChapter() {
        // TEST 3: Select Physics -> Current Electricity. Change subject to Chemistry.
        viewModel.setStudyDockSubject("Physics")
        viewModel.setStudyDockChapter("Current Electricity")
        assertEquals("Current Electricity", viewModel.studyDockSelectedChapter.value)

        // Switch to Chemistry
        viewModel.setStudyDockSubject("Chemistry")
        assertEquals("Chemistry", viewModel.studyDockSelectedSubject.value)
        // Current Electricity is not a Chemistry chapter, so it must be cleared
        assertNull(viewModel.studyDockSelectedChapter.value)
    }

    @Test
    fun test4_sessionIdentityIsUniquePerSession() {
        // Verify session ID generation produces distinct IDs across separate sessions
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val parsed = YouTubeUrlParser.parse(url)!!

        val session1 = viewModel.createLectureSession(
            originalUrl = parsed.originalUrl,
            normalizedUrl = parsed.normalizedUrl,
            videoId = parsed.videoId,
            subject = "Physics",
            chapter = "Current Electricity"
        )

        val session2 = viewModel.createLectureSession(
            originalUrl = parsed.originalUrl,
            normalizedUrl = parsed.normalizedUrl,
            videoId = parsed.videoId,
            subject = "Physics",
            chapter = "Magnetism"
        )

        assertNotEquals("Distinct sessions must have distinct IDs", session1.sessionId, session2.sessionId)
    }

    @Test
    fun test5_sharingNewVideoClearsStaleActiveSession() {
        // TEST 5: Share a different YouTube video, ensuring stale video-specific session is cleared
        val initialUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val parsed = YouTubeUrlParser.parse(initialUrl)!!
        viewModel.createLectureSession(
            originalUrl = parsed.originalUrl,
            normalizedUrl = parsed.normalizedUrl,
            videoId = parsed.videoId,
            subject = "Physics",
            chapter = null
        )
        assertNotNull(viewModel.activeLectureSession.value)
        assertEquals("dQw4w9WgXcQ", viewModel.activeLectureSession.value?.videoId)

        // Share a second video (11 chars: 9bZkp7q19f0)
        viewModel.openStudyDockWithSharedContent("Check out https://youtu.be/9bZkp7q19f0")

        // Old session must be cleared because videoId changed
        assertNull("Old video active lecture session should be cleared", viewModel.activeLectureSession.value)
        assertEquals("https://youtu.be/9bZkp7q19f0", viewModel.studyDockInputUrl.value)
        assertTrue(viewModel.studyDockSharedBanner.value?.contains("✓") == true)
    }

    @Test
    fun test6_entertainmentVideoAcceptedAsLinkNotVerifiedLecture() {
        // TEST 6: Valid YouTube URL (e.g. music video / entertainment)
        val musicUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val parsed = YouTubeUrlParser.parse(musicUrl)
        assertNotNull(parsed)

        val session = viewModel.createLectureSession(
            originalUrl = parsed!!.originalUrl,
            normalizedUrl = parsed.normalizedUrl,
            videoId = parsed.videoId,
            subject = null,
            chapter = null
        )

        // Analysis status is READY (format accepted), NOT COMPLETED or ANALYZING
        assertEquals(StudyDockAnalysisStatus.READY, session.analysisStatus)
        // No fake detected subject or AI data
        assertNull(session.detectedSubject)
        assertNull(session.detectedChapter)
        assertNull(session.futureContent)
    }
}
