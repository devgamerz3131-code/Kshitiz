package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlParserTest {

    // TEST 1: Paste valid youtube.com/watch URL -> accepted
    @Test
    fun testValidYouTubeWatchUrl() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertTrue(YouTubeUrlParser.isValidYouTubeUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    // TEST 2: Paste valid youtu.be URL -> accepted
    @Test
    fun testValidYouTuBeUrl() {
        val url = "https://youtu.be/dQw4w9WgXcQ"
        assertTrue(YouTubeUrlParser.isValidYouTubeUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    // TEST 3: Paste YouTube live URL -> accepted
    @Test
    fun testValidYouTubeLiveUrl() {
        val url = "https://www.youtube.com/live/dQw4w9WgXcQ"
        assertTrue(YouTubeUrlParser.isValidYouTubeUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    // TEST 4: Paste valid URL with spaces before/after -> spaces trimmed -> accepted
    @Test
    fun testValidUrlWithSpacesTrimmed() {
        val url = "   https://www.youtube.com/watch?v=dQw4w9WgXcQ  \n  "
        assertTrue(YouTubeUrlParser.isValidYouTubeUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // TEST 5: Paste URL with ?si= -> accepted
    @Test
    fun testValidYouTuBeWithSiQuery() {
        val url = "https://youtu.be/dQw4w9WgXcQ?si=test12345"
        assertTrue(YouTubeUrlParser.isValidYouTubeUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    // TEST 6: Paste URL with &t= -> accepted
    @Test
    fun testValidWatchWithTimestamp() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=120"
        assertTrue(YouTubeUrlParser.isValidYouTubeUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    // TEST 7: Paste Google URL -> rejected
    @Test
    fun testGoogleUrlRejected() {
        val url = "https://google.com"
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl(url))
        assertNull(YouTubeUrlParser.parse(url))
    }

    // TEST 8: Paste random text -> rejected
    @Test
    fun testRandomTextRejected() {
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("hello"))
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("Current Electricity Lecture"))
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("https://example.com/watch?v=dQw4w9WgXcQ"))
    }

    // TEST 9: Paste youtube.com without video ID -> rejected
    @Test
    fun testYouTubeWithoutVideoIdRejected() {
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("youtube.com"))
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("https://youtube.com/"))
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("https://youtu.be/"))
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("https://youtube.com/watch?v="))
    }

    // TEST 10: Empty or blank input -> rejected
    @Test
    fun testEmptyInputRejected() {
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl(""))
        assertFalse(YouTubeUrlParser.isValidYouTubeUrl("   "))
        assertNull(YouTubeUrlParser.parse(""))
    }

    // Variations test: m.youtube.com and embed
    @Test
    fun testMobileYouTube() {
        val url = "https://m.youtube.com/watch?v=dQw4w9WgXcQ"
        assertTrue(YouTubeUrlParser.isValidYouTubeUrl(url))
        val parsed = YouTubeUrlParser.parse(url)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // Step 3 Android Share tests: extractFirstYouTubeUrl
    @Test
    fun testExtractSharedTextWithTitleAndUrl() {
        val sharedText = "Physics Chapter 3 Current Electricity One Shot\nhttps://youtu.be/dQw4w9WgXcQ?si=abcdef12345"
        val parsed = YouTubeUrlParser.extractFirstYouTubeUrl(sharedText)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed?.normalizedUrl)
    }

    @Test
    fun testExtractSharedWatchUrlWithSurroundingPunctuation() {
        val sharedText = "Check this out (https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=60s) for revision."
        val parsed = YouTubeUrlParser.extractFirstYouTubeUrl(sharedText)
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    @Test
    fun testExtractSharedTextWithoutYouTubeUrlReturnsNull() {
        val sharedText = "Hey check out this physics book notes summary from website"
        val parsed = YouTubeUrlParser.extractFirstYouTubeUrl(sharedText)
        assertNull(parsed)
    }

    @Test
    fun testExtractSharedTextWithNonYouTubeUrlReturnsNull() {
        val sharedText = "Read this article: https://example.com/physics-lecture"
        val parsed = YouTubeUrlParser.extractFirstYouTubeUrl(sharedText)
        assertNull(parsed)
    }
}
