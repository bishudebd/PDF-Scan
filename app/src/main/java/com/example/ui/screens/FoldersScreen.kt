package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.FolderEntity
import com.example.ui.components.CreateEditFolderDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyDocumentsView
import com.example.ui.theme.CobaltBlue
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: MainViewModel,
    onOpenFolderDocuments: (Long) -> Unit
) {
    val foldersWithCount by viewModel.foldersWithCount.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderEntity?>(null) }

    Scaffold(
        modifier = Modifier.testTag("folders_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Folders", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${foldersWithCount.size} folder(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("create_folder_button")
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (foldersWithCount.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyDocumentsView(
                    title = stringResource(R.string.empty_folders_title),
                    subtitle = stringResource(R.string.empty_folders_desc),
                    primaryActionLabel = "Create First Folder",
                    onPrimaryAction = { showCreateDialog = true }
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(foldersWithCount, key = { it.folder.id }) { item ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    val accent = Color(item.folder.colorArgb)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_card_${item.folder.id}")
                            .clickable { onOpenFolderDocuments(item.folder.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(accent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = item.folder.name,
                                        tint = accent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "Folder options")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Folder") },
                                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                            onClick = {
                                                menuExpanded = false
                                                editingFolder = item.folder
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                deletingFolder = item.folder
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = item.folder.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.documentCount} ${if (item.documentCount == 1) "document" else "documents"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEditFolderDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color ->
                viewModel.createFolder(name, color)
                showCreateDialog = false
            }
        )
    }

    editingFolder?.let { folder ->
        CreateEditFolderDialog(
            initialName = folder.name,
            initialColor = folder.colorArgb,
            isEditing = true,
            onDismiss = { editingFolder = null },
            onConfirm = { name, color ->
                viewModel.updateFolder(folder.id, name, color)
                editingFolder = null
            }
        )
    }

    deletingFolder?.let { folder ->
        DeleteConfirmDialog(
            title = "Delete Folder",
            message = "Delete folder \"${folder.name}\"? Documents inside will remain safely in My Documents.",
            onDismiss = { deletingFolder = null },
            onConfirm = {
                viewModel.deleteFolder(folder.id)
                deletingFolder = null
            }
        )
    }
}
