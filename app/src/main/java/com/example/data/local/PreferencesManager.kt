package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfQuality
import com.example.data.model.ScanFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_scan_prefs")

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val defaultFilter: ScanFilter = ScanFilter.ENHANCED,
    val defaultPdfQuality: PdfQuality = PdfQuality.HIGH,
    val defaultPageSize: PageSizeOption = PageSizeOption.A4,
    val autoEdgeDetection: Boolean = true,
    val darkThemeEnabled: Boolean = false,
    val savedSignaturePath: String = ""
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEFAULT_FILTER = stringPreferencesKey("default_filter")
        val DEFAULT_PDF_QUALITY = stringPreferencesKey("default_pdf_quality")
        val DEFAULT_PAGE_SIZE = stringPreferencesKey("default_page_size")
        val AUTO_EDGE_DETECTION = booleanPreferencesKey("auto_edge_detection")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SAVED_SIGNATURE_PATH = stringPreferencesKey("saved_signature_path")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            defaultFilter = ScanFilter.fromName(prefs[Keys.DEFAULT_FILTER] ?: ScanFilter.ENHANCED.name),
            defaultPdfQuality = PdfQuality.fromName(prefs[Keys.DEFAULT_PDF_QUALITY] ?: PdfQuality.HIGH.name),
            defaultPageSize = PageSizeOption.fromName(prefs[Keys.DEFAULT_PAGE_SIZE] ?: PageSizeOption.A4.name),
            autoEdgeDetection = prefs[Keys.AUTO_EDGE_DETECTION] ?: true,
            darkThemeEnabled = prefs[Keys.DARK_THEME] ?: false,
            savedSignaturePath = prefs[Keys.SAVED_SIGNATURE_PATH] ?: ""
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setDefaultFilter(filter: ScanFilter) {
        context.dataStore.edit { it[Keys.DEFAULT_FILTER] = filter.name }
    }

    suspend fun setDefaultPdfQuality(quality: PdfQuality) {
        context.dataStore.edit { it[Keys.DEFAULT_PDF_QUALITY] = quality.name }
    }

    suspend fun setDefaultPageSize(size: PageSizeOption) {
        context.dataStore.edit { it[Keys.DEFAULT_PAGE_SIZE] = size.name }
    }

    suspend fun setAutoEdgeDetection(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_EDGE_DETECTION] = enabled }
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setSavedSignaturePath(path: String) {
        context.dataStore.edit { it[Keys.SAVED_SIGNATURE_PATH] = path }
    }
}
