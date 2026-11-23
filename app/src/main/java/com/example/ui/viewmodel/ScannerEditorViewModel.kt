package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.PreferencesManager
import com.example.data.model.CropQuad
import com.example.data.model.DraftScanPage
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfQuality
import com.example.data.model.ScanFilter
import com.example.data.model.ScanMode
import com.example.data.repository.DocumentRepository
import com.example.engine.DocumentScannerEngine
import com.example.engine.FileShareHelper
import com.example.engine.MlKitScanOutcome
import com.example.engine.ScannerPreflightStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ScannerEditorUiState(
    val scanMode: ScanMode = ScanMode.DOCUMENT,
    val autoCaptureEnabled: Boolean = true,
    val pages: List<DraftScanPage> = emptyList(),
    val activePageIndex: Int = 0,
    val rescanPageIndex: Int? = null,
    val idCardFrontPath: String? = null,
    val documentTitle: String = FileShareHelper.generateDefaultDocTitle(),
    val targetFolderId: Long? = null,
    val editingDocumentId: Long? = null,
    val pageSize: PageSizeOption = PageSizeOption.A4,
    val pdfQuality: PdfQuality = PdfQuality.HIGH,
    val isProcessing: Boolean = false,
    val processingLabel: String = "",
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val playServicesNotice: String? = null,
    val lowStorageStatus: ScannerPreflightStatus.LowStorage? = null
) {
    val activePage: DraftScanPage?
        get() = pages.getOrNull(activePageIndex)

    val isRescanningPage: Boolean
        get() = rescanPageIndex != null && rescanPageIndex in pages.indices
}

