package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.PreferencesManager
import com.example.data.local.ScannedDocumentEntity
import com.example.data.model.ExportImageFormat
import com.example.data.model.PdfQuality
import com.example.data.repository.DocumentRepository
import com.example.engine.CompressResult
import com.example.engine.FileShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class ActivePdfTool(val routeKey: String, val title: String) {
    HUB("hub", "PDF Tools"),
    MERGE("merge", "Merge PDF"),
    SPLIT("split", "Split PDF"),
    COMPRESS("compress", "Compress PDF"),
    PDF_TO_IMAGES("pdf_to_images", "PDF to Images"),
    SIGN("sign", "Sign PDF"),
    OCR("ocr", "OCR Text Scanner");

    companion object {
        fun fromKey(key: String?): ActivePdfTool =
            entries.firstOrNull { it.routeKey == key } ?: HUB
    }
}

data class PdfToolsUiState(
    val activeTool: ActivePdfTool = ActivePdfTool.HUB,
    val isProcessing: Boolean = false,
    val processingLabel: String = "",
    val statusMessage: String? = null,
    // Merge
    val selectedMergeDocIds: List<Long> = emptyList(),
    val mergeOutputTitle: String = FileShareHelper.generateDefaultDocTitle("Merged"),
    // Split
    val selectedSplitDocId: Long? = null,
    val splitPageThumbnails: List<String> = emptyList(),
    val selectedSplitPageIndices: Set<Int> = emptySet(),
    val splitOutputTitle: String = FileShareHelper.generateDefaultDocTitle("Split"),
    // Compress
    val selectedCompressDocId: Long? = null,
    val compressQuality: PdfQuality = PdfQuality.MEDIUM,
    val lastCompressResult: CompressResult? = null,
    val lastCompressedDocId: Long? = null,
    // PDF to Images
    val selectedExportDocId: Long? = null,
    val exportFormat: ExportImageFormat = ExportImageFormat.JPG,
    val exportedImageFiles: List<File> = emptyList(),
    // Sign PDF
    val selectedSignDocId: Long? = null,
    val signDocPagePaths: List<String> = emptyList(),
    val signTargetPageIndex: Int = 0,
    val signaturePreviewPath: String = "",
    val signPosX: Float = 0.72f,
    val signPosY: Float = 0.84f,
    val signWidthNorm: Float = 0.34f,
    // OCR Scanner
    val ocrSourceImagePath: String? = null,
    val ocrExtractedText: String = "",
    val ocrWordCount: Int = 0
)

