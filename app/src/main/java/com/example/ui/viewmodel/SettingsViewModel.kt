package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.PreferencesManager
import com.example.data.local.UserSettings
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfQuality
import com.example.data.model.ScanFilter
import com.example.data.repository.DocumentRepository
import com.example.data.repository.StorageStats
import com.example.engine.FileShareHelper
import com.example.engine.LocalBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val repository: DocumentRepository,
    private val backupManager: LocalBackupManager? = null
) : ViewModel() {

    val settings: StateFlow<UserSettings> = preferencesManager.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _storageStats = MutableStateFlow(StorageStats(0, 0L, 0L))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.computeStorageStats()
        }
    }

    fun setDefaultFilter(filter: ScanFilter) {
        viewModelScope.launch { preferencesManager.setDefaultFilter(filter) }
    }

    fun setDefaultPdfQuality(quality: PdfQuality) {
        viewModelScope.launch { preferencesManager.setDefaultPdfQuality(quality) }
    }

    fun setDefaultPageSize(size: PageSizeOption) {
        viewModelScope.launch { preferencesManager.setDefaultPageSize(size) }
    }

    fun setAutoEdgeDetection(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoEdgeDetection(enabled) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDarkThemeEnabled(enabled) }
    }

    fun clearScanCache() {
        viewModelScope.launch {
            val freed = repository.clearScanCache()
            refreshStorageStats()
            _message.value = "Cleared ${FileShareHelper.formatFileSize(freed)} of temporary scan cache"
        }
    }

    fun exportBackup(destinationUri: Uri) {
        val manager = backupManager ?: return
        if (_isBackupBusy.value) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            val result = manager.exportBackupToUri(destinationUri)
            _isBackupBusy.value = false
            result.onSuccess { summary ->
                _message.value = if (summary.exportedDocuments == 0) {
                    "Backup archive created (0 scanned documents found)"
                } else {
                    "Backed up ${summary.exportedDocuments} document(s) (${FileShareHelper.formatFileSize(summary.totalBytes)})"
                }
            }.onFailure { err ->
                _message.value = "Backup failed: ${err.localizedMessage ?: "Unable to save file"}"
            }
        }
    }

    fun restoreBackup(sourceUri: Uri) {
        val manager = backupManager ?: return
        if (_isBackupBusy.value) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            val result = manager.restoreBackupFromUri(sourceUri)
            _isBackupBusy.value = false
            refreshStorageStats()
            result.onSuccess { summary ->
                _message.value = if (summary.restoredDocuments == 0) {
                    "No PDF documents found in the selected backup file"
                } else {
                    "Restored ${summary.restoredDocuments} document(s) to My Documents"
                }
            }.onFailure { err ->
                _message.value = "Restore failed: ${err.localizedMessage ?: "Invalid backup archive"}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        preferencesManager = container.preferencesManager,
                        repository = container.documentRepository,
                        backupManager = container.backupManager
                    ) as T
                }
            }
    }
}
