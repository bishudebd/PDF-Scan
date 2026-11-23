package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.ScannedDocumentEntity
import com.example.data.model.ExportImageFormat
import com.example.data.model.PdfQuality
import com.example.engine.FileShareHelper
import com.example.ui.components.MetadataPill
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCobalt
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import com.example.ui.viewmodel.ActivePdfTool
import com.example.ui.viewmodel.PdfToolsUiState
import com.example.ui.viewmodel.PdfToolsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    viewModel: PdfToolsViewModel,
    onImageToPdf: () -> Unit,
    onIdCardScanner: () -> Unit,
    onCameraOcr: () -> Unit,
    onOpenDocument: (Long) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Handle system back when inside a specific tool sub-panel
    BackHandler(enabled = state.activeTool != ActivePdfTool.HUB) {
        viewModel.selectTool(ActivePdfTool.HUB)
    }

    // SAF PDF importer for any tool
    val pdfImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importExternalPdfForCurrentTool(uri, uri.lastPathSegment)
        }
    }

    // Photo Picker for OCR tool
    val ocrImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.runOcrOnImageUri(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.testTag("pdf_tools_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(state.activeTool.title, style = MaterialTheme.typography.titleLarge)
                            if (state.activeTool == ActivePdfTool.HUB) {
                                Text(
                                    text = "Complete offline PDF toolkit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (state.activeTool != ActivePdfTool.HUB) {
                            IconButton(onClick = { viewModel.selectTool(ActivePdfTool.HUB) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to PDF tools")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (state.activeTool) {
                    ActivePdfTool.HUB -> ToolsHubGrid(
                        onSelectTool = { viewModel.selectTool(it) },
                        onImageToPdf = onImageToPdf,
                        onIdCardScanner = onIdCardScanner
                    )
                    ActivePdfTool.MERGE -> MergePdfPanel(
                        state = state,
                        documents = documents,
                        viewModel = viewModel,
                        onImportPdf = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
                        onMerged = onOpenDocument
                    )
                    ActivePdfTool.SPLIT -> SplitPdfPanel(
                        state = state,
                        documents = documents,
                        viewModel = viewModel,
                        onImportPdf = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
                        onSplitCreated = onOpenDocument
                    )
                    ActivePdfTool.COMPRESS -> CompressPdfPanel(
                        state = state,
                        documents = documents,
                        viewModel = viewModel,
                        onImportPdf = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
                        onOpenDocument = onOpenDocument
                    )
                    ActivePdfTool.PDF_TO_IMAGES -> PdfToImagesPanel(
                        state = state,
                        documents = documents,
                        viewModel = viewModel,
                        onImportPdf = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
                        onShareExported = { files, fmt ->
                            FileShareHelper.shareImages(context, files, fmt.mimeType)
                        }
                    )
                    ActivePdfTool.SIGN -> SignPdfToolPanel(
                        state = state,
                        documents = documents,
                        viewModel = viewModel,
                        onImportExternalPdf = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
                        onDocumentCreated = onOpenDocument
                    )
                    ActivePdfTool.OCR -> OcrScannerToolPanel(
                        state = state,
                        documents = documents,
                        viewModel = viewModel,
                        onCaptureWithCamera = onCameraOcr,
                        onPickImageForOcr = {
                            ocrImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        }

        ProcessingOverlay(visible = state.isProcessing, label = state.processingLabel)
    }
}

private data class ToolHubEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
private fun ToolsHubGrid(
    onSelectTool: (ActivePdfTool) -> Unit,
    onImageToPdf: () -> Unit,
    onIdCardScanner: () -> Unit
) {
    val items = listOf(
        ToolHubEntry("img_to_pdf", "Image to PDF", "Convert gallery photos into crisp multi-page PDF", Icons.Filled.PictureAsPdf, AccentIndigo, onImageToPdf),
        ToolHubEntry("pdf_to_img", "PDF to Images", "Render every PDF page into high-res JPG or PNG", Icons.Filled.Collections, AccentRose) { onSelectTool(ActivePdfTool.PDF_TO_IMAGES) },
        ToolHubEntry("merge", "Merge PDF", "Combine multiple PDF documents in custom order", Icons.Filled.CallMerge, AccentCobalt) { onSelectTool(ActivePdfTool.MERGE) },
        ToolHubEntry("split", "Split PDF", "Extract selected pages into a new PDF file", Icons.Filled.CallSplit, AccentAmber) { onSelectTool(ActivePdfTool.SPLIT) },
        ToolHubEntry("compress", "Compress PDF", "Reduce PDF file size while keeping text sharp", Icons.Filled.Compress, AccentEmerald) { onSelectTool(ActivePdfTool.COMPRESS) },
        ToolHubEntry("sign", "Sign PDF", "Draw & place your digital signature on any page", Icons.Filled.Draw, AccentViolet) { onSelectTool(ActivePdfTool.SIGN) },
        ToolHubEntry("ocr", "OCR Text Scanner", "Extract editable text using Google ML Kit", Icons.Filled.TextSnippet, AccentIndigo) { onSelectTool(ActivePdfTool.OCR) },
        ToolHubEntry("id_card", "ID Card Scanner", "Scan front & back of ID card onto one A4 page", Icons.Filled.Badge, AccentTeal, onIdCardScanner)
    )

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tools_hub_${tool.id}")
                    .clickable(onClick = tool.onClick),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tool.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = tool.title, tint = tool.accent, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MergePdfPanel(
    state: PdfToolsUiState,
    documents: List<ScannedDocumentEntity>,
    viewModel: PdfToolsViewModel,
    onImportPdf: () -> Unit,
    onMerged: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("merge_pdf_panel")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select 2 or more PDFs (${state.selectedMergeDocIds.size} selected)",
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedButton(onClick = onImportPdf, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import PDF")
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.mergeOutputTitle,
                onValueChange = { viewModel.updateMergeTitle(it) },
                label = { Text("Merged Output Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(documents, key = { it.id }) { doc ->
            val orderIndex = state.selectedMergeDocIds.indexOf(doc.id)
            val isSelected = orderIndex >= 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleMergeDoc(doc.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CobaltBlueSoft else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) CobaltBlue else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CobaltBlue else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("${orderIndex + 1}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${doc.pageCount} page(s) · ${FileShareHelper.formatFileSize(doc.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected && orderIndex > 0) {
                        IconButton(onClick = { viewModel.moveMergeDocUp(doc.id) }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", tint = CobaltBlue)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.executeMerge(onMerged) },
                enabled = state.selectedMergeDocIds.size >= 2 && !state.isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("execute_merge_button")
            ) {
                Icon(Icons.Filled.CallMerge, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Merge ${state.selectedMergeDocIds.size} PDFs")
            }
        }
    }
}

@Composable
private fun SplitPdfPanel(
    state: PdfToolsUiState,
    documents: List<ScannedDocumentEntity>,
    viewModel: PdfToolsViewModel,
    onImportPdf: () -> Unit,
    onSplitCreated: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.testTag("split_pdf_panel")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1. Choose PDF to Split", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onImportPdf, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import PDF")
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(documents, key = { it.id }) { doc ->
                    val selected = doc.id == state.selectedSplitDocId
                    Surface(
                        onClick = { viewModel.selectDocumentForSplit(doc.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) CobaltBlueSoft else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CobaltBlue else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.width(155.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(doc.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${doc.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (state.splitPageThumbnails.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "2. Tap Pages to Extract (${state.selectedSplitPageIndices.size}/${state.splitPageThumbnails.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedButton(onClick = { viewModel.selectAllSplitPages() }) {
                        Text("All")
                    }
                }
            }

            item {
                val rows = state.splitPageThumbnails.withIndex().toList().chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowPages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPages.forEach { (idx, path) ->
                                val picked = state.selectedSplitPageIndices.contains(idx)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (picked) 3.dp else 1.dp,
                                            color = if (picked) CobaltBlue else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.toggleSplitPage(idx) }
                                ) {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "Page ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = if (picked) CobaltBlue else Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = "P${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (picked) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(CobaltBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            repeat(3 - rowPages.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.splitOutputTitle,
                    onValueChange = { viewModel.updateSplitTitle(it) },
                    label = { Text("New Split PDF Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = { viewModel.executeSplit(onSplitCreated) },
                    enabled = state.selectedSplitPageIndices.isNotEmpty() && !state.isProcessing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("execute_split_button")
                ) {
                    Icon(Icons.Filled.CallSplit, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract ${state.selectedSplitPageIndices.size} Page(s) to PDF")
                }
            }
        }
    }
}

@Composable
private fun CompressPdfPanel(
    state: PdfToolsUiState,
    documents: List<ScannedDocumentEntity>,
    viewModel: PdfToolsViewModel,
    onImportPdf: () -> Unit,
    onOpenDocument: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.testTag("compress_pdf_panel")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1. Select Document", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onImportPdf, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import PDF")
                }
            }
        }

        items(documents, key = { it.id }) { doc ->
            val selected = doc.id == state.selectedCompressDocId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectDocumentForCompress(doc.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) CobaltBlueSoft else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CobaltBlue else MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${doc.pageCount} page(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MetadataPill(text = FileShareHelper.formatFileSize(doc.fileSizeBytes))
                }
            }
        }

        item {
            Text("2. Compression Preset", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PdfQuality.entries.forEach { quality ->
                    FilterChip(
                        selected = state.compressQuality == quality,
                        onClick = { viewModel.selectCompressQuality(quality) },
                        label = { Text(quality.label) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.executeCompress() },
                enabled = state.selectedCompressDocId != null && !state.isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("execute_compress_button")
            ) {
                Icon(Icons.Filled.Compress, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compress PDF Now")
            }
        }

        state.lastCompressResult?.let { res ->
            item {
                val savedPct = if (res.originalSizeBytes > 0) {
                    (((res.originalSizeBytes - res.compressedSizeBytes).toDouble() / res.originalSizeBytes) * 100.0)
                        .toInt().coerceAtLeast(0)
                } else 0
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Compression Complete", style = MaterialTheme.typography.titleMedium, color = AccentEmerald)
                        Text(
                            text = "${FileShareHelper.formatFileSize(res.originalSizeBytes)}  →  ${FileShareHelper.formatFileSize(res.compressedSizeBytes)} ($savedPct% smaller)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        state.lastCompressedDocId?.let { newId ->
                            Button(
                                onClick = { onOpenDocument(newId) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                            ) {
                                Text("Open Compressed PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfToImagesPanel(
    state: PdfToolsUiState,
    documents: List<ScannedDocumentEntity>,
    viewModel: PdfToolsViewModel,
    onImportPdf: () -> Unit,
    onShareExported: (List<File>, ExportImageFormat) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.testTag("pdf_to_images_panel")
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1. Choose PDF", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onImportPdf, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import PDF")
                }
            }
        }

        items(documents, key = { it.id }) { doc ->
            val selected = doc.id == state.selectedExportDocId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectDocumentForExport(doc.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) CobaltBlueSoft else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CobaltBlue else MaterialTheme.colorScheme.outline)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${doc.pageCount} page(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MetadataPill(text = FileShareHelper.formatFileSize(doc.fileSizeBytes))
                }
            }
        }

        item {
            Text("2. Image Output Format", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportImageFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = state.exportFormat == fmt,
                        onClick = { viewModel.selectExportFormat(fmt) },
                        label = { Text(fmt.label) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.executePdfToImages() },
                enabled = state.selectedExportDocId != null && !state.isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("execute_pdf_to_images_button")
            ) {
                Icon(Icons.Filled.Collections, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Convert to ${state.exportFormat.extension.uppercase()} Images")
            }
        }

        if (state.exportedImageFiles.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Exported ${state.exportedImageFiles.size} Images", style = MaterialTheme.typography.titleSmall)
                            Button(
                                onClick = { onShareExported(state.exportedImageFiles, state.exportFormat) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share All")
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.exportedImageFiles) { file ->
                                AsyncImage(
                                    model = file,
                                    contentDescription = file.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 86.dp, height = 112.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
