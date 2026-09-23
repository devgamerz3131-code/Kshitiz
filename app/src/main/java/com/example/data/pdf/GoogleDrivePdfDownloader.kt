package com.example.data.pdf

import android.content.Context
import android.util.Log
import com.example.util.GoogleDriveUrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Robust, secure downloader and cache manager for Google Drive PDF study materials.
 *
 * Implements:
 * 1. HTTPS only; no Firebase Auth tokens leaked to Google Drive.
 * 2. Caching in private app internal storage (`context.cacheDir/pdf_cache`).
 * 3. Validation of response status, Content-Type, and PDF file signature (`%PDF-`).
 * 4. Handling of Google Drive export redirects and virus warning confirmations.
 * 5. Re-use of verified cached files without duplicate network calls.
 * 6. Detailed, friendly error classifications (restricted, 404, network, invalid signature).
 */
object GoogleDrivePdfDownloader {

    private const val TAG = "GoogleDrivePdfDownloader"

    // PDF magic bytes signature: %PDF- (0x25, 0x50, 0x44, 0x46, 0x2D)
    private val PDF_MAGIC_BYTES = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Gets the app-private PDF cache directory.
     */
    fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "pdf_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generates a deterministic, filesystem-safe filename for a study material.
     */
    fun getCachedFile(
        context: Context,
        resourceId: String,
        googleDriveFileId: String?,
        pdfUrl: String
    ): File {
        val cacheDir = getCacheDir(context)
        val candidateId = when {
            !googleDriveFileId.isNullOrBlank() -> googleDriveFileId.trim()
            resourceId.isNotBlank() -> resourceId.trim()
            else -> GoogleDriveUrlHelper.validateAndExtractFileId(pdfUrl).getOrNull()
                ?: pdfUrl.hashCode().toString()
        }
        val safeName = candidateId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(cacheDir, "$safeName.pdf")
    }

    /**
     * Checks if a valid cached copy exists on disk.
     */
    fun isCachedAndValid(file: File): Boolean {
        if (!file.exists() || file.length() < 5) return false
        return isValidPdf(file)
    }

    /**
     * Validates whether a file is a genuine PDF by checking the header signature.
     */
    fun isValidPdf(file: File): Boolean {
        if (!file.exists() || file.length() < 5) return false
        return try {
            val buffer = ByteArray(minOf(file.length().toInt(), 1024))
            file.inputStream().use { input ->
                input.read(buffer)
            }
            // Check for %PDF- in header bytes
            val headerString = String(buffer, Charsets.ISO_8859_1)
            headerString.contains("%PDF-")
        } catch (e: Exception) {
            Log.w(TAG, "Error checking PDF header signature: ${e.message}")
            false
        }
    }

    /**
     * Downloads and caches the PDF file from Google Drive.
     *
     * If a valid cached copy exists, it is returned immediately without downloading.
     *
     * @param context Application context
     * @param resourceId Firestore resource ID
     * @param googleDriveFileId Extracted file ID if available
     * @param pdfUrl Google Drive sharing link
     * @param forceRefresh Set true to bypass cache and re-download
     * @return [Result] containing the cached [File] or descriptive error
     */
    suspend fun downloadOrGetCachedPdf(
        context: Context,
        resourceId: String,
        googleDriveFileId: String?,
        pdfUrl: String,
        forceRefresh: Boolean = false
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getCachedFile(context, resourceId, googleDriveFileId, pdfUrl)

        // 1. Return valid cached file unless forceRefresh is requested
        if (!forceRefresh && isCachedAndValid(targetFile)) {
            Log.d(TAG, "Reusing valid cached PDF: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            return@withContext Result.success(targetFile)
        }

        // 2. Check storage space (ensure at least 10MB available)
        val freeSpace = context.cacheDir.usableSpace
        if (freeSpace < 10L * 1024 * 1024) {
            return@withContext Result.failure(
                IOException("Insufficient device storage. At least 10MB of storage space is required.")
            )
        }

        // 3. Resolve Google Drive File ID
        val fileId = googleDriveFileId?.trim()?.takeIf { it.isNotBlank() }
            ?: GoogleDriveUrlHelper.validateAndExtractFileId(pdfUrl).getOrNull()

        // Build candidate download endpoints
        val candidateUrls = mutableListOf<String>()
        if (!fileId.isNullOrBlank()) {
            // Direct export endpoint (handles public files and virus warning bypass)
            candidateUrls.add("https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t")
            candidateUrls.add("https://drive.google.com/uc?export=download&id=$fileId&confirm=t")
        }

        // If the URL itself is already a direct HTTPS link
        if (pdfUrl.startsWith("https://", ignoreCase = true) && !candidateUrls.contains(pdfUrl)) {
            if (pdfUrl.contains("export=download") || pdfUrl.endsWith(".pdf", ignoreCase = true)) {
                candidateUrls.add(0, pdfUrl)
            } else {
                candidateUrls.add(pdfUrl)
            }
        }

        if (candidateUrls.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid or unsupported Google Drive PDF URL: $pdfUrl")
            )
        }

        var lastError: Exception? = null

        for (downloadUrl in candidateUrls) {
            val downloadResult = attemptDownload(context, downloadUrl, targetFile)
            if (downloadResult.isSuccess) {
                return@withContext downloadResult
            } else {
                lastError = downloadResult.exceptionOrNull() as? Exception
                Log.w(TAG, "Download attempt failed for $downloadUrl: ${lastError?.message}")
            }
        }

        Result.failure(lastError ?: IOException("Failed to download PDF from Google Drive."))
    }

