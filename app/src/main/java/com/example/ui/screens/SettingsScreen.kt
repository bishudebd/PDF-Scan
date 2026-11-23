package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.config.AppAboutConfig
import com.example.config.AppAboutInfo
import kotlinx.coroutines.launch

private val SettingsLightCanvas = Color(0xFFF3F6FC)
private val SettingsAccentBlue = Color(0xFF1D6CF2)
private val SettingsLightBorder = Color(0xFFE7EEF8)
private val SettingsLightDivider = Color(0xFFEFF3F9)
private val SettingsLightIconInk = Color(0xFF121B2E)

private data class SettingsMenuEntry(
    val icon: ImageVector,
    val titleRes: Int,
    val subtitleRes: Int,
    val accentIcon: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAboutApp: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    aboutInfo: AppAboutInfo = AppAboutConfig.current
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    fun showToastMessage(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    val menuItems = remember(aboutInfo) {
        listOf(
            SettingsMenuEntry(
                icon = Icons.Outlined.Settings,
                titleRes = R.string.settings_item_app_settings,
                subtitleRes = R.string.settings_item_app_settings_sub,
                testTag = "settings_row_app_settings",
                onClick = onOpenAppSettings
            ),
            SettingsMenuEntry(
                icon = Icons.Outlined.Contrast,
                titleRes = R.string.settings_item_appearance,
                subtitleRes = R.string.settings_item_appearance_sub,
                testTag = "settings_row_appearance",
                onClick = onOpenAppearance
            ),
            SettingsMenuEntry(
                icon = Icons.Outlined.CloudUpload,
                titleRes = R.string.settings_item_backup_restore,
                subtitleRes = R.string.settings_item_backup_restore_sub,
                testTag = "settings_row_backup_restore",
                onClick = onOpenBackupRestore
            ),
            SettingsMenuEntry(
                icon = Icons.Outlined.Info,
                titleRes = R.string.settings_item_about_app,
                subtitleRes = R.string.settings_item_about_app_sub,
                accentIcon = true,
                testTag = "settings_row_about_app",
                onClick = onOpenAboutApp
            ),
            SettingsMenuEntry(
                icon = Icons.Outlined.Email,
                titleRes = R.string.settings_item_contact_developer,
                subtitleRes = R.string.settings_item_contact_developer_sub,
                testTag = "settings_row_contact_developer",
                onClick = {
                    SettingsSystemActions.contactDeveloper(
                        context = context,
                        aboutInfo = aboutInfo,
                        onFallbackCopied = {
                            showToastMessage("Email copied: ${aboutInfo.founder.contactEmail}")
                        }
                    )
                }
            ),
            SettingsMenuEntry(
                icon = Icons.Outlined.VerifiedUser,
                titleRes = R.string.settings_item_privacy_policy,
                subtitleRes = R.string.settings_item_privacy_policy_sub,
                testTag = "settings_row_privacy_policy",
                onClick = onOpenPrivacyPolicy
            ),
            SettingsMenuEntry(
                icon = Icons.Outlined.StarOutline,
                titleRes = R.string.settings_item_rate_us,
                subtitleRes = R.string.settings_item_rate_us_sub,
                testTag = "settings_row_rate_us",
                onClick = {
                    SettingsSystemActions.openPlayStoreListing(
                        context = context,
                        aboutInfo = aboutInfo,
                        onError = {
                            showToastMessage("Google Play listing will open once published")
                        }
                    )
                }
            ),
            SettingsMenuEntry(
                icon = Icons.Outlined.Share,
                titleRes = R.string.settings_item_share_app,
                subtitleRes = R.string.settings_item_share_app_sub,
                testTag = "settings_row_share_app",
                onClick = {
                    SettingsSystemActions.shareApp(
                        context = context,
                        aboutInfo = aboutInfo,
                        onError = { showToastMessage("Unable to open share menu") }
                    )
                }
            )
        )
    }

    SettingsSoftBlueBackdrop(isDark = isDark) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.nav_settings),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = if (isDark) {
                            MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                        } else {
                            SettingsLightCanvas.copy(alpha = 0.92f)
                        }
                    )
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
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isDark) MaterialTheme.colorScheme.outline else SettingsLightBorder
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_menu_card")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            menuItems.forEachIndexed { index, item ->
                                SettingsMenuItemRow(
                                    icon = item.icon,
                                    title = stringResource(item.titleRes),
                                    subtitle = stringResource(item.subtitleRes),
                                    iconTint = when {
                                        item.accentIcon -> SettingsAccentBlue
                                        isDark -> MaterialTheme.colorScheme.onSurface
                                        else -> SettingsLightIconInk
                                    },
                                    testTag = item.testTag,
                                    onClick = item.onClick
                                )
                                if (index < menuItems.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.8.dp,
                                        color = if (isDark) {
                                            MaterialTheme.colorScheme.outlineVariant
                                        } else {
                                            SettingsLightDivider
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Shared light background with subtle curved blue gradient accents matching the reference UI.
 */
@Composable
internal fun SettingsSoftBlueBackdrop(
    isDark: Boolean,
    showHeroArcs: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColor = if (isDark) MaterialTheme.colorScheme.background else SettingsLightCanvas
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseColor)
            .drawBehind {
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1D4ED8).copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.14f),
                            radius = size.width * 0.75f
                        ),
                        radius = size.width * 0.75f,
                        center = Offset(size.width * 0.85f, size.height * 0.14f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2563EB).copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.12f, size.height * 0.78f),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = Offset(size.width * 0.12f, size.height * 0.78f)
                    )
                } else {
                    if (showHeroArcs) {
                        // Top-right soft blue arc behind the About PDF Scan hero
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFC8E0FF).copy(alpha = 0.65f), Color(0xFFE6F0FF).copy(alpha = 0.0f)),
                                center = Offset(size.width * 0.92f, size.height * 0.18f),
                                radius = size.width * 0.72f
                            ),
                            radius = size.width * 0.72f,
                            center = Offset(size.width * 0.92f, size.height * 0.18f)
                        )
                        // Mid-left soft blue wave behind the hero tagline
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFD2E5FF).copy(alpha = 0.58f), Color(0xFFEBF3FF).copy(alpha = 0.0f)),
                                center = Offset(size.width * 0.08f, size.height * 0.28f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width * 0.08f, size.height * 0.28f)
                        )
                    } else {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFDCEBFF).copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(size.width * 0.90f, size.height * 0.08f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width * 0.90f, size.height * 0.08f)
                        )
                    }
                    // Subtle lower ambient sky-blue wash
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFD9E9FF).copy(alpha = 0.40f), Color.Transparent),
                            center = Offset(size.width * 0.50f, size.height * 0.95f),
                            radius = size.width * 0.85f
                        ),
                        radius = size.width * 0.85f,
                        center = Offset(size.width * 0.50f, size.height * 0.95f)
                    )
                }
            }
    ) {
        content()
    }
}