class PdfToolsViewModel(
    private val repository: DocumentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val allDocuments: StateFlow<List<ScannedDocumentEntity>> =
        repository.observeAllDocuments()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PdfToolsUiState())
    val uiState: StateFlow<PdfToolsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.settingsFlow.collect { settings ->
                if (settings.savedSignaturePath.isNotBlank() && File(settings.savedSignaturePath).exists()) {
                    _uiState.update { it.copy(signaturePreviewPath = settings.savedSignaturePath) }
                }
            }
        }
    }

    fun selectTool(tool: ActivePdfTool, preselectDocId: Long? = null) {
        _uiState.update {
            it.copy(
                activeTool = tool,
                statusMessage = null,
                lastCompressResult = null,
                exportedImageFiles = emptyList()
            )
        }
        if (preselectDocId != null) {
            when (tool) {
                ActivePdfTool.SPLIT -> selectDocumentForSplit(preselectDocId)
                ActivePdfTool.COMPRESS -> selectDocumentForCompress(preselectDocId)
                ActivePdfTool.PDF_TO_IMAGES -> selectDocumentForExport(preselectDocId)
                ActivePdfTool.SIGN -> selectDocumentForSign(preselectDocId)
                ActivePdfTool.OCR -> runOcrOnSavedDocument(preselectDocId)
                else -> {}
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    // --- MERGE PDF ---
    fun toggleMergeDoc(docId: Long) {
        _uiState.update { state ->
            val list = state.selectedMergeDocIds.toMutableList()
            if (list.contains(docId)) list.remove(docId) else list.add(docId)
            state.copy(selectedMergeDocIds = list)
        }
    }

    fun moveMergeDocUp(docId: Long) {
        _uiState.update { state ->
            val list = state.selectedMergeDocIds.toMutableList()
            val idx = list.indexOf(docId)
            if (idx > 0) {
                list.removeAt(idx)
                list.add(idx - 1, docId)
            }
            state.copy(selectedMergeDocIds = list)
        }
    }

    fun updateMergeTitle(title: String) {
        _uiState.update { it.copy(mergeOutputTitle = title) }
    }

    fun executeMerge(onCreated: (Long) -> Unit) {
        val ids = _uiState.value.selectedMergeDocIds
        if (ids.size < 2) {
            _uiState.update { it.copy(statusMessage = "Select at least 2 PDF documents to merge") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Merging ${ids.size} PDFs…") }
            val newId = repository.mergeSavedDocuments(ids, _uiState.value.mergeOutputTitle)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    selectedMergeDocIds = emptyList(),
                    mergeOutputTitle = FileShareHelper.generateDefaultDocTitle("Merged"),
                    statusMessage = if (newId != null) "Merged PDF saved!" else "Merge failed"
                )
            }
            if (newId != null) onCreated(newId)
        }
    }

    // --- SPLIT PDF ---
    fun selectDocumentForSplit(docId: Long) {
        viewModelScope.launch {
            val docWithPages = repository.getDocumentWithPages(docId) ?: return@launch
            val paths = docWithPages.orderedPages.map { it.imagePath }
            _uiState.update {
                it.copy(
                    selectedSplitDocId = docId,
                    splitPageThumbnails = paths,
                    selectedSplitPageIndices = if (paths.isNotEmpty()) setOf(0) else emptySet(),
                    splitOutputTitle = "${docWithPages.document.title}_Split"
                )
            }
        }
    }

    fun toggleSplitPage(pageIndex: Int) {
        _uiState.update { state ->
            val set = state.selectedSplitPageIndices.toMutableSet()
            if (set.contains(pageIndex)) set.remove(pageIndex) else set.add(pageIndex)
            state.copy(selectedSplitPageIndices = set)
        }
    }

    fun selectAllSplitPages() {
        _uiState.update { state ->
            state.copy(selectedSplitPageIndices = state.splitPageThumbnails.indices.toSet())
        }
    }

    fun updateSplitTitle(title: String) {
        _uiState.update { it.copy(splitOutputTitle = title) }
    }

    fun executeSplit(onCreated: (Long) -> Unit) {
        val docId = _uiState.value.selectedSplitDocId ?: return
        val pages = _uiState.value.selectedSplitPageIndices.sorted()
        if (pages.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Select at least one page to extract") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Extracting ${pages.size} page(s)…") }
            val newId = repository.splitSavedDocument(docId, pages, _uiState.value.splitOutputTitle)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = if (newId != null) "Split PDF saved!" else "Split failed"
                )
            }
            if (newId != null) onCreated(newId)
        }
    }

    // --- COMPRESS PDF ---
    fun selectDocumentForCompress(docId: Long) {
        _uiState.update {
            it.copy(selectedCompressDocId = docId, lastCompressResult = null, lastCompressedDocId = null)
        }
    }

    fun selectCompressQuality(quality: PdfQuality) {
        _uiState.update { it.copy(compressQuality = quality) }
    }

    fun executeCompress() {
        val docId = _uiState.value.selectedCompressDocId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Compressing PDF…") }
            val resultPair = repository.compressSavedDocument(docId, _uiState.value.compressQuality)
            if (resultPair != null) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastCompressedDocId = resultPair.first,
                        lastCompressResult = resultPair.second,
                        statusMessage = "Compressed PDF saved to My Documents"
                    )
                }
            } else {
                _uiState.update { it.copy(isProcessing = false, statusMessage = "Compression failed") }
            }
        }
    }

    // --- PDF TO IMAGES ---
    fun selectDocumentForExport(docId: Long) {
        _uiState.update { it.copy(selectedExportDocId = docId, exportedImageFiles = emptyList()) }
    }

    fun selectExportFormat(format: ExportImageFormat) {
        _uiState.update { it.copy(exportFormat = format) }
    }

    fun executePdfToImages() {
        val docId = _uiState.value.selectedExportDocId ?: return
        val format = _uiState.value.exportFormat
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Rendering ${format.extension.uppercase()} pages…") }
            val files = repository.exportDocumentAsImages(docId, format)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    exportedImageFiles = files,
                    statusMessage = if (files.isNotEmpty()) "Exported ${files.size} ${format.extension.uppercase()} images" else "Export failed"
                )
            }
        }
    }

    // --- SIGN PDF ---
    fun selectDocumentForSign(docId: Long) {
        viewModelScope.launch {
            val docWithPages = repository.getDocumentWithPages(docId) ?: return@launch
            _uiState.update {
                it.copy(
                    selectedSignDocId = docId,
                    signDocPagePaths = docWithPages.orderedPages.map { p -> p.imagePath },
                    signTargetPageIndex = 0
                )
            }
        }
    }

    fun selectSignPage(pageIndex: Int) {
        _uiState.update { it.copy(signTargetPageIndex = pageIndex) }
    }

    fun updateSignaturePlacement(xNorm: Float, yNorm: Float, widthNorm: Float) {
        _uiState.update {
            it.copy(
                signPosX = xNorm.coerceIn(0.15f, 0.85f),
                signPosY = yNorm.coerceIn(0.12f, 0.90f),
                signWidthNorm = widthNorm.coerceIn(0.18f, 0.65f)
            )
        }
    }

    fun saveDrawnSignatureBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val sigFile = File(repository.documentsRootDir, "saved_signature.png")
            repository.imageProcessor.saveBitmap(bitmap, sigFile, ExportImageFormat.PNG, 100)
            preferencesManager.setSavedSignaturePath(sigFile.absolutePath)
            _uiState.update {
                it.copy(
                    signaturePreviewPath = sigFile.absolutePath,
                    statusMessage = "Signature saved"
                )
            }
        }
    }

    fun executeSignPdf(onCreated: (Long) -> Unit) {
        val state = _uiState.value
        val docId = state.selectedSignDocId ?: return
        val sigPath = state.signaturePreviewPath
        if (sigPath.isBlank() || !File(sigPath).exists()) {
            _uiState.update { it.copy(statusMessage = "Please draw your signature first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Applying digital signature…") }
            val sigBitmap = repository.imageProcessor.decodeSampledBitmapFromFile(sigPath, 900)
            if (sigBitmap == null) {
                _uiState.update { it.copy(isProcessing = false, statusMessage = "Could not load signature") }
                return@launch
            }
            val signedId = repository.signSavedDocument(
                documentId = docId,
                pageIndex = state.signTargetPageIndex,
                signatureBitmap = sigBitmap,
                centerXNorm = state.signPosX,
                centerYNorm = state.signPosY,
                widthNorm = state.signWidthNorm
            )
            sigBitmap.recycle()
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = if (signedId != null) "Signed PDF created!" else "Signing failed"
                )
            }
            if (signedId != null) onCreated(signedId)
        }
    }

    // --- OCR TEXT SCANNER ---
    fun runOcrOnImageUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Extracting text with ML Kit…") }
            val tempFile = File(repository.scanCacheDir, "ocr_src_${System.currentTimeMillis()}.jpg")
            val copied = repository.imageProcessor.copyUriToLocalFile(uri, tempFile, 2000)
            if (!copied) {
                _uiState.update { it.copy(isProcessing = false, statusMessage = "Could not load image") }
                return@launch
            }
            runOcrOnLocalFile(tempFile.absolutePath)
        }
    }

    fun runOcrOnSavedDocument(documentId: Long) {
        viewModelScope.launch {
            val doc = repository.getDocumentWithPages(documentId) ?: return@launch
            val firstPage = doc.orderedPages.firstOrNull()?.imagePath ?: return@launch
            runOcrOnLocalFile(firstPage, documentId)
        }
    }

    fun runOcrOnLocalFile(filePath: String, saveToDocId: Long? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingLabel = "Scanning text with ML Kit…",
                    ocrSourceImagePath = filePath
                )
            }
            val bmp = repository.imageProcessor.decodeSampledBitmapFromFile(filePath, 2000)
            if (bmp == null) {
                _uiState.update { it.copy(isProcessing = false, statusMessage = "Unable to decode image") }
                return@launch
            }
            val res = repository.ocrEngine.extractTextFromBitmap(bmp)
            bmp.recycle()
            res.onSuccess { ocr ->
                if (saveToDocId != null && ocr.fullText.isNotBlank()) {
                    repository.updateDocumentOcrText(saveToDocId, ocr.fullText)
                }
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        ocrExtractedText = ocr.fullText,
                        ocrWordCount = ocr.wordCount,
                        statusMessage = if (ocr.fullText.isBlank()) "No readable text found" else "Extracted ${ocr.wordCount} words"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isProcessing = false, statusMessage = "OCR failed: ${err.localizedMessage ?: "Unknown"}")
                }
            }
        }
    }

    fun updateOcrEditedText(text: String) {
        val words = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        _uiState.update { it.copy(ocrExtractedText = text, ocrWordCount = words) }
    }

    fun importExternalPdfForCurrentTool(uri: Uri, name: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingLabel = "Importing PDF…") }
            val docId = repository.importExternalPdfUri(uri, name)
            _uiState.update { it.copy(isProcessing = false) }
            if (docId != null) {
                when (_uiState.value.activeTool) {
                    ActivePdfTool.MERGE -> toggleMergeDoc(docId)
                    ActivePdfTool.SPLIT -> selectDocumentForSplit(docId)
                    ActivePdfTool.COMPRESS -> selectDocumentForCompress(docId)
                    ActivePdfTool.PDF_TO_IMAGES -> selectDocumentForExport(docId)
                    ActivePdfTool.SIGN -> selectDocumentForSign(docId)
                    ActivePdfTool.OCR -> runOcrOnSavedDocument(docId)
                    ActivePdfTool.HUB -> {}
                }
            } else {
                _uiState.update { it.copy(statusMessage = "Could not import PDF") }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PdfToolsViewModel(container.documentRepository, container.preferencesManager) as T
                }
            }
    }
}
