package com.example.ui.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Native Android PdfRenderer wrapper with LRU bitmap caching and thread-safe operations.
 *
 * Responsibilities:
 * 1. Safe lifecycle management for [PdfRenderer] and [ParcelFileDescriptor].
 * 2. Strict thread synchronization using [Mutex] since [PdfRenderer] cannot open multiple pages concurrently.
 * 3. In-memory LRU cache to avoid re-rendering visible pages during scrolling.
 * 4. High-contrast white canvas backing for proper equation, diagram, and watermark rendering.
 * 5. Accurate page aspect ratios.
 */
class PdfRenderEngine private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer
) {

    companion object {
        private const val TAG = "PdfRenderEngine"

        /**
         * Opens a PDF file and constructs a [PdfRenderEngine] instance.
         */
        fun open(file: File): Result<PdfRenderEngine> {
            return try {
                if (!file.exists()) {
                    return Result.failure(IllegalArgumentException("PDF file does not exist: ${file.absolutePath}"))
                }
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                Result.success(PdfRenderEngine(pfd, renderer))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open PDF with native PdfRenderer: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private val mutex = Mutex()
    private var isClosed = false

    // Cache up to 12 page bitmaps in memory for smooth vertical scrolling
    private val bitmapCache = object : LruCache<Int, Bitmap>(12) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Note: We avoid manual recycle() here to prevent crashes if a recomposition is still drawing
        }
    }

    /**
     * Total number of pages in the PDF document.
     */
    val pageCount: Int
        get() = if (isClosed) 0 else renderer.pageCount

    /**
     * Retrieves the native dimensions (width and height in points) for a given page.
     * Default A4 ratio (~595 x 842 points) is returned if unavailable.
     */
    suspend fun getPageDimensions(pageIndex: Int): Pair<Int, Int> = withContext(Dispatchers.IO) {
        if (isClosed || pageIndex < 0 || pageIndex >= pageCount) {
            return@withContext Pair(595, 842)
        }

        mutex.withLock {
            if (isClosed) return@withContext Pair(595, 842)
            try {
                val page = renderer.openPage(pageIndex)
                val w = page.width
                val h = page.height
                page.close()
                Pair(w, h)
            } catch (e: Exception) {
                Log.w(TAG, "Error getting page $pageIndex dimensions: ${e.message}")
                Pair(595, 842)
            }
        }
    }

    /**
     * Renders a specific page into a high-quality ARGB_8888 [Bitmap].
     *
     * @param pageIndex 0-indexed page number
     * @param targetWidthPx Desired target width in pixels (e.g. screen width)
     */
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (isClosed || pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        // 1. Check in-memory LRU cache
        synchronized(bitmapCache) {
            val cached = bitmapCache.get(pageIndex)
            if (cached != null && !cached.isRecycled) {
                return@withContext cached
            }
        }

        // 2. Synchronized rendering with Mutex (PdfRenderer is not thread-safe)
        mutex.withLock {
            if (isClosed) return@withContext null

            try {
                val page = renderer.openPage(pageIndex)
                val pWidth = page.width
                val pHeight = page.height

                // Compute scale factor based on screen target width (clamped between 1.0f and 3.0f)
                val scale = if (pWidth > 0 && targetWidthPx > 0) {
                    (targetWidthPx.toFloat() / pWidth.toFloat()).coerceIn(1.0f, 3.0f)
                } else {
                    1.5f
                }

                val outWidth = (pWidth * scale).toInt().coerceAtLeast(100)
                val outHeight = (pHeight * scale).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                // Draw pure white background for crisp contrast with equations, text, and watermarks
                canvas.drawColor(Color.WHITE)

                // Render page content
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                synchronized(bitmapCache) {
                    bitmapCache.put(pageIndex, bitmap)
                }

                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Exception rendering page $pageIndex: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Closes the [PdfRenderer] and [ParcelFileDescriptor] and frees cached resources.
     */
    fun close() {
        if (isClosed) return
        isClosed = true

        try {
            renderer.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing PdfRenderer: ${e.message}")
        }

        try {
            pfd.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ParcelFileDescriptor: ${e.message}")
        }

        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }
    }
}