/**
 * Shared real Android Intent helpers used across Settings and About PDF Scan screens.
 */
internal object SettingsSystemActions {
    fun openUrl(context: Context, url: String, onError: () -> Unit) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            onError()
        }
    }

    fun contactDeveloper(
        context: Context,
        aboutInfo: AppAboutInfo,
        onFallbackCopied: () -> Unit
    ) {
        val subject = "${aboutInfo.appName} ${aboutInfo.formattedVersionLine} - Support & Feedback"
        val mailUri = Uri.parse(
            "mailto:${Uri.encode(aboutInfo.founder.contactEmail)}?subject=${Uri.encode(subject)}"
        )
        val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                copyTextToClipboard(context, "Support Email", aboutInfo.founder.contactEmail)
                onFallbackCopied()
            }
        } catch (_: Exception) {
            copyTextToClipboard(context, "Support Email", aboutInfo.founder.contactEmail)
            onFallbackCopied()
        }
    }

    fun openPlayStoreListing(
        context: Context,
        aboutInfo: AppAboutInfo,
        onError: () -> Unit
    ) {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${aboutInfo.packageId}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            openUrl(context, aboutInfo.playStoreUrl, onError)
        }
    }

    fun shareApp(
        context: Context,
        aboutInfo: AppAboutInfo,
        onError: () -> Unit
    ) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, aboutInfo.appName)
                putExtra(Intent.EXTRA_TEXT, aboutInfo.shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share ${aboutInfo.appName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            onError()
        }
    }

    fun copyTextToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
