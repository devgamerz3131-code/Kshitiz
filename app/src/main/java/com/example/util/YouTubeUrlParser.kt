package com.example.util

import java.net.URI

/**
 * Validated YouTube Lecture URL representation.
 */
data class ParsedYouTubeUrl(
    val originalUrl: String,
    val normalizedUrl: String,
    val videoId: String
)

/**
 * YouTube URL Parser and Validator for StudyDock.
 *
 * Supports common YouTube URL formats:
 * - https://www.youtube.com/watch?v=VIDEO_ID
 * - https://youtube.com/watch?v=VIDEO_ID
 * - https://m.youtube.com/watch?v=VIDEO_ID
 * - https://youtu.be/VIDEO_ID
 * - https://www.youtube.com/live/VIDEO_ID
 * - https://youtube.com/live/VIDEO_ID
 *
 * Also handles query parameters such as ?si=..., &t=..., &list=..., etc.
 */
object YouTubeUrlParser {

    private val YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be"
    )

    // Standard YouTube Video ID: 11 characters containing [a-zA-Z0-9_-]
    private val VIDEO_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")

    /**
     * Trims leading/trailing whitespace and surrounding line breaks.
     */
    fun cleanRawInput(input: String): String {
        return input.trim().replace("\r", "").replace("\n", "").trim()
    }

    /**
     * Checks if the given raw input is a valid YouTube lecture URL.
     */
    fun isValidYouTubeUrl(rawUrl: String): Boolean {
        return parse(rawUrl) != null
    }

    /**
     * Extracts videoId if valid, otherwise returns null.
     */
    fun extractVideoId(rawUrl: String): String? {
        return parse(rawUrl)?.videoId
    }

    /**
     * Returns canonical normalized YouTube watch URL (e.g. https://www.youtube.com/watch?v=VIDEO_ID)
     * or null if invalid.
     */
    fun normalizeYouTubeUrl(rawUrl: String): String? {
        return parse(rawUrl)?.normalizedUrl
    }

    /**
     * Robustly parses and validates raw YouTube URL string.
     * Returns ParsedYouTubeUrl if valid, null otherwise.
     */
    fun parse(rawUrl: String): ParsedYouTubeUrl? {
        val cleaned = cleanRawInput(rawUrl)
        if (cleaned.isBlank()) return null

        // Add scheme if missing for URI parsing
        val urlWithScheme = if (!cleaned.startsWith("http://", ignoreCase = true) &&
            !cleaned.startsWith("https://", ignoreCase = true)
        ) {
            "https://$cleaned"
        } else {
            cleaned
        }

        val uri = try {
            URI(urlWithScheme)
        } catch (_: Exception) {
            return null
        }

        val rawHost = uri.host ?: return null
        val host = rawHost.lowercase().trim()

        if (host !in YOUTUBE_HOSTS) {
            return null
        }

        val path = uri.path ?: ""
        var extractedId: String? = null

        if (host == "youtu.be") {
            // Format: https://youtu.be/VIDEO_ID or /VIDEO_ID?si=...
            val segments = path.split('/').filter { it.isNotBlank() }
            if (segments.isNotEmpty()) {
                extractedId = segments[0]
            }
        } else {
            // youtube.com, www.youtube.com, m.youtube.com
            when {
                // watch?v=VIDEO_ID
                path.equals("/watch", ignoreCase = true) || path.startsWith("/watch/", ignoreCase = true) -> {
                    extractedId = extractQueryParam(uri.query, "v")
                }
                // live/VIDEO_ID
                path.startsWith("/live/", ignoreCase = true) -> {
                    val segments = path.split('/').filter { it.isNotBlank() }
                    // segments should be ["live", "VIDEO_ID"]
                    if (segments.size >= 2 && segments[0].equals("live", ignoreCase = true)) {
                        extractedId = segments[1]
                    }
                }
                // embed/VIDEO_ID
                path.startsWith("/embed/", ignoreCase = true) -> {
                    val segments = path.split('/').filter { it.isNotBlank() }
                    if (segments.size >= 2 && segments[0].equals("embed", ignoreCase = true)) {
                        extractedId = segments[1]
                    }
                }
                // shorts/VIDEO_ID
                path.startsWith("/shorts/", ignoreCase = true) -> {
                    val segments = path.split('/').filter { it.isNotBlank() }
                    if (segments.size >= 2 && segments[0].equals("shorts", ignoreCase = true)) {
                        extractedId = segments[1]
                    }
                }
            }
        }

        val finalId = extractedId?.trim() ?: return null

        // Validate video ID pattern (typically 11 characters)
        if (!VIDEO_ID_REGEX.matches(finalId)) {
            return null
        }

        val normalized = "https://www.youtube.com/watch?v=$finalId"

        return ParsedYouTubeUrl(
            originalUrl = cleaned,
            normalizedUrl = normalized,
            videoId = finalId
        )
    }

    /**
     * Extracts the first valid YouTube URL found in arbitrary text (e.g. shared from the YouTube app
     * like "Physics Chapter 3 Lecture\nhttps://youtu.be/dQw4w9WgXcQ?si=test") and returns its parsed representation.
     * Returns null if no valid YouTube URL is found in the text.
     */
    fun extractFirstYouTubeUrl(text: String?): ParsedYouTubeUrl? {
        if (text.isNullOrBlank()) return null

        // First attempt direct parsing in case the text is purely a URL (with or without surrounding whitespace)
        val directParse = parse(text)
        if (directParse != null) return directParse

        // Split text by whitespace, newlines, and common delimiters to inspect individual candidate tokens
        val tokens = text.split(Regex("[\\s\"'<>(),\\[\\]]+")).filter { it.isNotBlank() }
        for (token in tokens) {
            val candidate = parse(token)
            if (candidate != null) {
                return candidate
            }
        }
        return null
    }

    private fun extractQueryParam(query: String?, paramName: String): String? {
        if (query.isNullOrBlank()) return null
        val pairs = query.split('&')
        for (pair in pairs) {
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2 && parts[0].equals(paramName, ignoreCase = true)) {
                return parts[1]
            }
        }
        return null
    }
}
