package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.DocumentWithPages
import com.example.data.local.FolderEntity
import com.example.data.model.ExportImageFormat
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PdfPreviewUiState(
    val documentWithPages: DocumentWithPages? = null,
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val busyMessage: String = "",
    val extractedOcrText: String? = null,
    val showOcrSheet: Boolean = false,
    val exportedFiles: List<File> = emptyList(),
    val exportedFormat: ExportImageFormat = ExportImageFormat.JPG,
    val message: String? = null
)

class PdfPreviewViewModel(
    private val documentId: Long,
    private val repository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfPreviewUiState())
    val uiState: StateFlow<PdfPreviewUiState> = _uiState.asStateFlow()

    val folders: StateFlow<List<FolderEntity>> = repository.observeAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.observeDocumentWithPages(documentId).collect { doc ->
                _uiState.update {
                    it.copy(
                        documentWithPages = doc,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun renameDocument(newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(documentId, newTitle)
            _uiState.update { it.copy(message = "Document renamed") }
        }
    }

    fun moveToFolder(folderId: Long?) {
        viewModelScope.launch {
            repository.moveDocumentToFolder(documentId, folderId)
            _uiState.update { it.copy(message = if (folderId == null) "Removed from folder" else "Moved to folder") }
        }
    }

    fun deleteDocument(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
            onDeleted()
        }
    }

    fun deletePage(pageId: Long, onDocumentDeleted: () -> Unit) {
        val currentPages = _uiState.value.documentWithPages?.orderedPages.orEmpty()
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Updating PDF…") }
            repository.deletePageFromDocument(documentId, pageId)
            _uiState.update { it.copy(isBusy = false) }
            if (currentPages.size <= 1) {
                onDocumentDeleted()
            } else {
                _uiState.update { it.copy(message = "Page removed") }
            }
        }
    }

    fun runOcrOnPage(pageIndex: Int) {
        val doc = _uiState.value.documentWithPages ?: return
        val page = doc.orderedPages.getOrNull(pageIndex) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Recognizing text with ML Kit…") }
            val bmp = repository.imageProcessor.decodeSampledBitmapFromFile(page.imagePath, 2000)
            if (bmp == null) {
                _uiState.update { it.copy(isBusy = false, message = "Could not decode page image") }
                return@launch
            }
            val res = repository.ocrEngine.extractTextFromBitmap(bmp)
            bmp.recycle()
            res.onSuccess { ocr ->
                if (ocr.fullText.isNotBlank()) {
                    val combinedOcr = if (doc.document.ocrText.isBlank()) {
                        ocr.fullText
                    } else if (!doc.document.ocrText.contains(ocr.fullText)) {
                        "${doc.document.ocrText}\n\n${ocr.fullText}"
                    } else {
                        doc.document.ocrText
                    }
                    repository.updateDocumentOcrText(documentId, combinedOcr)
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        extractedOcrText = ocr.fullText.ifBlank { "No text detected on this page." },
                        showOcrSheet = true
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, message = "OCR error: ${err.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun dismissOcrSheet() {
        _uiState.update { it.copy(showOcrSheet = false) }
    }

    fun exportAsImages(format: ExportImageFormat, onExported: (List<File>, ExportImageFormat) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Exporting ${format.extension.uppercase()} images…") }
            val files = repository.exportDocumentAsImages(documentId, format)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    exportedFiles = files,
                    exportedFormat = format,
                    message = if (files.isNotEmpty()) "Exported ${files.size} ${format.extension.uppercase()} file(s)" else "Export failed"
                )
            }
            if (files.isNotEmpty()) {
                onExported(files, format)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        fun factory(documentId: Long, container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PdfPreviewViewModel(documentId, container.documentRepository) as T
                }
            }
    }
}
