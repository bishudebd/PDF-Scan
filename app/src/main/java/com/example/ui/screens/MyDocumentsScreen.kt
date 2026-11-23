package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FolderEntity
import com.example.data.local.ScannedDocumentEntity
import com.example.data.model.DocumentSortOrder
import com.example.engine.FileShareHelper
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DocumentGridCard
import com.example.ui.components.DocumentListCard
import com.example.ui.components.EmptyDocumentsView
import com.example.ui.components.MoveToFolderDialog
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDocumentsScreen(
    viewModel: MainViewModel,
    onScanNew: () -> Unit,
    onOpenDocument: (Long) -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var renamingDoc by remember { mutableStateOf<ScannedDocumentEntity?>(null) }
    var movingDoc by remember { mutableStateOf<ScannedDocumentEntity?>(null) }
    var deletingDoc by remember { mutableStateOf<ScannedDocumentEntity?>(null) }

    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importExternalPdf(uri, uri.lastPathSegment, onOpenDocument)
        }
    }

    val activeFolder: FolderEntity? = remember(folders, selectedFolderId) {
        folders.firstOrNull { it.id == selectedFolderId }
    }

    Scaffold(
        modifier = Modifier.testTag("my_documents_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeFolder?.name ?: "My Documents",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${documents.size} PDF document(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { importPdfLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.testTag("documents_import_pdf_button")
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "Import external PDF", tint = CobaltBlue)
                    }
                    IconButton(
                        onClick = { viewModel.toggleGridView() },
                        modifier = Modifier.testTag("documents_toggle_grid_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle layout"
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort documents")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            DocumentSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.label,
                                            color = if (order == sortOrder) CobaltBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateSortOrder(order)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search by title or OCR text…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("documents_search_input")
            )

            // Active folder filter chip
            if (activeFolder != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CobaltBlueSoft,
                    border = BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Folder: ${activeFolder.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = CobaltBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.selectFolder(null) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear folder filter", tint = CobaltBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (documents.isEmpty()) {
                EmptyDocumentsView(
                    title = if (searchQuery.isNotBlank()) "No matching documents" else "Your library is empty",
                    subtitle = if (searchQuery.isNotBlank()) {
                        "Try searching another keyword or clearing the filter."
                    } else {
                        "Scan documents with your camera or import PDFs from device storage."
                    },
                    primaryActionLabel = if (searchQuery.isBlank()) "Scan Document" else null,
                    onPrimaryAction = if (searchQuery.isBlank()) onScanNew else null
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentGridCard(
                            document = doc,
                            onClick = { onOpenDocument(doc.id) },
                            onShare = { FileShareHelper.sharePdf(context, File(doc.pdfPath), doc.title) },
                            onRename = { renamingDoc = doc },
                            onMoveToFolder = { movingDoc = doc },
                            onDelete = { deletingDoc = doc }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentListCard(
                            document = doc,
                            onClick = { onOpenDocument(doc.id) },
                            onShare = { FileShareHelper.sharePdf(context, File(doc.pdfPath), doc.title) },
                            onRename = { renamingDoc = doc },
                            onMoveToFolder = { movingDoc = doc },
                            onDelete = { deletingDoc = doc }
                        )
                    }
                }
            }
        }
    }

    renamingDoc?.let { doc ->
        RenameDocumentDialog(
            currentTitle = doc.title,
            onDismiss = { renamingDoc = null },
            onConfirm = { newTitle ->
                viewModel.renameDocument(doc.id, newTitle)
                renamingDoc = null
            }
        )
    }

    movingDoc?.let { doc ->
        MoveToFolderDialog(
            folders = folders,
            currentFolderId = doc.folderId,
            onDismiss = { movingDoc = null },
            onSelectFolder = { folderId ->
                viewModel.moveDocumentToFolder(doc.id, folderId)
                movingDoc = null
            }
        )
    }

    deletingDoc?.let { doc ->
        DeleteConfirmDialog(
            title = "Delete Document",
            message = "Delete \"${doc.title}\" permanently?",
            onDismiss = { deletingDoc = null },
            onConfirm = {
                viewModel.deleteDocument(doc.id)
                deletingDoc = null
            }
        )
    }
}
