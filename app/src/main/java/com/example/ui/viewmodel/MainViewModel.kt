package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.FolderEntity
import com.example.data.local.FolderWithCount
import com.example.data.local.PreferencesManager
import com.example.data.local.ScannedDocumentEntity
import com.example.data.local.UserSettings
import com.example.data.model.DocumentSortOrder
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: DocumentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val userSettings: StateFlow<UserSettings?> = preferencesManager.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentDocuments: StateFlow<List<ScannedDocumentEntity>> =
        repository.observeRecentDocuments(limit = 6)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<ScannedDocumentEntity>> =
        repository.observeAllDocuments()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersWithCount: StateFlow<List<FolderWithCount>> =
        repository.observeFoldersWithCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<FolderEntity>> =
        repository.observeAllFolders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(DocumentSortOrder.NEWEST)
    val sortOrder: StateFlow<DocumentSortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val filteredDocuments: StateFlow<List<ScannedDocumentEntity>> = combine(
        allDocuments,
        _searchQuery,
        _sortOrder,
        _selectedFolderId
    ) { docs, query, sort, folderId ->
        val folderFiltered = if (folderId != null) {
            docs.filter { it.folderId == folderId }
        } else {
            docs
        }
        val queryFiltered = if (query.isBlank()) {
            folderFiltered
        } else {
            val q = query.trim().lowercase()
            folderFiltered.filter {
                it.title.lowercase().contains(q) || it.ocrText.lowercase().contains(q)
            }
        }
        when (sort) {
            DocumentSortOrder.NEWEST -> queryFiltered.sortedByDescending { it.updatedAt }
            DocumentSortOrder.OLDEST -> queryFiltered.sortedBy { it.updatedAt }
            DocumentSortOrder.NAME_ASC -> queryFiltered.sortedBy { it.title.lowercase() }
            DocumentSortOrder.SIZE_DESC -> queryFiltered.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: DocumentSortOrder) {
        _sortOrder.value = order
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun renameDocument(documentId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(documentId, newTitle)
            _statusMessage.value = "Document renamed"
        }
    }

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
            _statusMessage.value = "Document deleted"
        }
    }

    fun moveDocumentToFolder(documentId: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.moveDocumentToFolder(documentId, folderId)
            _statusMessage.value = if (folderId == null) "Removed from folder" else "Moved to folder"
        }
    }

    fun createFolder(name: String, colorArgb: Long) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createFolder(name, colorArgb)
                _statusMessage.value = "Folder \"$name\" created"
            }
        }
    }

    fun updateFolder(folderId: Long, name: String, colorArgb: Long) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.updateFolder(folderId, name, colorArgb)
                _statusMessage.value = "Folder updated"
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) _selectedFolderId.value = null
            _statusMessage.value = "Folder deleted"
        }
    }

    fun importExternalPdf(uri: Uri, displayName: String?, onImported: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.importExternalPdfUri(uri, displayName)
            if (id != null) {
                _statusMessage.value = "PDF imported to My Documents"
                onImported(id)
            } else {
                _statusMessage.value = "Could not import selected PDF"
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(container.documentRepository, container.preferencesManager) as T
                }
            }
    }
}