class ScannerEditorViewModel(
    private val repository: DocumentRepository,
    private val preferencesManager: PreferencesManager,
    val scannerEngine: DocumentScannerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerEditorUiState())
    val uiState: StateFlow<ScannerEditorUiState> = _uiState.asStateFlow()

    fun startFreshSession(mode: ScanMode = ScanMode.DOCUMENT, folderId: Long? = null) {
        viewModelScope.launch {
            val settings = preferencesManager.settingsFlow.first()
            val prefix = when (mode) {
                ScanMode.ID_CARD -> "ID_Card"
                ScanMode.OCR -> "OCR_Scan"
                ScanMode.DOCUMENT -> "Scan"
            }
            val storageCheck = scannerEngine.checkStorage()
            _uiState.value = ScannerEditorUiState(
                scanMode = mode,
                autoCaptureEnabled = settings.autoEdgeDetection,
                documentTitle = FileShareHelper.generateDefaultDocTitle(prefix),
                targetFolderId = folderId,
                pageSize = settings.defaultPageSize,
                pdfQuality = settings.defaultPdfQuality,
                lowStorageStatus = storageCheck as? ScannerPreflightStatus.LowStorage
            )
        }
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.update { it.copy(scanMode = mode, idCardFrontPath = null) }
    }

    fun toggleAutoCapture() {
        _uiState.update { it.copy(autoCaptureEnabled = !it.autoCaptureEnabled) }
    }

    /**
     * Enters single-page rescan mode for [pageIndex]. Next captured/ML-Kit page replaces it in place.
     */
    fun startRescanForPage(pageIndex: Int) {
        _uiState.update { state ->
            if (pageIndex in state.pages.indices) {
                state.copy(rescanPageIndex = pageIndex, activePageIndex = pageIndex)
            } else {
                state
            }
        }
    }

    fun clearRescanMode() {
        _uiState.update { it.copy(rescanPageIndex = null) }
    }

    /**
     * Checks free disk space before capture or export, surfacing [lowStorageStatus] when critically low.
     */
    fun ensureStorageAvailable(requiredBytes: Long = DocumentScannerEngine.MIN_SCAN_STORAGE_BYTES): Boolean {
        val status = scannerEngine.checkStorage(requiredBytes)
        return if (status is ScannerPreflightStatus.LowStorage) {
            _uiState.update { it.copy(lowStorageStatus = status) }
            false
        } else {
            _uiState.update { it.copy(lowStorageStatus = null) }
            true
        }
    }

    fun clearScanCacheAndRecheckStorage() {
        viewModelScope.launch {
            val freed = repository.clearScanCache()
            val status = scannerEngine.checkStorage()
            _uiState.update {
                it.copy(
                    lowStorageStatus = status as? ScannerPreflightStatus.LowStorage,
                    infoMessage = "Freed ${FileShareHelper.formatFileSize(freed)} from temporary scan cache"
                )
            }
        }
    }

    fun reportMlKitPreflightFailure(status: ScannerPreflightStatus) {
        when (status) {
            is ScannerPreflightStatus.LowStorage -> {
                _uiState.update { it.copy(lowStorageStatus = status) }
            }
            is ScannerPreflightStatus.PlayServicesUnavailable -> {
                _uiState.update {
                    it.copy(
                        playServicesNotice = "${status.errorMessage} Using built-in CameraX auto-capture scanner."
                    )
                }
            }
            ScannerPreflightStatus.Ready -> {}
        }
    }

    fun reportScannerError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /**
     * Handles the typed [MlKitScanOutcome] returned by Google ML Kit Document Scanner:
     * supports multi-page ingestion, single-page in-place rescan, cancellation, and error reporting.
     */
    fun handleMlKitOutcome(
        outcome: MlKitScanOutcome,
        onPagesReady: () -> Unit
    ) {
        when (outcome) {
            is MlKitScanOutcome.Cancelled -> {
                val wasRescanning = _uiState.value.isRescanningPage
                _uiState.update {
                    it.copy(
                        rescanPageIndex = null,
                        infoMessage = if (wasRescanning) {
                            "Rescan cancelled — original page kept"
                        } else {
                            "Scanning cancelled — existing pages preserved"
                        }
                    )
                }
            }
            is MlKitScanOutcome.Error -> {
                _uiState.update {
                    it.copy(
                        rescanPageIndex = null,
                        errorMessage = outcome.message
                    )
                }
            }
            is MlKitScanOutcome.Success -> {
                ingestMlKitScannedPages(outcome.pageUris, onPagesReady)
            }
        }
    }

    fun loadExistingDocumentForEditing(documentId: Long, onLoaded: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Loading document…") }
            val docWithPages = repository.getDocumentWithPages(documentId)
            val settings = preferencesManager.settingsFlow.first()
            if (docWithPages != null) {
                val draftPages = docWithPages.orderedPages.map { p ->
                    DraftScanPage(
                        id = UUID.randomUUID().toString(),
                        rawImagePath = p.originalImagePath,
                        croppedImagePath = p.originalImagePath,
                        enhancedImagePath = p.imagePath,
                        cropQuad = CropQuad.FULL,
                        rotationDegrees = p.rotationDegrees,
                        filter = ScanFilter.fromName(p.filterName)
                    )
                }
                _uiState.value = ScannerEditorUiState(
                    pages = draftPages,
                    activePageIndex = 0,
                    documentTitle = docWithPages.document.title,
                    targetFolderId = docWithPages.document.folderId,
                    editingDocumentId = docWithPages.document.id,
                    pageSize = settings.defaultPageSize,
                    pdfQuality = settings.defaultPdfQuality,
                    isProcessing = false
                )
                onLoaded()
            } else {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Document not found") }
            }
        }
    }

    /**
     * Adds a captured photo file from CameraX into the active session.
     * Supports single-page rescan replacement, dual-side ID Card merge, and multi-page capture.
     */
    fun addCapturedFile(
        file: File,
        onIdCardComplete: (() -> Unit)? = null,
        onRescanComplete: (() -> Unit)? = null
    ) {
        if (!ensureStorageAvailable()) return
        viewModelScope.launch {
            val settings = preferencesManager.settingsFlow.first()
            val state = _uiState.value

            // Single-page rescan replacement
            val rescanIdx = state.rescanPageIndex
            if (rescanIdx != null && rescanIdx in state.pages.indices) {
                _uiState.update { it.copy(isProcessing = true, processingLabel = "Replacing page ${rescanIdx + 1}…") }
                val quad = if (settings.autoEdgeDetection) {
                    repository.imageProcessor.detectDocumentQuad(file.absolutePath)
                } else {
                    CropQuad.FULL
                }
                val oldFilter = state.pages[rescanIdx].filter
                val replacement = DraftScanPage(
                    id = UUID.randomUUID().toString(),
                    rawImagePath = file.absolutePath,
                    cropQuad = quad,
                    filter = oldFilter
                )
                _uiState.update { cur ->
                    val list = cur.pages.toMutableList()
                    if (rescanIdx in list.indices) {
                        list[rescanIdx] = replacement
                    }
                    cur.copy(
                        pages = list,
                        activePageIndex = rescanIdx,
                        rescanPageIndex = null,
                        isProcessing = false,
                        infoMessage = "Page ${rescanIdx + 1} rescanned"
                    )
                }
                onRescanComplete?.invoke()
                return@launch
            }

            if (state.scanMode == ScanMode.ID_CARD) {
                val front = state.idCardFrontPath
                if (front == null) {
                    _uiState.update { it.copy(idCardFrontPath = file.absolutePath) }
                } else {
                    _uiState.update { it.copy(isProcessing = true, processingLabel = "Composing ID Card sheet…") }
                    val combinedFile = File(repository.scanCacheDir, "id_combined_${System.currentTimeMillis()}.jpg")
                    val ok = repository.imageProcessor.combineIdCardSides(front, file.absolutePath, combinedFile.absolutePath)
                    if (ok) {
                        val page = DraftScanPage(
                            id = UUID.randomUUID().toString(),
                            rawImagePath = combinedFile.absolutePath,
                            croppedImagePath = combinedFile.absolutePath,
                            cropQuad = CropQuad.FULL,
                            filter = settings.defaultFilter
                        )
                        _uiState.update {
                            val updated = it.pages + page
                            it.copy(
                                pages = updated,
                                activePageIndex = updated.lastIndex,
                                idCardFrontPath = null,
                                isProcessing = false
                            )
                        }
                        onIdCardComplete?.invoke()
                    } else {
                        _uiState.update {
                            it.copy(isProcessing = false, errorMessage = "Failed to combine ID card sides")
                        }
                    }
                }
                return@launch
            }

            // Standard document page capture
            val detectedQuad = if (settings.autoEdgeDetection) {
                repository.imageProcessor.detectDocumentQuad(file.absolutePath)
            } else {
                CropQuad.FULL
            }
            val page = DraftScanPage(
                id = UUID.randomUUID().toString(),
                rawImagePath = file.absolutePath,
                cropQuad = detectedQuad,
                filter = settings.defaultFilter
            )
            _uiState.update {
                val updated = it.pages + page
                it.copy(
                    pages = updated,
                    activePageIndex = updated.lastIndex
                )
            }
        }
    }

    /**
     * Imports one or multiple images selected from Android Photo Picker into a draft session.
     */
    fun importImageUris(
        uris: List<Uri>,
        appendToExisting: Boolean = false,
        titlePrefix: String = "Imported",
        onReady: () -> Unit
    ) {
        if (uris.isEmpty()) return
        if (!ensureStorageAvailable()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Importing ${uris.size} image(s)…") }
            val settings = preferencesManager.settingsFlow.first()
            val newPages = mutableListOf<DraftScanPage>()
            uris.forEachIndexed { idx, uri ->
                val dest = File(repository.scanCacheDir, "import_${System.currentTimeMillis()}_$idx.jpg")
                val copied = repository.imageProcessor.copyUriToLocalFile(uri, dest, PdfQuality.HIGH.maxDimensionPx)
                if (copied) {
                    val quad = if (settings.autoEdgeDetection) {
                        repository.imageProcessor.detectDocumentQuad(dest.absolutePath)
                    } else {
                        CropQuad.FULL
                    }
                    newPages.add(
                        DraftScanPage(
                            id = UUID.randomUUID().toString(),
                            rawImagePath = dest.absolutePath,
                            cropQuad = quad,
                            filter = settings.defaultFilter
                        )
                    )
                }
            }
            if (newPages.isEmpty()) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Could not load selected images") }
                return@launch
            }
            _uiState.update { prev ->
                val rescanIdx = prev.rescanPageIndex
                if (rescanIdx != null && rescanIdx in prev.pages.indices) {
                    val list = prev.pages.toMutableList()
                    list[rescanIdx] = newPages.first()
                    prev.copy(
                        pages = list,
                        activePageIndex = rescanIdx,
                        rescanPageIndex = null,
                        isProcessing = false,
                        infoMessage = "Page ${rescanIdx + 1} replaced"
                    )
                } else {
                    val combined = if (appendToExisting) prev.pages + newPages else newPages
                    prev.copy(
                        pages = combined,
                        activePageIndex = if (appendToExisting) prev.pages.size else 0,
                        documentTitle = if (appendToExisting) prev.documentTitle else FileShareHelper.generateDefaultDocTitle(titlePrefix),
                        editingDocumentId = if (appendToExisting) prev.editingDocumentId else null,
                        pageSize = settings.defaultPageSize,
                        pdfQuality = settings.defaultPdfQuality,
                        isProcessing = false
                    )
                }
            }
            onReady()
        }
    }

    /**
     * Ingests high-resolution pages returned by Google ML Kit Document Scanner into our editing pipeline.
     * If rescanning an individual page, replaces that exact page in-place!
     */
    fun ingestMlKitScannedPages(pageUris: List<Uri>, onReady: () -> Unit) {
        if (pageUris.isEmpty()) return
        if (!ensureStorageAvailable()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Processing ${pageUris.size} scanned page(s)…") }
            val settings = preferencesManager.settingsFlow.first()
            val draftPages = mutableListOf<DraftScanPage>()
            pageUris.forEachIndexed { idx, uri ->
                val dest = File(repository.scanCacheDir, "mlkit_${System.currentTimeMillis()}_$idx.jpg")
                if (repository.imageProcessor.copyUriToLocalFile(uri, dest, PdfQuality.HIGH.maxDimensionPx)) {
                    draftPages.add(
                        DraftScanPage(
                            id = UUID.randomUUID().toString(),
                            rawImagePath = dest.absolutePath,
                            croppedImagePath = dest.absolutePath,
                            enhancedImagePath = dest.absolutePath,
                            cropQuad = CropQuad.FULL,
                            filter = ScanFilter.ORIGINAL
                        )
                    )
                }
            }
            if (draftPages.isNotEmpty()) {
                _uiState.update { prev ->
                    val rescanIdx = prev.rescanPageIndex
                    if (rescanIdx != null && rescanIdx in prev.pages.indices) {
                        val updated = prev.pages.toMutableList()
                        updated[rescanIdx] = draftPages.first()
                        prev.copy(
                            pages = updated,
                            activePageIndex = rescanIdx,
                            rescanPageIndex = null,
                            isProcessing = false,
                            infoMessage = "Page ${rescanIdx + 1} rescanned with ML Kit"
                        )
                    } else {
                        prev.copy(
                            pages = prev.pages + draftPages,
                            activePageIndex = prev.pages.size,
                            pageSize = settings.defaultPageSize,
                            pdfQuality = settings.defaultPdfQuality,
                            isProcessing = false
                        )
                    }
                }
                onReady()
            } else {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "No pages could be read from ML Kit scanner") }
            }
        }
    }

    fun selectPage(index: Int) {
        _uiState.update {
            if (index in it.pages.indices) it.copy(activePageIndex = index) else it
        }
    }

    fun removePage(index: Int) {
        _uiState.update { state ->
            if (index !in state.pages.indices) return@update state
            val updated = state.pages.toMutableList().apply { removeAt(index) }
            val newIdx = state.activePageIndex.coerceAtMost((updated.size - 1).coerceAtLeast(0))
            state.copy(
                pages = updated,
                activePageIndex = newIdx,
                infoMessage = "Page ${index + 1} deleted"
            )
        }
    }

    /**
     * Rearranges pages inside the active multi-page scanning session.
     */
    fun movePage(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            if (fromIndex !in state.pages.indices || toIndex !in state.pages.indices || fromIndex == toIndex) {
                return@update state
            }
            val list = state.pages.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val newActive = when (state.activePageIndex) {
                fromIndex -> toIndex
                else -> state.activePageIndex.coerceIn(0, list.lastIndex)
            }
            state.copy(pages = list, activePageIndex = newActive)
        }
    }

    fun updateActiveCropQuad(quad: CropQuad) {
        _uiState.update { state ->
            val idx = state.activePageIndex
            if (idx !in state.pages.indices) return@update state
            val updated = state.pages.toMutableList()
            updated[idx] = updated[idx].copy(cropQuad = quad)
            state.copy(pages = updated)
        }
    }

    fun autoDetectActivePageEdges() {
        val page = _uiState.value.activePage ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Detecting document edges…") }
            val quad = repository.imageProcessor.detectDocumentQuad(page.rawImagePath)
            updateActiveCropQuad(quad)
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun resetActiveCropToFull() {
        updateActiveCropQuad(CropQuad.FULL)
    }

    fun rotateActivePage90(clockwise: Boolean = true) {
        _uiState.update { state ->
            val idx = state.activePageIndex
            if (idx !in state.pages.indices) return@update state
            val updated = state.pages.toMutableList()
            val cur = updated[idx]
            val delta = if (clockwise) 90 else 270
            updated[idx] = cur.copy(rotationDegrees = (cur.rotationDegrees + delta) % 360)
            state.copy(pages = updated)
        }
    }

    /**
     * Commits 4-point perspective correction + rotation across all pages and prepares
     * high-resolution enhanced previews before entering the Enhance / Review screen.
     */
    fun applyCropForAllPagesAndContinue(onDone: () -> Unit) {
        if (!ensureStorageAvailable()) return
        viewModelScope.launch {
            val pagesSnapshot = _uiState.value.pages
            if (pagesSnapshot.isEmpty()) return@launch
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Applying perspective correction…") }

            val maxDim = _uiState.value.pdfQuality.maxDimensionPx
            val updatedPages = pagesSnapshot.mapIndexed { idx, page ->
                val croppedOut = File(repository.scanCacheDir, "cropped_${page.id}_${System.currentTimeMillis()}_$idx.jpg")
                val cropOk = repository.imageProcessor.applyPerspectiveCropAndRotate(
                    sourcePath = page.rawImagePath,
                    outputPath = croppedOut.absolutePath,
                    quad = page.cropQuad,
                    rotationDegrees = page.rotationDegrees,
                    maxDimension = maxDim
                )
                val croppedPath = if (cropOk) croppedOut.absolutePath else page.rawImagePath

                val enhancedOut = File(repository.scanCacheDir, "enh_${page.id}_${System.currentTimeMillis()}_$idx.jpg")
                val enhOk = repository.imageProcessor.applyFilterToFile(
                    sourcePath = croppedPath,
                    outputPath = enhancedOut.absolutePath,
                    filter = page.filter,
                    brightness = page.brightness,
                    contrast = page.contrast,
                    maxDimension = maxDim
                )
                page.copy(
                    croppedImagePath = croppedPath,
                    enhancedImagePath = if (enhOk) enhancedOut.absolutePath else croppedPath
                )
            }

            _uiState.update { it.copy(pages = updatedPages, isProcessing = false) }
            onDone()
        }
    }

    fun applyFilterToActivePage(filter: ScanFilter) {
        val state = _uiState.value
        val idx = state.activePageIndex
        val page = state.pages.getOrNull(idx) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Applying ${filter.label}…") }
            val outFile = File(repository.scanCacheDir, "enh_${page.id}_${filter.name}_${System.currentTimeMillis()}.jpg")
            val ok = repository.imageProcessor.applyFilterToFile(
                sourcePath = page.activeSourcePath,
                outputPath = outFile.absolutePath,
                filter = filter,
                brightness = page.brightness,
                contrast = page.contrast,
                maxDimension = state.pdfQuality.maxDimensionPx
            )
            _uiState.update { cur ->
                val list = cur.pages.toMutableList()
                if (idx in list.indices) {
                    list[idx] = list[idx].copy(
                        filter = filter,
                        enhancedImagePath = if (ok) outFile.absolutePath else list[idx].enhancedImagePath
                    )
                }
                cur.copy(pages = list, isProcessing = false)
            }
        }
    }

    fun updateBrightnessContrast(brightness: Float, contrast: Float) {
        val state = _uiState.value
        val idx = state.activePageIndex
        val page = state.pages.getOrNull(idx) ?: return
        viewModelScope.launch {
            val outFile = File(repository.scanCacheDir, "adj_${page.id}_${System.currentTimeMillis()}.jpg")
            val ok = repository.imageProcessor.applyFilterToFile(
                sourcePath = page.activeSourcePath,
                outputPath = outFile.absolutePath,
                filter = page.filter,
                brightness = brightness,
                contrast = contrast,
                maxDimension = state.pdfQuality.maxDimensionPx
            )
            _uiState.update { cur ->
                val list = cur.pages.toMutableList()
                if (idx in list.indices) {
                    list[idx] = list[idx].copy(
                        brightness = brightness,
                        contrast = contrast,
                        enhancedImagePath = if (ok) outFile.absolutePath else list[idx].enhancedImagePath
                    )
                }
                cur.copy(pages = list)
            }
        }
    }

    fun applyActiveFilterToAllPages() {
        val state = _uiState.value
        val active = state.activePage ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Applying ${active.filter.label} to all pages…") }
            val updated = state.pages.mapIndexed { idx, p ->
                val outFile = File(repository.scanCacheDir, "enh_all_${p.id}_${System.currentTimeMillis()}_$idx.jpg")
                val ok = repository.imageProcessor.applyFilterToFile(
                    sourcePath = p.activeSourcePath,
                    outputPath = outFile.absolutePath,
                    filter = active.filter,
                    brightness = active.brightness,
                    contrast = active.contrast,
                    maxDimension = state.pdfQuality.maxDimensionPx
                )
                p.copy(
                    filter = active.filter,
                    brightness = active.brightness,
                    contrast = active.contrast,
                    enhancedImagePath = if (ok) outFile.absolutePath else p.enhancedImagePath
                )
            }
            _uiState.update {
                it.copy(
                    pages = updated,
                    isProcessing = false,
                    infoMessage = "${active.filter.label} applied to all ${updated.size} pages"
                )
            }
        }
    }

    fun rotateEnhancedActivePage() {
        val state = _uiState.value
        val idx = state.activePageIndex
        val page = state.pages.getOrNull(idx) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Rotating page…") }
            val rotatedSource = File(repository.scanCacheDir, "rot_src_${page.id}_${System.currentTimeMillis()}.jpg")
            val okRot = repository.imageProcessor.applyPerspectiveCropAndRotate(
                sourcePath = page.activeSourcePath,
                outputPath = rotatedSource.absolutePath,
                quad = CropQuad.FULL,
                rotationDegrees = 90,
                maxDimension = state.pdfQuality.maxDimensionPx
            )
            val newSrc = if (okRot) rotatedSource.absolutePath else page.activeSourcePath
            val enhFile = File(repository.scanCacheDir, "rot_enh_${page.id}_${System.currentTimeMillis()}.jpg")
            val okEnh = repository.imageProcessor.applyFilterToFile(
                sourcePath = newSrc,
                outputPath = enhFile.absolutePath,
                filter = page.filter,
                brightness = page.brightness,
                contrast = page.contrast,
                maxDimension = state.pdfQuality.maxDimensionPx
            )
            _uiState.update { cur ->
                val list = cur.pages.toMutableList()
                if (idx in list.indices) {
                    list[idx] = list[idx].copy(
                        croppedImagePath = newSrc,
                        enhancedImagePath = if (okEnh) enhFile.absolutePath else newSrc,
                        rotationDegrees = (list[idx].rotationDegrees + 90) % 360
                    )
                }
                cur.copy(pages = list, isProcessing = false)
            }
        }
    }

    fun updateDocumentTitle(title: String) {
        _uiState.update { it.copy(documentTitle = title) }
    }

    fun updatePageSize(size: PageSizeOption) {
        _uiState.update { it.copy(pageSize = size) }
    }

    fun updatePdfQuality(quality: PdfQuality) {
        _uiState.update { it.copy(pdfQuality = quality) }
    }

    fun saveDocumentAsPdf(onSaved: (Long) -> Unit) {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        if (!ensureStorageAvailable(DocumentScannerEngine.MIN_EXPORT_STORAGE_BYTES)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Generating high-resolution PDF…") }
            val docId = repository.saveDraftSessionAsDocument(
                title = state.documentTitle,
                pages = state.pages,
                pageSize = state.pageSize,
                quality = state.pdfQuality,
                folderId = state.targetFolderId,
                existingDocumentId = state.editingDocumentId
            )
            _uiState.update { it.copy(isProcessing = false) }
            if (docId != null) {
                onSaved(docId)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to generate PDF — check available storage") }
            }
        }
    }

    /**
     * Generates high-resolution JPG output files for all scanned pages in the current session.
     */
    fun exportSessionAsJpgs(onExported: (List<File>) -> Unit) {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        if (!ensureStorageAvailable(DocumentScannerEngine.MIN_EXPORT_STORAGE_BYTES)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Generating high-resolution JPGs…") }
            val files = repository.exportDraftSessionAsJpgs(
                title = state.documentTitle,
                pages = state.pages,
                quality = state.pdfQuality
            )
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    infoMessage = if (files.isNotEmpty()) {
                        "Generated ${files.size} high-resolution JPG file(s)"
                    } else null,
                    errorMessage = if (files.isEmpty()) "Failed to generate JPG output" else null
                )
            }
            if (files.isNotEmpty()) {
                onExported(files)
            }
        }
    }

    fun clearNotices() {
        _uiState.update {
            it.copy(
                infoMessage = null,
                errorMessage = null,
                playServicesNotice = null
            )
        }
    }

    fun dismissLowStorageDialog() {
        _uiState.update { it.copy(lowStorageStatus = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScannerEditorViewModel(
                        repository = container.documentRepository,
                        preferencesManager = container.preferencesManager,
                        scannerEngine = container.scannerEngine
                    ) as T
                }
            }
    }
}
