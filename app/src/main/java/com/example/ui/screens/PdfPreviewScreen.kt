package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ExportImageFormat
import com.example.engine.FileShareHelper
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.MetadataPill
import com.example.ui.components.MoveToFolderDialog
import com.example.ui.components.ProcessingOverlay
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.theme.CobaltBlue
import com.example.ui.viewmodel.PdfPreviewViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    viewModel: PdfPreviewViewModel,
    onBack: () -> Unit,
    onEditDocument: (Long) -> Unit,
    onSignDocument: (Long) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val docWithPages = state.documentWithPages
    val pages = docWithPages?.orderedPages.orEmpty()
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.testTag("pdf_preview_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = docWithPages?.document?.title ?: "Document Preview",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (docWithPages != null) {
                                Text(
                                    text = "${FileShareHelper.formatFileSize(docWithPages.document.fileSizeBytes)} · ${pages.size} page(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (docWithPages != null) {
                            IconButton(
                                onClick = {
                                    FileShareHelper.sharePdf(
                                        context,
                                        File(docWithPages.document.pdfPath),
                                        docWithPages.document.title
                                    )
                                },
                                modifier = Modifier.testTag("preview_share_pdf_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share PDF", tint = CobaltBlue)
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) },
                                        onClick = { menuExpanded = false; showRenameDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move to Folder") },
                                        leadingIcon = { Icon(Icons.Filled.DriveFileMove, null) },
                                        onClick = { menuExpanded = false; showMoveDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Re-Enhance / Filter") },
                                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                        onClick = {
                                            menuExpanded = false
                                            onEditDocument(docWithPages.document.id)
                                        }
                                    )
                                    if (pages.size > 1) {
                                        DropdownMenuItem(
                                            text = { Text("Remove Current Page") },
                                            leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) },
                                            onClick = {
                                                menuExpanded = false
                                                val currentPage = pages.getOrNull(pagerState.currentPage)
                                                if (currentPage != null) {
                                                    viewModel.deletePage(currentPage.id, onBack)
                                                }
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete Document", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { menuExpanded = false; showDeleteDialog = true }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                if (docWithPages != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PreviewBottomAction(
                                icon = Icons.Filled.Share,
                                label = "Share PDF",
                                testTag = "preview_action_share",
                                onClick = {
                                    FileShareHelper.sharePdf(
                                        context,
                                        File(docWithPages.document.pdfPath),
                                        docWithPages.document.title
                                    )
                                }
                            )
                            PreviewBottomAction(
                                icon = Icons.Filled.Collections,
                                label = "JPG / PNG",
                                testTag = "preview_action_export_img",
                                onClick = { showExportDialog = true }
                            )
                            PreviewBottomAction(
                                icon = Icons.Filled.TextSnippet,
                                label = "OCR Text",
                                testTag = "preview_action_ocr",
                                onClick = { viewModel.runOcrOnPage(pagerState.currentPage) }
                            )
                            PreviewBottomAction(
                                icon = Icons.Filled.Draw,
                                label = "Sign PDF",
                                testTag = "preview_action_sign",
                                onClick = { onSignDocument(docWithPages.document.id) }
                            )
                            PreviewBottomAction(
                                icon = Icons.Filled.Edit,
                                label = "Filters",
                                testTag = "preview_action_filters",
                                onClick = { onEditDocument(docWithPages.document.id) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (pages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isLoading) "Loading document…" else "Document not available",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MetadataPill(text = "Page ${pagerState.currentPage + 1} of ${pages.size}")
                    Spacer(modifier = Modifier.height(10.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { pageIdx ->
                        val page = pages[pageIdx]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                shadowElevation = 6.dp,
                                color = Color.White,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AsyncImage(
                                    model = File(page.imagePath),
                                    contentDescription = "Page ${pageIdx + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        ProcessingOverlay(visible = state.isBusy, label = state.busyMessage)
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Pages as Images") },
            text = {
                Text("Choose an image format to render and share all pages of this document.")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportAsImages(ExportImageFormat.PNG) { files, fmt ->
                                FileShareHelper.shareImages(context, files, fmt.mimeType)
                            }
                        }
                    ) { Text("Export PNG") }
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.exportAsImages(ExportImageFormat.JPG) { files, fmt ->
                                FileShareHelper.shareImages(context, files, fmt.mimeType)
                            }
                        }
                    ) { Text("Export JPG") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (state.showOcrSheet) {
        val text = state.extractedOcrText.orEmpty()
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissOcrSheet() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Extracted OCR Text",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            FileShareHelper.copyToClipboard(context, "OCR Text", text)
                            viewModel.dismissOcrSheet()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            FileShareHelper.shareText(context, text)
                            viewModel.dismissOcrSheet()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Text")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showRenameDialog && docWithPages != null) {
        RenameDocumentDialog(
            currentTitle = docWithPages.document.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                viewModel.renameDocument(newTitle)
                showRenameDialog = false
            }
        )
    }

    if (showMoveDialog && docWithPages != null) {
        MoveToFolderDialog(
            folders = folders,
            currentFolderId = docWithPages.document.folderId,
            onDismiss = { showMoveDialog = false },
            onSelectFolder = { folderId ->
                viewModel.moveToFolder(folderId)
                showMoveDialog = false
            }
        )
    }

    if (showDeleteDialog && docWithPages != null) {
        DeleteConfirmDialog(
            title = "Delete Document",
            message = "Permanently delete \"${docWithPages.document.title}\" from your device?",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteDocument(onBack)
            }
        )
    }
}

@Composable
private fun PreviewBottomAction(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = label, tint = CobaltBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
