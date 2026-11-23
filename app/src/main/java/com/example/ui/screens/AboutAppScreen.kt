package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.config.AppAboutConfig
import com.example.config.AppAboutInfo
import com.example.config.FounderProfile
import kotlinx.coroutines.launch

private val AboutAccentBlue = Color(0xFF1D6CF2)
private val AboutBadgeStart = Color(0xFF5A6DF8)
private val AboutBadgeEnd = Color(0xFF7B6AF6)
private val AboutCardBorderLight = Color(0xFFE6EDF7)
private val AboutDividerLight = Color(0xFFEEF2F8)
private val AboutInfoCardBgLight = Color(0xFFEFF5FE)
private val AboutInfoCardBorderLight = Color(0xFFD9E6F9)
private val AboutDarkHeading = Color(0xFF0B162C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBack: () -> Unit,
    aboutInfo: AppAboutInfo = AppAboutConfig.current
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    fun showFeedback(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    SettingsSoftBlueBackdrop(isDark = isDark, showHeroArcs = true) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("about_pdf_scan_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.about_screen_title),
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
                            modifier = Modifier.testTag("about_back_button")
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
                            Color(0xFFF3F6FC).copy(alpha = 0.92f)
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
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Top Application Information Hero
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_pdf_scan_mark),
                        contentDescription = aboutInfo.appName,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("about_app_logo")
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = aboutInfo.appName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = if (isDark) MaterialTheme.colorScheme.onBackground else AboutDarkHeading,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("about_app_name")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = aboutInfo.formattedVersionLine,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("about_version_build")
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = aboutInfo.tagline,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("about_tagline")
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // 2. Main White Sheet Container holding Founder & Developer + About the App
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDark) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        } else {
                            Color.White.copy(alpha = 0.96f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isDark) MaterialTheme.colorScheme.outline else AboutCardBorderLight
                        ),
                        shadowElevation = if (isDark) 0.dp else 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Section Heading: Founder & Developer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = AboutAccentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(R.string.about_founder_section_title),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = if (isDark) MaterialTheme.colorScheme.onSurface else AboutDarkHeading
                                )
                            }

                            // 3 & 4. Founder Profile + Contact Information Card
                            FounderAndContactCard(
                                founder = aboutInfo.founder,
                                isDark = isDark,
                                onWebsiteClick = {
                                    SettingsSystemActions.openUrl(
                                        context = context,
                                        url = aboutInfo.founder.websiteUrl,
                                        onError = {
                                            showFeedback("Visit ${aboutInfo.founder.websiteUrl} in your browser")
                                        }
                                    )
                                },
                                onContactClick = {
                                    SettingsSystemActions.contactDeveloper(
                                        context = context,
                                        aboutInfo = aboutInfo,
                                        onFallbackCopied = {
                                            showFeedback("Email copied: ${aboutInfo.founder.contactEmail}")
                                        }
                                    )
                                }
                            )

                            // 5. Separate Rounded "About the App" Card
                            AboutTheAppInfoCard(
                                description = aboutInfo.aboutDescription,
                                isDark = isDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FounderAndContactCard(
    founder: FounderProfile,
    isDark: Boolean,
    onWebsiteClick: () -> Unit,
    onContactClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outline else AboutCardBorderLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("founder_profile_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Founder Bio Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = founder.avatarRes),
                    contentDescription = stringResource(R.string.about_founder_avatar_cd, founder.fullName),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFDCE6F4),
                            shape = CircleShape
                        )
                        .testTag("founder_avatar_image")
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = founder.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else AboutDarkHeading,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("founder_name_text")
                    )

                    // Blue-Purple Gradient Designation Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AboutBadgeStart, AboutBadgeEnd)
                                )
                            )
                            .padding(horizontal = 11.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = founder.designation,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("founder_designation_badge")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = founder.organizationHeadline,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("founder_organization_text")
                        )
                        Text(
                            text = founder.bioDescription,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("founder_bio_text")
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.9.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant else AboutDividerLight
            )

            // Contact Row 1: Website (clickable)
            FounderContactRow(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.about_website_label),
                isDark = isDark,
                testTag = "founder_website_row",
                onClick = onWebsiteClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = founder.websiteUrl,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = AboutAccentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.about_open_website_cd),
                        tint = AboutAccentBlue,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.8.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant else AboutDividerLight
            )

            // Contact Row 2: Country (India with clean drawn tricolor badge)
            FounderContactRow(
                icon = Icons.Outlined.LocationOn,
                label = stringResource(R.string.about_country_label),
                isDark = isDark,
                testTag = "founder_country_row",
                onClick = null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (founder.country.equals("India", ignoreCase = true)) {
                        IndiaMiniFlagBadge()
                    }
                    Text(
                        text = founder.country,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.8.dp,
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant else AboutDividerLight
            )

            // Contact Row 3: Support Email (clickable)
            FounderContactRow(
                icon = Icons.Outlined.Email,
                label = stringResource(R.string.about_contact_label),
                isDark = isDark,
                testTag = "founder_contact_row",
                onClick = onContactClick
            ) {
                Text(
                    text = founder.contactEmail,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp
                    ),
                    color = AboutAccentBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FounderContactRow(
    icon: ImageVector,
    label: String,
    isDark: Boolean,
    testTag: String,
    onClick: (() -> Unit)?,
    valueContent: @Composable () -> Unit
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
        .padding(horizontal = 14.dp, vertical = 11.dp)
        .testTag(testTag)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) MaterialTheme.colorScheme.onSurface else AboutDarkHeading,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(74.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f)) {
            valueContent()
        }
    }
}

@Composable
private fun AboutTheAppInfoCard(
    description: String,
    isDark: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                AboutInfoCardBgLight
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outline else AboutInfoCardBorderLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("about_the_app_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = AboutAccentBlue,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.about_the_app_heading),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else AboutDarkHeading
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}

/**
 * Clean vector-drawn mini India tricolor badge matching the Country row in the reference UI.
 */
@Composable
private fun IndiaMiniFlagBadge() {
    Canvas(
        modifier = Modifier
            .size(width = 20.dp, height = 14.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, Color(0xFFD1D9E6), RoundedCornerShape(2.dp))
    ) {
        val bandH = size.height / 3f
        drawRect(color = Color(0xFFFF9933), topLeft = Offset.Zero, size = Size(size.width, bandH))
        drawRect(color = Color.White, topLeft = Offset(0f, bandH), size = Size(size.width, bandH))
        drawRect(color = Color(0xFF138808), topLeft = Offset(0f, bandH * 2f), size = Size(size.width, bandH))
        drawCircle(
            color = Color(0xFF000080),
            radius = bandH * 0.34f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}
