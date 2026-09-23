package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.pdf.GoogleDrivePdfDownloader
import com.example.ui.pdf.PdfRenderEngine
import com.example.ui.theme.RankifyPrimary
import kotlinx.coroutines.launch
import java.io.File

/**
 * Parameters identifying the study material to render in the native PDF Reader.
 */
data class PdfReaderParams(
    val resourceId: String = "",
    val title: String = "",
    val pdfUrl: String = "",
    val googleDriveFileId: String? = null,
    val subject: String = "",
    val chapter: String = ""
)

/**
 * Common, reusable native Android PDF Reader Screen for Rankify.
 *
 * Used identically across:
 * - Practice Question Bank Chapter folders
 * - StudyDock Lecture Hub Resources & Related Chapter Resources
 *
 * Features:
 * 1. Downloads and validates Google Drive PDF into private app cache.
 * 2. Native rendering using [PdfRenderEngine] with [android.graphics.pdf.PdfRenderer].
 * 3. Vertical smooth page scrolling with [LazyColumn].
 * 4. Pinch-to-zoom and pan gestures with double-tap zoom toggle.
 * 5. Dynamic page indicator ("Page X of Y") in top bar.
 * 6. Authentic A4 paper proportions with white reading canvas.
 * 7. Clean, friendly error handling (private file, network loss, invalid signature) with Retry and Browser escape.
 * 8. Automatic resource release upon leaving screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankifyPdfReaderScreen(
    params: PdfReaderParams,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Intercept hardware and gesture back
    BackHandler(onBack = onClose)

    // Download and Rendering state
    var isLoading by remember { mutableStateOf(true) }
    var loadingMessage by remember { mutableStateOf("Opening study material...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var renderEngine by remember { mutableStateOf<PdfRenderEngine?>(null) }

    // Function to load or reload PDF
    fun loadPdf(forceRefresh: Boolean = false) {
        isLoading = true
        errorMessage = null
        loadingMessage = if (forceRefresh) "Refreshing from Google Drive..." else "Loading document..."

        coroutineScope.launch {
            // Close previous engine if any
            renderEngine?.close()
            renderEngine = null

            val downloadResult = GoogleDrivePdfDownloader.downloadOrGetCachedPdf(
                context = context,
                resourceId = params.resourceId,
                googleDriveFileId = params.googleDriveFileId,
                pdfUrl = params.pdfUrl,
                forceRefresh = forceRefresh
            )

            downloadResult.fold(
                onSuccess = { pdfFile ->
                    loadingMessage = "Rendering pages..."
                    val engineResult = PdfRenderEngine.open(pdfFile)
                    engineResult.fold(
                        onSuccess = { engine ->
                            renderEngine = engine
                            isLoading = false
                        },
                        onFailure = { error ->
                            isLoading = false
                            errorMessage = "Failed to render PDF: ${error.message ?: "Unsupported document format"}"
                        }
                    )
                },
                onFailure = { error ->
                    isLoading = false
                    errorMessage = error.message ?: "Unable to download PDF from Google Drive."
                }
            )
        }
    }

    // Initial load
    LaunchedEffect(params.pdfUrl, params.googleDriveFileId, params.resourceId) {
        loadPdf(forceRefresh = false)
    }

    // Clean up PdfRenderEngine when navigating away
    DisposableEffect(Unit) {
        onDispose {
            renderEngine?.close()
        }
    }

    val totalPages = renderEngine?.pageCount ?: 0
    val listState = rememberLazyListState()
    val firstVisibleIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    val currentPage = if (totalPages > 0) (firstVisibleIndex + 1).coerceIn(1, totalPages) else 1

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("rankify_pdf_reader_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = params.title.ifBlank { "Study Material" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = listOfNotNull(
                            params.subject.takeIf { it.isNotBlank() },
                            params.chapter.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")

                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("pdf_reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Page Indicator: Page X of Y
                    if (totalPages > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RankifyPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("pdf_page_indicator")
                        ) {
                            Text(
                                text = "Page $currentPage of $totalPages",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = RankifyPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Reload Action
                    IconButton(
                        onClick = { loadPdf(forceRefresh = true) },
                        modifier = Modifier.testTag("pdf_reader_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload PDF"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE2E8F0)) // Neutral reading backdrop
        ) {
            when {
                // 1. Loading State
                isLoading -> {
                    PdfLoadingView(message = loadingMessage)
                }

                // 2. Error State
                errorMessage != null -> {
                    PdfErrorView(
                        errorMessage = errorMessage ?: "Unknown error",
                        pdfUrl = params.pdfUrl,
                        onRetry = { loadPdf(forceRefresh = true) },
                        onClose = onClose
                    )
                }

                // 3. Document Render View
                renderEngine != null && totalPages > 0 -> {
                    PdfDocumentView(
                        engine = renderEngine!!,
                        pageCount = totalPages,
                        listState = listState
                    )
                }

                // 4. Empty Document Fallback
                else -> {
                    PdfErrorView(
                        errorMessage = "The document contains no readable pages.",
                        pdfUrl = params.pdfUrl,
                        onRetry = { loadPdf(forceRefresh = true) },
                        onClose = onClose
                    )
                }
            }
        }
    }
}

/**
 * Centered progress view while downloading or rendering the PDF.
 */
