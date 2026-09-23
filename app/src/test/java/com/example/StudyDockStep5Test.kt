package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.StudyDockResourceService
import com.example.data.model.StudyDockLectureSession
import com.example.util.ExternalAiPromptHelper
import com.example.util.YouTubeUrlParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudyDockStep5Test {

    private val validYouTubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val expectedVideoId = "dQw4w9WgXcQ"

    @Test
    fun testYouTubeUrlParserExtraction() {
        val parsed = YouTubeUrlParser.parse(validYouTubeUrl)
        assertNotNull("Should parse valid YouTube URL", parsed)
        assertEquals(expectedVideoId, parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    @Test
    fun testBuildLecturePrompt_FullContext() {
        val prompt = ExternalAiPromptHelper.buildLecturePrompt(
            youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            subject = "Physics",
            chapter = "Current Electricity"
        )

        assertTrue("Prompt should specify Class 12 CBSE", prompt.contains("Class 12 CBSE student"))
        assertTrue("Prompt should contain the exact YouTube URL", prompt.contains("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue("Prompt should include Subject", prompt.contains("Subject: Physics"))
        assertTrue("Prompt should include Chapter", prompt.contains("Chapter: Current Electricity"))
        assertTrue("Prompt should request Hinglish summary", prompt.contains("Hinglish summary"))
        assertTrue("Prompt should request important formulas", prompt.contains("Important formulas"))
        assertTrue("Prompt should request 5 practice questions", prompt.contains("Five practice questions"))
        assertTrue("Prompt should caution against fake timestamps", prompt.contains("Do not invent"))

        // Critical privacy verification: ensure no personal fields are present
        assertFalse(prompt.contains("Email:"))
        assertFalse(prompt.contains("Marks:"))
        assertFalse(prompt.contains("Student name:"))
    }

    @Test
    fun testBuildLecturePrompt_UrlOnlyWithoutSubject() {
        val prompt = ExternalAiPromptHelper.buildLecturePrompt(
            youtubeUrl = "https://youtu.be/dQw4w9WgXcQ",
            subject = null,
            chapter = null
        )

        assertTrue(prompt.contains("https://youtu.be/dQw4w9WgXcQ"))
        assertFalse(prompt.contains("Subject:"))
        assertFalse(prompt.contains("Chapter:"))
        assertFalse(prompt.contains("null"))
    }

    @Test
    fun testClipboardCopy() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val success = ExternalAiPromptHelper.copyToClipboard(context, "Test Prompt Content", "TestLabel")
        assertTrue("Copy to clipboard should succeed", success)
    }

    @Test
    fun testLaunchChatGPT_WhenAppNotInstalled_FallsBackToClipboardAndWeb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prompt = "Test CBSE prompt with https://youtu.be/dQw4w9WgXcQ"
        val result = ExternalAiPromptHelper.launchChatGPT(context, prompt)

        assertEquals("ChatGPT", result.assistantName)
        assertTrue("Prompt should always be copied to clipboard", result.promptCopiedToClipboard)
        assertEquals("Prompt copied! Long-press the chat box and tap Paste.", result.messageToStudent)
        assertFalse("Direct sharing cannot succeed if app is not installed", result.directSharingAttempted)
    }

    @Test
    fun testLaunchGemini_WhenAppNotInstalled_FallsBackToClipboardAndWeb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prompt = "Test CBSE prompt with https://youtu.be/dQw4w9WgXcQ"
        val result = ExternalAiPromptHelper.launchGemini(context, prompt)

        assertEquals("Gemini", result.assistantName)
        assertTrue("Prompt should always be copied to clipboard", result.promptCopiedToClipboard)
        assertEquals("Prompt copied! Long-press the chat box and tap Paste.", result.messageToStudent)
        assertFalse("Direct sharing cannot succeed if app is not installed", result.directSharingAttempted)
    }

    @Test
    fun testCuratedResourceMatching_PhysicsCurrentElectricity() = runBlocking {
        val (exact, chapter) = StudyDockResourceService.getResourcesForLecture(
            videoId = expectedVideoId,
            subject = "Physics",
            chapter = "Current Electricity"
        )

        // Chapter resources should have curated CBSE notes, questions, and formula sheet
        assertTrue("Should return chapter level resources for Physics Current Electricity", chapter.isNotEmpty())
        assertTrue("Should contain revision notes", chapter.any { it.title.contains("Revision Notes") })
        assertTrue("Should contain formula sheet", chapter.any { it.title.contains("Formula Sheet") })
    }
}
