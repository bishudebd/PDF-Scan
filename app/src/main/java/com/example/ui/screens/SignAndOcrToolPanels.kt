package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ScannedDocumentEntity
import com.example.engine.FileShareHelper
import com.example.ui.components.MetadataPill
import com.example.ui.components.SignaturePadDialog
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import com.example.ui.viewmodel.PdfToolsUiState
import com.example.ui.viewmodel.PdfToolsViewModel
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SignPdfToolPanel(
    state: PdfToolsUiState,
    documents: List<ScannedDocumentEntity>,
    viewModel: PdfToolsViewModel,
    onImportExternalPdf: () -> Unit,
    onDocumentCreated: (Long) -> Unit
) {
    var showSignaturePad by remember { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("sign_pdf_panel")
    ) {
        // Step 1: Signature Creation / Preview Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1. Your Digital Signature", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Draw your signature once and place it anywhere on a page.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showSignaturePad = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                            modifier = Modifier.testTag("open_signature_pad_button")
                        ) {
                            Icon(Icons.Filled.Draw, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.signaturePreviewPath.isBlank()) "Draw" else "Redraw")
                        }
                    }

                    if (state.signaturePreviewPath.isNotBlank() && File(state.signaturePreviewPath).exists()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = File(state.signaturePreviewPath),
                                contentDescription = "Saved digital signature",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Step 2: Select PDF Document
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("2. Select PDF Document", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onImportExternalPdf, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import PDF")
                }
            }
        }

        if (documents.isEmpty()) {
            item {
                Text(
                    text = "No saved PDFs yet. Scan a document or tap Import PDF above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(documents, key = { it.id }) { doc ->
                        val selected = doc.id == state.selectedSignDocId
                        Surface(
                            onClick = { viewModel.selectDocumentForSign(doc.id) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) CobaltBlueSoft else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CobaltBlue else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.width(150.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = File(doc.thumbnailPath),
                                    contentDescription = doc.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 46.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${doc.pageCount}p", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 3: Interactive Page Preview with Draggable Signature Placement
        val activePagePath = state.signDocPagePaths.getOrNull(state.signTargetPageIndex)
        if (activePagePath != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. Position Signature on Page", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Drag the signature box directly on the page preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (state.signDocPagePaths.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(state.signDocPagePaths) { idx, _ ->
                                    val isCurrent = idx == state.signTargetPageIndex
                                    Surface(
                                        onClick = { viewModel.selectSignPage(idx) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) CobaltBlue else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "Page ${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .onSizeChanged { previewSize = it }
                        ) {
                            AsyncImage(
                                model = File(activePagePath),
                                contentDescription = "PDF page to sign",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (state.signaturePreviewPath.isNotBlank() && File(state.signaturePreviewPath).exists() && previewSize.width > 0) {
                                val sigWpx = (previewSize.width * state.signWidthNorm).roundToInt()
                                val sigHpx = (sigWpx * 0.42f).roundToInt()
                                val offsetX = (previewSize.width * state.signPosX - sigWpx / 2f).roundToInt()
                                val offsetY = (previewSize.height * state.signPosY - sigHpx / 2f).roundToInt()

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX, offsetY) }
                                        .size(
                                            width = with(density) { sigWpx.toDp() },
                                            height = with(density) { sigHpx.toDp() }
                                        )
                                        .border(1.5.dp, CobaltBlue, RoundedCornerShape(6.dp))
                                        .background(CobaltBlue.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                        .pointerInput(previewSize, state.signPosX, state.signPosY, state.signWidthNorm) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                if (previewSize.width > 0 && previewSize.height > 0) {
                                                    val nextX = state.signPosX + dragAmount.x / previewSize.width
                                                    val nextY = state.signPosY + dragAmount.y / previewSize.height
                                                    viewModel.updateSignaturePlacement(nextX, nextY, state.signWidthNorm)
                                                }
                                            }
                                        }
                                ) {
                                    AsyncImage(
                                        model = File(state.signaturePreviewPath),
                                        contentDescription = "Draggable signature overlay",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Signature Size", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(12.dp))
                            Slider(
                                value = state.signWidthNorm,
                                onValueChange = { w ->
                                    viewModel.updateSignaturePlacement(state.signPosX, state.signPosY, w)
                                },
                                valueRange = 0.18f..0.65f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.executeSignPdf(onDocumentCreated) },
                            enabled = state.signaturePreviewPath.isNotBlank() && !state.isProcessing,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("apply_sign_pdf_button")
                        ) {
                            Icon(Icons.Filled.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Signed PDF")
                        }
                    }
                }
            }
        }
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSaveSignature = { bmp ->
                viewModel.saveDrawnSignatureBitmap(bmp)
                showSignaturePad = false
            }
        )
    }
}

@Composable
fun OcrScannerToolPanel(
    state: PdfToolsUiState,
    documents: List<ScannedDocumentEntity>,
    viewModel: PdfToolsViewModel,
    onCaptureWithCamera: () -> Unit,
    onPickImageForOcr: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_scanner_panel")
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("On-Device ML Kit OCR", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Scan printed documents, receipts, or photos into editable text completely offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCaptureWithCamera,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ocr_camera_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Text")
                        }
                        OutlinedButton(
                            onClick = onPickImageForOcr,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ocr_gallery_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From Gallery")
                        }
                    }
                }
            }
        }

        if (documents.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Or Extract From Saved Document", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(documents.take(10), key = { it.id }) { doc ->
                            Surface(
                                onClick = { viewModel.runOcrOnSavedDocument(doc.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.width(140.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = File(doc.thumbnailPath),
                                        contentDescription = doc.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 32.dp, height = 42.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.ocrSourceImagePath != null && File(state.ocrSourceImagePath).exists()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    AsyncImage(
                        model = File(state.ocrSourceImagePath),
                        contentDescription = "OCR scanned image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().background(Color.White)
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recognized Text", style = MaterialTheme.typography.titleMedium)
                        MetadataPill(text = "${state.ocrWordCount} words")
                    }

                    OutlinedTextField(
                        value = state.ocrExtractedText,
                        onValueChange = { viewModel.updateOcrEditedText(it) },
                        placeholder = { Text("Recognized text will appear here. You can edit, copy, or share it.") },
                        minLines = 7,
                        maxLines = 14,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ocr_result_textfield")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                FileShareHelper.copyToClipboard(context, "OCR Text", state.ocrExtractedText)
                            },
                            enabled = state.ocrExtractedText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                        Button(
                            onClick = {
                                FileShareHelper.shareText(context, state.ocrExtractedText)
                            },
                            enabled = state.ocrExtractedText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Text")
                        }
                    }
                }
            }
        }
    }
}
