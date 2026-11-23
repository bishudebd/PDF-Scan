package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.FolderEntity
import com.example.data.local.ScannedDocumentEntity
import com.example.engine.FileShareHelper
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import com.example.ui.theme.FolderColorPalette
import java.io.File

@Composable
fun DocumentListCard(
    document: ScannedDocumentEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("document_card_${document.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Crisp paper thumbnail
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = File(document.thumbnailPath)
                if (thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = CobaltBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                // PDF badge bottom corner
                Surface(
                    color = CobaltBlue,
                    shape = RoundedCornerShape(topEnd = 6.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (document.isSigned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Signed document",
                            tint = AccentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FileShareHelper.formatDate(document.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetadataPill(text = "${document.pageCount} ${if (document.pageCount == 1) "page" else "pages"}")
                    MetadataPill(text = FileShareHelper.formatFileSize(document.fileSizeBytes))
                }
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.testTag("share_doc_${document.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share ${document.title}",
                    tint = CobaltBlue
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("more_doc_${document.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options for ${document.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Folder") },
                        leadingIcon = { Icon(Icons.Filled.DriveFileMove, null) },
                        onClick = {
                            menuExpanded = false
                            onMoveToFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share PDF") },
                        leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentGridCard(
    document: ScannedDocumentEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onMoveToFolder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("document_grid_card_${document.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = File(document.thumbnailPath)
                if (thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = CobaltBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${document.pageCount}p",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${FileShareHelper.formatShortDate(document.updatedAt)} · ${FileShareHelper.formatFileSize(document.fileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Share") }, onClick = { menuExpanded = false; onShare() })
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuExpanded = false; onRename() })
                        DropdownMenuItem(text = { Text("Move to Folder") }, onClick = { menuExpanded = false; onMoveToFolder() })
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = CobaltBlueSoft
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = CobaltBlue,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyDocumentsView(
    title: String,
    subtitle: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_documents_1795327146947),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onPrimaryAction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("empty_state_primary_button")
            ) {
                Text(primaryActionLabel)
            }
        }
    }
}

@Composable
fun ProcessingOverlay(
    visible: Boolean,
    label: String
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = CobaltBlue)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = label.ifBlank { "Processing…" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RenameDocumentDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(currentTitle) { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Document", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Document Name") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_document_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("confirm_rename_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MoveToFolderDialog(
    folders: List<FolderEntity>,
    currentFolderId: Long?,
    onDismiss: () -> Unit,
    onSelectFolder: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Folder", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentFolderId == null) CobaltBlueSoft else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFolder(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No Folder (Root)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (currentFolderId == null) {
                            Icon(Icons.Filled.Check, null, tint = CobaltBlue)
                        }
                    }
                }
                if (folders.isEmpty()) {
                    Text(
                        text = "Create folders from the Folders tab to organize documents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    folders.forEach { folder ->
                        val selected = folder.id == currentFolderId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) CobaltBlueSoft else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFolder(folder.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Folder, null, tint = Color(folder.colorArgb))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(folder.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                if (selected) {
                                    Icon(Icons.Filled.Check, null, tint = CobaltBlue)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CreateEditFolderDialog(
    initialName: String = "",
    initialColor: Long = FolderColorPalette.first(),
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedColor by remember(initialColor) { mutableLongStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) "Edit Folder" else "New Folder",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g., Invoices, Receipts, IDs") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input")
                )
                Column {
                    Text(
                        text = "Folder Accent Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FolderColorPalette.forEach { colorArgb ->
                            val isSelected = selectedColor == colorArgb
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorArgb },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_folder_button")
            ) {
                Text(if (isEditing) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
