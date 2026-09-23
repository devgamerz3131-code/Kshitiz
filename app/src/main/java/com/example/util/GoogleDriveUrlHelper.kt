package com.example.util

import java.net.URI

object GoogleDriveUrlHelper {

    private val FILE_ID_REGEX = Regex(
        """(?:/file/d/|/d/|[?&]id=)([a-zA-Z0-9_-]{20,})""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Validates whether the given string is a supported Google Drive HTTPS sharing URL.
     * Returns a [Result] containing the extracted Google Drive file ID if valid,
     * or a descriptive failure error.
     */
    fun validateAndExtractFileId(url: String): Result<String> {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("PDF Link cannot be empty."))
        }

        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("Google Drive link must use secure HTTPS (e.g. https://drive.google.com/...).")
            )
        }

        val host = try {
            val uri = URI(trimmed)
            uri.host?.lowercase() ?: ""
        } catch (_: Exception) {
            return Result.failure(
                IllegalArgumentException("Invalid URL structure.")
            )
        }

        val isGoogleDriveHost = host == "drive.google.com" ||
                host == "docs.google.com" ||
                host.endsWith(".drive.google.com") ||
                host.endsWith(".docs.google.com")

        if (!isGoogleDriveHost) {
            return Result.failure(
                IllegalArgumentException("Only Google Drive sharing links (drive.google.com) are supported.")
            )
        }

        val match = FILE_ID_REGEX.find(trimmed)
        val fileId = match?.groupValues?.getOrNull(1)

        if (fileId.isNullOrBlank()) {
            return Result.failure(
                IllegalArgumentException("Could not extract a valid Google Drive file ID from the link. Please provide a standard file-sharing link (e.g. https://drive.google.com/file/d/...).")
            )
        }

        return Result.success(fileId)
    }

    /**
     * Converts a file ID to a direct streaming/download URL for Google Drive.
     */
    fun getDirectStreamingUrl(fileId: String): String {
        return "https://drive.google.com/uc?id=$fileId&export=download"
    }

    /**
     * Extracts YouTube Video ID if present (optional field).
     * Returns null if blank, or the 11-character video ID if found/valid.
     */
    fun extractYouTubeVideoId(input: String?): String? {
        val trimmed = input?.trim() ?: return null
        if (trimmed.isBlank()) return null

        val regex = Regex("""(?:youtu\.be/|youtube\.com/(?:watch\?v=|embed/|v/))([a-zA-Z0-9_-]{11})""")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.groupValues[1]
        }

        // If user entered just the 11-char ID directly
        if (trimmed.length == 11 && trimmed.matches(Regex("""[a-zA-Z0-9_-]{11}"""))) {
            return trimmed
        }

        return trimmed
    }
}
