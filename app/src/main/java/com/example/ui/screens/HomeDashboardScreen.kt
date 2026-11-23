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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FolderEntity
import com.example.data.local.ScannedDocumentEntity
import com.example.engine.FileShareHelper
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.DocumentListCard
import com.example.ui.components.EmptyDocumentsView
import com.example.ui.components.MoveToFolderDialog
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCobalt
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MidnightNavy
import java.io.File

private data class QuickToolTile(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeDashboardScreen(
    recentDocuments: List<ScannedDocumentEntity>,
    totalDocCount: Int,
    folders: List<FolderEntity>,
    onScanDocument: () -> Unit,
    onImportImages: () -> Unit,
    onImageToPdf: () -> Unit,
    onPdfToImages: () -> Unit,
    onMergePdf: () -> Unit,
    onSplitPdf: () -> Unit,
    onCompressPdf: () -> Unit,
    onSignPdf: () -> Unit,
    onOcrScanner: () -> Unit,
    onIdCardScanner: () -> Unit,
    onOpenSearch: () -> Unit,
    onViewAllDocuments: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    onRenameDocument: (Long, String) -> Unit,
    onMoveDocument: (Long, Long?) -> Unit,
    onDeleteDocument: (Long) -> Unit
) {
    val context = LocalContext.current
    var renamingDoc by remember { mutableStateOf<ScannedDocumentEntity?>(null) }
    var movingDoc by remember { mutableStateOf<ScannedDocumentEntity?>(null) }
    var deletingDoc by remember { mutableStateOf<ScannedDocumentEntity?>(null) }

    val quickTools = remember(
        onScanDocument, onImportImages, onImageToPdf, onPdfToImages,
        onMergePdf, onSplitPdf, onCompressPdf, onSignPdf, onOcrScanner, onIdCardScanner
    ) {
        listOf(
            QuickToolTile("scan_doc", "Scan Document", Icons.Filled.DocumentScanner, AccentCobalt, onScanDocument),
            QuickToolTile("import_images", "Import Images", Icons.Filled.PhotoLibrary, AccentTeal, onImportImages),
            QuickToolTile("image_to_pdf", "Image to PDF", Icons.Filled.PictureAsPdf, AccentIndigo, onImageToPdf),
            QuickToolTile("pdf_to_images", "PDF to Images", Icons.Filled.Collections, AccentRose, onPdfToImages),
            QuickToolTile("merge_pdf", "Merge PDF", Icons.Filled.CallMerge, AccentCobalt, onMergePdf),
            QuickToolTile("split_pdf", "Split PDF", Icons.Filled.CallSplit, AccentAmber, onSplitPdf),
            QuickToolTile("compress_pdf", "Compress PDF", Icons.Filled.Compress, AccentEmerald, onCompressPdf),
            QuickToolTile("sign_pdf", "Sign PDF", Icons.Filled.Draw, AccentViolet, onSignPdf),
            QuickToolTile("ocr_scanner", "OCR Scanner", Icons.Filled.TextSnippet, AccentIndigo, onOcrScanner),
            QuickToolTile("id_card_scanner", "ID Card Scan", Icons.Filled.Badge, AccentTeal, onIdCardScanner)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Brand Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (totalDocCount == 0) "Local Private Vault" else "$totalDocCount saved locally",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Quick Search Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("home_search_bar")
                    .clickable(onClick = onOpenSearch),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search documents",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.dashboard_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Executive Hero Scanner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepNavy, MidnightNavy, CobaltBlue)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "MULTI-PAGE PDF ENGINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFBFDBFE),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Scan & Edit PDFs\nwith Studio Clarity",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Auto edge detection, filters, OCR & digital signing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DocumentScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onScanDocument,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = DeepNavy
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("hero_scan_button")
                        ) {
                            Icon(Icons.Filled.DocumentScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Document", fontWeight = FontWeight.SemiBold)
                        }
                        Surface(
                            onClick = onImportImages,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                            modifier = Modifier
                                .weight(0.85f)
                                .height(48.dp)
                                .testTag("hero_import_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Image, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Import",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Complete 10-Tool Quick Grid (2 rows of 5 or 2-column / 3-column crisp cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.section_quick_tools),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Clean 2x5 (5 rows of 2? No, 2 rows of 4 + 2? Better: 3+3+4? Let's do clean 2 rows of 5 -> tight on small screens; 2 columns or 3 columns -> 3+3+4 or 5x2 grid of compact pill cards! Even better: 2 rows of 5 horizontal or 3x3 + 1? Actually 2 columns x 5 rows? Too tall! 3 columns? 10 items = 3,3,3,1. What about 2 rows of 4 = 8 tools, since Scan Document & Import Images are already Hero buttons — wait, let's show ALL 10 in a crisp 2-column compact row OR 2x5? Wait: 2 rows of 4 + 2 featured = 10! Look how clean 2 featured cards (ID Card Scanner + OCR Text Scanner) + 2 rows of 4 tool tiles (Scan Document, Import Images, Image to PDF, PDF to Images, Merge PDF, Split PDF, Compress PDF, Sign PDF) looks!)
                val primaryGrid = quickTools.take(8)
                primaryGrid.chunked(4).forEach { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTools.forEach { tile ->
                            DashboardToolCell(
                                tile = tile,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Specialized Smart Scanners row (OCR Text Scanner + ID Card Scanner)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickTools.drop(8).forEach { tile ->
                        SmartBannerToolCard(
                            tile = tile,
                            subtitle = if (tile.id == "ocr_scanner") "Extract text via ML Kit" else "Front & back on 1 page",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Recent Documents Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.section_recent_documents),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (recentDocuments.isNotEmpty()) {
                    TextButton(
                        onClick = onViewAllDocuments,
                        modifier = Modifier.testTag("view_all_documents_button")
                    ) {
                        Text(stringResource(R.string.action_view_all), color = CobaltBlue)
                    }
                }
            }
        }

        if (recentDocuments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    EmptyDocumentsView(
                        title = stringResource(R.string.empty_documents_title),
                        subtitle = stringResource(R.string.empty_documents_desc),
                        primaryActionLabel = "Scan Your First Document",
                        onPrimaryAction = onScanDocument
                    )
                }
            }
        } else {
            items(recentDocuments, key = { it.id }) { doc ->
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

    renamingDoc?.let { doc ->
        RenameDocumentDialog(
            currentTitle = doc.title,
            onDismiss = { renamingDoc = null },
            onConfirm = { newTitle ->
                onRenameDocument(doc.id, newTitle)
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
                onMoveDocument(doc.id, folderId)
                movingDoc = null
            }
        )
    }

    deletingDoc?.let { doc ->
        DeleteConfirmDialog(
            title = "Delete Document",
            message = "Delete \"${doc.title}\" and all its pages from local storage?",
            onDismiss = { deletingDoc = null },
            onConfirm = {
                onDeleteDocument(doc.id)
                deletingDoc = null
            }
        )
    }
}

@Composable
private fun DashboardToolCell(
    tile: QuickToolTile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("tool_tile_${tile.id}")
            .clickable(onClick = tile.onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tile.accent.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.label,
                    tint = tile.accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmartBannerToolCard(
    tile: QuickToolTile,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("tool_tile_${tile.id}")
            .clickable(onClick = tile.onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tile.accent.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, tile.accent.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tile.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