@Composable
private fun PdfLoadingView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_reader_loading"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = RankifyPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading PDF",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Friendly error presentation with detailed context and Retry/Browser fallback.
 */
@Composable
private fun PdfErrorView(
    errorMessage: String,
    pdfUrl: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_reader_error_view"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unable to Open PDF",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Retry Button
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RankifyPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("pdf_error_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Open in Browser Fallback (Informing student explicitly)
                    if (pdfUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    Toast.makeText(
                                        context,
                                        "Opening document in external browser...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Could not launch browser: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("pdf_error_open_browser_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Browser",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("pdf_error_back_button")
                ) {
                    Text("Return to Previous Screen")
                }
            }
        }
    }
}

/**
 * Clean native PDF reading canvas with vertical scrolling, pinch-to-zoom, and responsive layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfDocumentView(
    engine: PdfRenderEngine,
    pageCount: Int,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    // Zoom and pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.roundToPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                        if (zoomScale > 1.05f) {
                            // Bound panning based on zoom level
                            val maxPanX = (zoomScale - 1f) * 600f
                            val maxPanY = (zoomScale - 1f) * 1200f
                            panOffset = Offset(
                                x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                                y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                            )
                        } else {
                            panOffset = Offset.Zero
                        }
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        translationX = panOffset.x,
                        translationY = panOffset.y
                    )
                    .testTag("pdf_page_list")
            ) {
                items(pageCount, key = { it }) { pageIndex ->
                    PdfPageCard(
                        engine = engine,
                        pageIndex = pageIndex,
                        targetWidthPx = viewportWidthPx,
                        onDoubleTapZoom = {
                            if (zoomScale > 1.2f) {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            } else {
                                zoomScale = 2f
                            }
                        }
                    )
                }
            }

            // Floating Zoom Indicator & Controls (appears when zoomed in)
            AnimatedVisibility(
                visible = zoomScale > 1.05f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${(zoomScale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("pdf_zoom_reset")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = RankifyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual PDF Page rendered with accurate A4 aspect ratio on a pure white surface.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfPageCard(
    engine: PdfRenderEngine,
    pageIndex: Int,
    targetWidthPx: Int,
    onDoubleTapZoom: () -> Unit
) {
    var pageBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember(pageIndex) { mutableStateOf(true) }
    var pageAspectRatio by remember(pageIndex) { mutableFloatStateOf(1f / 1.4142f) } // Standard A4 (1:√2)

    LaunchedEffect(pageIndex, targetWidthPx) {
        isRendering = true
        val dims = engine.getPageDimensions(pageIndex)
        if (dims.first > 0 && dims.second > 0) {
            pageAspectRatio = dims.first.toFloat() / dims.second.toFloat()
        }
        val bmp = engine.renderPage(pageIndex, targetWidthPx)
        pageBitmap = bmp
        isRendering = false
    }

    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(pageAspectRatio)
            .shadow(4.dp, RoundedCornerShape(6.dp), clip = false)
            .combinedClickable(
                onClick = {},
                onDoubleClick = onDoubleTapZoom
            )
            .testTag("pdf_page_$pageIndex")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = pageBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isRendering) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = RankifyPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rendering Page ${pageIndex + 1}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            } else {
                Text(
                    text = "Page ${pageIndex + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}
