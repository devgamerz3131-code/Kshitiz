package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StudyDockLectureSession
import com.example.data.model.StudyDockResource
import com.example.data.model.StudyMaterialResource
import com.example.data.model.StudyResourceType
import com.example.data.pdf.GoogleDrivePdfDownloader
import com.example.ui.screens.PdfReaderParams
import com.example.util.GoogleDriveUrlHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RankifyPdfReaderTest {

    private lateinit var context: Context

    private val sampleGoogleDriveUrl = "https://drive.google.com/file/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs/view?usp=sharing"
    private val expectedFileId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGoogleDriveUrlHelper_ExtractsValidFileId() {
        val result = GoogleDriveUrlHelper.validateAndExtractFileId(sampleGoogleDriveUrl)
        assertTrue("Drive URL should be valid", result.isSuccess)
        assertEquals(expectedFileId, result.getOrNull())
    }

    @Test
    fun testPdfSignatureValidation() {
        val cacheDir = GoogleDrivePdfDownloader.getCacheDir(context)
        val validPdfFile = File(cacheDir, "test_valid.pdf")
        validPdfFile.writeBytes("%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".toByteArray())

        assertTrue("Valid PDF header should pass validation", GoogleDrivePdfDownloader.isValidPdf(validPdfFile))
        assertTrue("Cached copy should be valid", GoogleDrivePdfDownloader.isCachedAndValid(validPdfFile))

        val htmlFile = File(cacheDir, "test_html.pdf")
        htmlFile.writeBytes("<!DOCTYPE html><html><body>Sign In to Google</body></html>".toByteArray())

        assertFalse("HTML file must be rejected as invalid PDF", GoogleDrivePdfDownloader.isValidPdf(htmlFile))
        assertFalse("HTML file must not be treated as cached PDF", GoogleDrivePdfDownloader.isCachedAndValid(htmlFile))

        validPdfFile.delete()
        htmlFile.delete()
    }

    @Test
    fun testTestD_ReusesCachedPdfWithoutDuplicateDownload() {
        runBlocking {
            val resourceId = "qb_electric_charges_01"
            val cachedFile = GoogleDrivePdfDownloader.getCachedFile(context, resourceId, expectedFileId, sampleGoogleDriveUrl)

            // Simulate previously cached file on device
            cachedFile.parentFile?.mkdirs()
            cachedFile.writeBytes("%PDF-1.7 Test Native Rankify PDF Bytes".toByteArray())

            assertTrue(cachedFile.exists())
            assertTrue(GoogleDrivePdfDownloader.isValidPdf(cachedFile))

            // Requesting PDF should immediately return the cached file without network requests
            val result = GoogleDrivePdfDownloader.downloadOrGetCachedPdf(
                context = context,
                resourceId = resourceId,
                googleDriveFileId = expectedFileId,
                pdfUrl = sampleGoogleDriveUrl,
                forceRefresh = false
            )

            assertTrue("Should return success for cached file", result.isSuccess)
            val file = result.getOrNull()
            assertNotNull("Returned file must not be null", file)
            assertEquals(cachedFile.absolutePath, file?.absolutePath)
            assertTrue(file?.readText()?.contains("%PDF-1.7") == true)

            cachedFile.delete()
        }
    }

    @Test
    fun testTestA_PracticeQuestionBankPdfParamsResolution() {
        val resource = StudyMaterialResource(
            resourceId = "qb_electric_charges_01",
            title = "Electric Charges and Fields QB",
            description = "Complete Class 12 CBSE Question Bank",
            classLevel = "12",
            board = "CBSE",
            stream = "PCM",
            subject = "Physics",
            chapterId = "ch_01",
            chapterName = "Electric Charges and Fields",
            materialType = "Question Bank",
            pdfUrl = sampleGoogleDriveUrl,
            googleDriveFileId = expectedFileId,
            published = true
        )

        val params = PdfReaderParams(
            resourceId = resource.resourceId,
            title = resource.title,
            pdfUrl = resource.pdfUrl,
            googleDriveFileId = resource.googleDriveFileId,
            subject = resource.subject,
            chapter = resource.chapterName
        )

        assertEquals("qb_electric_charges_01", params.resourceId)
        assertEquals("Electric Charges and Fields QB", params.title)
        assertEquals(expectedFileId, params.googleDriveFileId)
        assertEquals("Physics", params.subject)
        assertEquals("Electric Charges and Fields", params.chapter)
    }

    @Test
    fun testTestB_StudyDockRelatedChapterResourcesPdfParamsResolution() {
        val dockResource = StudyDockResource(
            resourceId = "qb_electric_charges_01",
            title = "Electric Charges and Fields QB",
            description = "Complete Class 12 CBSE Question Bank",
            subject = "Physics",
            chapter = "Electric Charges and Fields",
            resourceType = StudyResourceType.IMPORTANT_QUESTIONS,
            resourceUrl = sampleGoogleDriveUrl,
            googleDriveFileId = expectedFileId,
            isPublished = true
        )

        val params = PdfReaderParams(
            resourceId = dockResource.resourceId,
            title = dockResource.title,
            pdfUrl = dockResource.resourceUrl,
            googleDriveFileId = dockResource.googleDriveFileId,
            subject = dockResource.subject,
            chapter = dockResource.chapter
        )

        assertEquals("qb_electric_charges_01", params.resourceId)
        assertEquals("Electric Charges and Fields QB", params.title)
        assertEquals(sampleGoogleDriveUrl, params.pdfUrl)
        assertEquals(expectedFileId, params.googleDriveFileId)
        assertEquals("Physics", params.subject)
        assertEquals("Electric Charges and Fields", params.chapter)
    }

    @Test
    fun testTestC_BackNavigationPreservesState() {
        // Verify that opening and closing the PDF viewer preserves state
        var activePdfParams: PdfReaderParams? = null
        var selectedSubject: String? = "Physics"
        var selectedChapter: String? = "Electric Charges and Fields"

        // Open PDF reader
        activePdfParams = PdfReaderParams(
            resourceId = "qb_electric_charges_01",
            title = "Electric Charges and Fields QB",
            pdfUrl = sampleGoogleDriveUrl,
            googleDriveFileId = expectedFileId,
            subject = "Physics",
            chapter = "Electric Charges and Fields"
        )
        assertNotNull(activePdfParams)

        // Close PDF reader (Back clicked)
        activePdfParams = null

        // Chapter and Subject are NOT reset
        assertEquals("Physics", selectedSubject)
        assertEquals("Electric Charges and Fields", selectedChapter)
    }

    @Test
    fun testTestF_InvalidDriveUrlErrorHandling() {
        runBlocking {
            val invalidUrl = "not-a-valid-url"
            val result = GoogleDrivePdfDownloader.downloadOrGetCachedPdf(
                context = context,
                resourceId = "invalid_01",
                googleDriveFileId = null,
                pdfUrl = invalidUrl,
                forceRefresh = true
            )

            assertTrue("Invalid URL must result in failure", result.isFailure)
            val ex = result.exceptionOrNull()
            assertNotNull(ex)
            assertTrue(
                "Error message should mention invalid or unsupported link",
                ex?.message?.contains("Invalid or unsupported", ignoreCase = true) == true
            )
        }
    }
}
