package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ExportImageFormat
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfQuality
import com.example.data.model.ScanFilter
import com.example.engine.FileShareHelper
import com.example.ui.components.MetadataPill
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import com.example.ui.viewmodel.ScannerEditorViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhanceScreen(
    viewModel: ScannerEditorViewModel,
    onBack: () -> Unit,
    onAddPage: () -> Unit,
    onRescanPage: () -> Unit,
    onCropPage: () -> Unit,
    onSavedDocument: (Long) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activePage = state.activePage
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAdjustSliders by remember { mutableStateOf(false) }
    var showSaveOptionsDialog by remember { mutableStateOf(false) }
    var showPagesManagerSheet by remember { mutableStateOf(false) }

    var brightnessSlider by remember(activePage?.id) {
        mutableFloatStateOf(activePage?.brightness ?: 0f)
    }
    var contrastSlider by remember(activePage?.id) {
        mutableFloatStateOf(activePage?.contrast ?: 1f)
    }

    val pagerState = rememberPagerState(
        initialPage = state.activePageIndex.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0)),
        pageCount = { state.pages.size.coerceAtLeast(1) }
    )

    // Keep HorizontalPager and ViewModel activePageIndex bidirectionally synced
    LaunchedEffect(pagerState.currentPage, state.pages.size) {
        if (pagerState.currentPage in state.pages.indices && pagerState.currentPage != state.activePageIndex) {
            viewModel.selectPage(pagerState.currentPage)
        }
    }
    LaunchedEffect(state.activePageIndex, state.pages.size) {
        if (state.activePageIndex in state.pages.indices && pagerState.currentPage != state.activePageIndex) {
            pagerState.animateScrollToPage(state.activePageIndex)
        }
    }

    LaunchedEffect(state.infoMessage, state.errorMessage) {
        val msg = state.errorMessage ?: state.infoMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotices()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.testTag("enhance_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSaveOptionsDialog = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = state.documentTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${state.pages.size} page(s) · ${state.pageSize.name} · ${state.pdfQuality.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit title and output quality",
                                tint = CobaltBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Manage / Reorder / Preview All Pages button
                        IconButton(
                            onClick = { showPagesManagerSheet = true },
                            modifier = Modifier.testTag("enhance_manage_pages_button")
                        ) {
                            Icon(Icons.Filled.GridView, contentDescription = "Preview and rearrange all pages", tint = CobaltBlue)
                        }
                        // Add page button
                        IconButton(
                            onClick = {
                                viewModel.clearRescanMode()
                                onAddPage()
                            },
                            modifier = Modifier.testTag("enhance_add_page_button")
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Add another page", tint = CobaltBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 10.dp, bottom = 14.dp)
                    ) {
                        // Brightness & Contrast collapsible panel
                        if (showAdjustSliders && activePage != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Brightness", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(78.dp))
                                    Slider(
                                        value = brightnessSlider,
                                        onValueChange = { brightnessSlider = it },
                                        onValueChangeFinished = {
                                            viewModel.updateBrightnessContrast(brightnessSlider, contrastSlider)
                                        },
                                        valueRange = -50f..50f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Contrast", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(78.dp))
                                    Slider(
                                        value = contrastSlider,
                                        onValueChange = { contrastSlider = it },
                                        onValueChangeFinished = {
                                            viewModel.updateBrightnessContrast(brightnessSlider, contrastSlider)
                                        },
                                        valueRange = 0.6f..1.8f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Filter Chips Row: Original, Magic Colour, Grayscale, Black & White, Crisp Document
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ScanFilter.entries) { filter ->
                                val selected = activePage?.filter == filter
                                Surface(
                                    onClick = { viewModel.applyFilterToActivePage(filter) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) CobaltBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) CobaltBlue else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                                ) {
                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Page-level quick tools: Crop | Rescan | Rotate | Adjust | Apply All
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                TextButton(onClick = onCropPage, modifier = Modifier.testTag("enhance_crop_button")) {
                                    Icon(Icons.Filled.Crop, null, tint = CobaltBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Crop", color = CobaltBlue)
                                }
                            }
                            item {
                                TextButton(
                                    onClick = {
                                        viewModel.startRescanForPage(state.activePageIndex)
                                        onRescanPage()
                                    },
                                    modifier = Modifier.testTag("enhance_rescan_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, null, tint = CobaltBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rescan", color = CobaltBlue)
                                }
                            }
                            item {
                                TextButton(
                                    onClick = { viewModel.rotateEnhancedActivePage() },
                                    modifier = Modifier.testTag("enhance_rotate_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.RotateRight, null, tint = CobaltBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rotate", color = CobaltBlue)
                                }
                            }
                            item {
                                TextButton(
                                    onClick = { showAdjustSliders = !showAdjustSliders },
                                    modifier = Modifier.testTag("enhance_adjust_button")
                                ) {
                                    Icon(Icons.Filled.Tune, null, tint = CobaltBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adjust", color = CobaltBlue)
                                }
                            }
                            if (state.pages.size > 1) {
                                item {
                                    TextButton(
                                        onClick = { viewModel.applyActiveFilterToAllPages() },
                                        modifier = Modifier.testTag("enhance_apply_all_button")
                                    ) {
                                        Icon(Icons.Filled.DoneAll, null, tint = CobaltBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Apply All", color = CobaltBlue)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Output CTA Row: High-Res Export JPG + Save High-Res PDF
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportSessionAsJpgs { files ->
                                        FileShareHelper.shareImages(context, files, ExportImageFormat.JPG.mimeType)
                                    }
                                },
                                enabled = state.pages.isNotEmpty() && !state.isProcessing,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, CobaltBlue),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .height(48.dp)
                                    .testTag("enhance_export_jpg_button")
                            ) {
                                Icon(Icons.Filled.Image, null, tint = CobaltBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JPG", color = CobaltBlue)
                            }
                            Button(
                                onClick = { viewModel.saveDocumentAsPdf(onSavedDocument) },
                                enabled = state.pages.isNotEmpty() && !state.isProcessing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(48.dp)
                                    .testTag("enhance_save_pdf_button")
                            ) {
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save PDF")
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.pages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataPill(text = "Page ${state.activePageIndex + 1} of ${state.pages.size}")
                        activePage?.let { MetadataPill(text = it.filter.label) }
                    }

                    // Swipeable Multi-Page Preview Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { idx ->
                        val page = state.pages.getOrNull(idx)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (page != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    shadowElevation = 6.dp,
                                    color = Color.White,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    AsyncImage(
                                        model = File(page.displayPath),
                                        contentDescription = "Scanned page ${idx + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Thumbnail Filmstrip
                    if (state.pages.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(state.pages, key = { _, p -> p.id }) { idx, page ->
                                val selected = idx == state.activePageIndex
                                Box(
                                    modifier = Modifier
                                        .size(width = 48.dp, height = 62.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) CobaltBlue else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.selectPage(idx)
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        }
                                ) {
                                    AsyncImage(
                                        model = File(page.displayPath),
                                        contentDescription = "Page ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = if (selected) CobaltBlue else Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("All pages removed", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onAddPage) { Text("Scan New Page") }
                        }
                    }
                }
            }
        }

        ProcessingOverlay(visible = state.isProcessing, label = state.processingLabel)
    }

    // All Pages Preview, Reorder, Rescan, Add & Delete Manager Sheet
    if (showPagesManagerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPagesManagerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("All Scanned Pages (${state.pages.size})", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Preview, reorder, rescan, or delete pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            showPagesManagerSheet = false
                            viewModel.clearRescanMode()
                            onAddPage()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        Icon(Icons.Filled.AddAPhoto, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    itemsIndexed(state.pages, key = { _, p -> p.id }) { idx, page ->
                        val isSelected = idx == state.activePageIndex
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CobaltBlue else MaterialTheme.colorScheme.outline
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.76f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .clickable {
                                            viewModel.selectPage(idx)
                                            showPagesManagerSheet = false
                                        }
                                ) {
                                    AsyncImage(
                                        model = File(page.displayPath),
                                        contentDescription = "Page ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = CobaltBlue,
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = "Page ${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Reorder Left | Reorder Right | Rescan | Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.movePage(idx, idx - 1) },
                                        enabled = idx > 0,
                                        modifier = Modifier.size(34.dp).testTag("move_page_left_$idx")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move earlier", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.movePage(idx, idx + 1) },
                                        enabled = idx < state.pages.lastIndex,
                                        modifier = Modifier.size(34.dp).testTag("move_page_right_$idx")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move later", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            showPagesManagerSheet = false
                                            viewModel.startRescanForPage(idx)
                                            onRescanPage()
                                        },
                                        modifier = Modifier.size(34.dp).testTag("rescan_page_$idx")
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "Rescan page ${idx + 1}", tint = CobaltBlue, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.removePage(idx) },
                                        modifier = Modifier.size(34.dp).testTag("delete_page_$idx")
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete page ${idx + 1}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveOptionsDialog = false },
            title = { Text("Document Output Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.documentTitle,
                        onValueChange = { viewModel.updateDocumentTitle(it) },
                        label = { Text("Document Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Page Size", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageSizeOption.entries.forEach { size ->
                            FilterChip(
                                selected = state.pageSize == size,
                                onClick = { viewModel.updatePageSize(size) },
                                label = { Text(size.name) }
                            )
                        }
                    }
                    Text("Output Resolution", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PdfQuality.entries) { q ->
                            FilterChip(
                                selected = state.pdfQuality == q,
                                onClick = { viewModel.updatePdfQuality(q) },
                                label = { Text(q.label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSaveOptionsDialog = false }) { Text("Done") }
            }
        )
    }

    state.lowStorageStatus?.let { low ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissLowStorageDialog() },
            icon = { Icon(Icons.Filled.WarningAmber, null, tint = AccentAmber) },
            title = { Text("Low Device Storage") },
            text = {
                Text("Only ${low.availableMb} MB is free (${low.requiredMb} MB needed for high-resolution PDF/JPG output). Clear scan cache to free space.")
            },
            confirmButton = {
                Button(onClick = { viewModel.clearScanCacheAndRecheckStorage() }) { Text("Clear Scan Cache") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLowStorageDialog() }) { Text("Dismiss") }
            }
        )
    }
}