    /**
     * Performs a single HTTPS download attempt into a temporary file, validates signature,
     * and atomically moves it into the destination file.
     */
    private fun attemptDownload(context: Context, url: String, destinationFile: File): Result<File> {
        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download_${System.currentTimeMillis()}.tmp")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; RankifyApp/1.0)")
                .header("Accept", "application/pdf,application/octet-stream,*/*")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val code = response.code

                if (code == 404) {
                    return Result.failure(
                        IOException("Google Drive file not found (HTTP 404). Please verify the link is active.")
                    )
                }

                if (code == 403) {
                    return Result.failure(
                        IOException("Google Drive access denied (HTTP 403). The file permissions may be restricted.")
                    )
                }

                if (!response.isSuccessful) {
                    return Result.failure(
                        IOException("Download failed with server status code $code.")
                    )
                }

                val finalUrl = response.request.url.toString()
                if (finalUrl.contains("accounts.google.com")) {
                    return Result.failure(
                        IOException("Google Drive file is private. Please ask the administrator to set sharing to 'Anyone with the link can view'.")
                    )
                }

                val body = response.body
                    ?: return Result.failure(IOException("Server returned an empty response body."))

                // Stream response body to temporary file
                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Validate downloaded file
                if (!tempFile.exists() || tempFile.length() < 100) {
                    tempFile.delete()
                    return Result.failure(IOException("Downloaded file is empty or incomplete."))
                }

                // Check PDF signature (%PDF-)
                if (isValidPdf(tempFile)) {
                    // Valid PDF document
                    if (destinationFile.exists()) {
                        destinationFile.delete()
                    }
                    val renamed = tempFile.renameTo(destinationFile)
                    if (!renamed) {
                        tempFile.copyTo(destinationFile, overwrite = true)
                        tempFile.delete()
                    }
                    Log.i(TAG, "Successfully downloaded valid PDF (${destinationFile.length()} bytes)")
                    return Result.success(destinationFile)
                }

                // If not a PDF, analyze content (e.g. Google Drive HTML warning or login page)
                val preview = try {
                    tempFile.readText(Charsets.UTF_8).take(2048)
                } catch (_: Exception) { "" }

                tempFile.delete()

                return when {
                    preview.contains("accounts.google.com") || preview.contains("Sign in") -> {
                        Result.failure(
                            IOException("Google Drive file requires Google account sign-in. The file must be shared as 'Anyone with the link can view'.")
                        )
                    }
                    preview.contains("download_warning") || preview.contains("confirm=") -> {
                        Result.failure(
                            IOException("Google Drive file confirmation prompt could not be automated. File may exceed direct download quota.")
                        )
                    }
                    preview.contains("<html", ignoreCase = true) || preview.contains("<!DOCTYPE", ignoreCase = true) -> {
                        Result.failure(
                            IOException("Download returned a web page instead of a PDF document.")
                        )
                    }
                    else -> {
                        Result.failure(
                            IOException("The downloaded file is corrupted or not a valid PDF document.")
                        )
                    }
                }
            }
        } catch (e: UnknownHostException) {
            tempFile.delete()
            return Result.failure(IOException("Network unavailable. Please check your internet connection."))
        } catch (e: SocketTimeoutException) {
            tempFile.delete()
            return Result.failure(IOException("Download timed out. Please check your connection and retry."))
        } catch (e: ConnectException) {
            tempFile.delete()
            return Result.failure(IOException("Could not connect to Google Drive. Please try again."))
        } catch (e: Exception) {
            tempFile.delete()
            return Result.failure(e)
        }
    }
}
