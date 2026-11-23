package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.config.AppAboutConfig
import com.example.config.AppAboutInfo
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfQuality
import com.example.data.model.ScanFilter
import com.example.engine.FileShareHelper
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CobaltBlue
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SubScreenLightBorder = Color(0xFFE7EEF8)

/**
 * Settings -> App Settings:
 * Scanner defaults, PDF output defaults, and local device storage controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(Unit) { viewModel.refreshStorageStats() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    SettingsSoftBlueBackdrop(isDark = isDark) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("app_settings_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                SubSettingsTopBar(
                    title = stringResource(R.string.settings_item_app_settings),
                    onBack = onBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SettingsSurfaceCard(isDark = isDark) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.DocumentScanner, null, tint = CobaltBlue)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Scanner & PDF Defaults", style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Automatic Edge Detection", style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = "Automatically detect paper corners when capturing.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = settings.autoEdgeDetection,
                                        onCheckedChange = { viewModel.setAutoEdgeDetection(it) },
                                        modifier = Modifier.testTag("setting_auto_edge_switch")
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Default Scan Filter", style = MaterialTheme.typography.titleSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(ScanFilter.entries) { filter ->
                                            FilterChip(
                                                selected = settings.defaultFilter == filter,
                                                onClick = { viewModel.setDefaultFilter(filter) },
                                                label = { Text(filter.shortLabel) }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Default PDF Quality", style = MaterialTheme.typography.titleSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(PdfQuality.entries) { quality ->
                                            FilterChip(
                                                selected = settings.defaultPdfQuality == quality,
                                                onClick = { viewModel.setDefaultPdfQuality(quality) },
                                                label = { Text(quality.label) }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Default Page Size", style = MaterialTheme.typography.titleSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(PageSizeOption.entries) { size ->
                                            FilterChip(
                                                selected = settings.defaultPageSize == size,
                                                onClick = { viewModel.setDefaultPageSize(size) },
                                                label = { Text(size.name) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SettingsSurfaceCard(isDark = isDark) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Storage, null, tint = CobaltBlue)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Local Device Storage", style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saved Documents & Exports", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        FileShareHelper.formatFileSize(storageStats.totalPdfBytes),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CobaltBlue
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Temporary Scan Cache", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        FileShareHelper.formatFileSize(storageStats.totalCacheBytes),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.clearScanCache() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("clear_cache_button")
                                ) {
                                    Icon(Icons.Filled.CleaningServices, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Clear Temporary Scan Cache")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Settings -> Appearance:
 * Light & Dark mode selector persisted via DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    SettingsSoftBlueBackdrop(isDark = isDark) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("appearance_settings_screen"),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                SubSettingsTopBar(
                    title = stringResource(R.string.settings_item_appearance),
                    onBack = onBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsSurfaceCard(isDark = isDark) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ThemeOptionRow(
                                icon = Icons.Filled.LightMode,
                                title = "Light Theme",
                                subtitle = "Clean white cards with subtle blue highlights",
                                selected = !settings.darkThemeEnabled,
                                testTag = "appearance_light_option",
                                onClick = { viewModel.setDarkTheme(false) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            ThemeOptionRow(
                                icon = Icons.Filled.DarkMode,
                                title = "Dark Theme",
                                subtitle = "Deep midnight theme for low-light scanning",
                                selected = settings.darkThemeEnabled,
                                testTag = "appearance_dark_option",
                                onClick = { viewModel.setDarkTheme(true) }
                            )
                        }
                    }

                    SettingsSurfaceCard(isDark = isDark) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable Dark Mode", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Applies immediately across all screens",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.darkThemeEnabled,
                                onCheckedChange = { viewModel.setDarkTheme(it) },
                                modifier = Modifier.testTag("setting_dark_theme_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) CobaltBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = CobaltBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Settings -> Backup & Restore:
 * Real SAF ZIP backup export and restore for local scanned PDF documents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBackupBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(Unit) { viewModel.refreshStorageStats() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackup(uri)
        }
    }

    SettingsSoftBlueBackdrop(isDark = isDark) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("backup_restore_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                SubSettingsTopBar(
                    title = stringResource(R.string.settings_item_backup_restore),
                    onBack = onBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsSurfaceCard(isDark = isDark) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CloudUpload, null, tint = CobaltBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Local Document Backup", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "Export all saved PDF documents into a portable ZIP backup file on your device or cloud drive, and restore them anytime without needing an account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Documents Ready for Backup", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${storageStats.documentCount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CobaltBlue
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Local Size", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    FileShareHelper.formatFileSize(storageStats.totalPdfBytes),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    exportBackupLauncher.launch("PDF_Scan_Backup_$stamp.zip")
                                },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("backup_export_button")
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Backup Archive (.zip)")
                            }

                            OutlinedButton(
                                onClick = {
                                    restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("backup_restore_button")
                            ) {
                                Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore from Backup (.zip)")
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.Security, null, tint = AccentEmerald)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Private & Under Your Control",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AccentEmerald
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Backups are created directly on your device using Android's system file picker. No mandatory registration or cloud upload occurs unless you choose a cloud folder.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Settings -> Privacy Policy:
 * Dedicated Material 3 screen detailing local-first data protection + online policy link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    aboutInfo: AppAboutInfo = AppAboutConfig.current
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    SettingsSoftBlueBackdrop(isDark = isDark) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("privacy_policy_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                SubSettingsTopBar(
                    title = stringResource(R.string.settings_item_privacy_policy),
                    onBack = onBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(aboutInfo.privacySections) { section ->
                        SettingsSurfaceCard(isDark = isDark) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = section.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                SettingsSystemActions.openUrl(
                                    context = context,
                                    url = aboutInfo.privacyPolicyUrl,
                                    onError = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Policy available at ${aboutInfo.founder.websiteUrl}")
                                        }
                                    }
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("privacy_open_online_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Privacy Policy on ${aboutInfo.founder.organizationName}")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubSettingsTopBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("sub_settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsSurfaceCard(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outline else SubScreenLightBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}
