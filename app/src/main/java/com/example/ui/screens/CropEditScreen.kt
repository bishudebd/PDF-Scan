package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.PdfScanApplication
import com.example.ui.components.InteractiveCropCanvas
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.DeepNavy
import com.example.ui.viewmodel.ScannerEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropEditScreen(
    viewModel: ScannerEditorViewModel,
    onBack: () -> Unit,
    onAddMorePages: () -> Unit,
    onRescanActivePage: () -> Unit,
    onProceedToEnhance: () -> Unit
) {
    val context = LocalContext.current
    val imageProcessor = (context.applicationContext as PdfScanApplication).container.imageProcessor
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activePage = state.activePage
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.infoMessage, state.errorMessage) {
        val msg = state.errorMessage ?: state.infoMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotices()
        }
    }

    // Load preview bitmap of current raw page with its rotation preview
    val previewBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = activePage?.rawImagePath,
        key2 = activePage?.rotationDegrees
    ) {
        value = withContext(Dispatchers.Default) {
            val page = activePage ?: return@withContext null
            val raw = imageProcessor.decodeSampledBitmapFromFile(page.rawImagePath, 1600)
                ?: return@withContext null
            if (page.rotationDegrees % 360 == 0) {
                raw
            } else {
                val m = Matrix().apply { postRotate(page.rotationDegrees.toFloat()) }
                val rot = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                if (rot != raw) raw.recycle()
                rot
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.testTag("crop_edit_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color(0xFF0A1220),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Crop & Perspective",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            if (state.pages.isNotEmpty()) {
                                Text(
                                    text = "Page ${state.activePageIndex + 1} of ${state.pages.size} · Drag corners to deskew",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (activePage != null) {
                            IconButton(
                                onClick = {
                                    viewModel.startRescanForPage(state.activePageIndex)
                                    onRescanActivePage()
                                },
                                modifier = Modifier.testTag("crop_rescan_page_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Rescan current page", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = onAddMorePages,
                            modifier = Modifier.testTag("crop_add_page_button")
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Add more pages", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
                )
            },
            bottomBar = {
                Surface(
                    color = DeepNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 10.dp, bottom = 14.dp)
                    ) {
                        if (state.pages.size > 1) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                itemsIndexed(state.pages, key = { _, p -> p.id }) { idx, page ->
                                    val selected = idx == state.activePageIndex
                                    Box(
                                        modifier = Modifier
                                            .size(width = 52.dp, height = 68.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) CobaltBlue else Color.White.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.selectPage(idx) }
                                    ) {
                                        AsyncImage(
                                            model = File(page.rawImagePath),
                                            contentDescription = "Page ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = if (selected) CobaltBlue else Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.68f))
                                                .clickable { viewModel.removePage(idx) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove page", tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons Row: Auto Crop | Full | Rot L | Rot R | Next
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CropActionButton(
                                icon = Icons.Filled.AutoFixHigh,
                                label = "Auto",
                                testTag = "crop_auto_detect_button",
                                onClick = { viewModel.autoDetectActivePageEdges() }
                            )
                            CropActionButton(
                                icon = Icons.Filled.CropFree,
                                label = "Full",
                                testTag = "crop_full_page_button",
                                onClick = { viewModel.resetActiveCropToFull() }
                            )
                            CropActionButton(
                                icon = Icons.AutoMirrored.Filled.RotateLeft,
                                label = "Left",
                                testTag = "crop_rotate_left_button",
                                onClick = { viewModel.rotateActivePage90(clockwise = false) }
                            )
                            CropActionButton(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                label = "Right",
                                testTag = "crop_rotate_button",
                                onClick = { viewModel.rotateActivePage90(clockwise = true) }
                            )
                            Button(
                                onClick = { viewModel.applyCropForAllPagesAndContinue(onProceedToEnhance) },
                                enabled = state.pages.isNotEmpty() && !state.isProcessing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("crop_next_button")
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp != null && activePage != null) {
                    InteractiveCropCanvas(
                        bitmap = bmp,
                        quad = activePage.cropQuad,
                        onQuadChange = { viewModel.updateActiveCropQuad(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = if (state.pages.isEmpty()) "No pages in session" else "Loading document preview…",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        ProcessingOverlay(visible = state.isProcessing, label = state.processingLabel)
    }

    state.lowStorageStatus?.let { low ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissLowStorageDialog() },
            icon = { Icon(Icons.Filled.WarningAmber, null, tint = AccentAmber) },
            title = { Text("Low Device Storage") },
            text = {
                Text("Only ${low.availableMb} MB free (${low.requiredMb} MB needed). Clear scan cache to continue.")
            },
            confirmButton = {
                Button(onClick = { viewModel.clearScanCacheAndRecheckStorage() }) { Text("Clear Cache") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLowStorageDialog() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CropActionButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
    }
}
