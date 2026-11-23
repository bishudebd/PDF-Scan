package com.example.config

import androidx.annotation.DrawableRes
import com.example.BuildConfig
import com.example.R

/**
 * Centralized profile configuration for the Founder & Developer.
 * Edit values in [AppAboutConfig.current] to update founder identity, photo drawable,
 * organization, website, country, or support email across the entire application.
 */
data class FounderProfile(
    val fullName: String,
    val designation: String,
    val organizationName: String,
    val organizationHeadline: String,
    val bioDescription: String,
    val websiteUrl: String,
    val country: String,
    val contactEmail: String,
    @DrawableRes val avatarRes: Int = R.drawable.ic_founder_avatar_placeholder
)

/**
 * Structured privacy policy section rendered on the Privacy Policy screen.
 */
data class PrivacySection(
    val title: String,
    val description: String
)

/**
 * Centralized "About PDF Scan" configuration combining editable branding,
 * founder details, Play Store / share links, and runtime version/build info from [BuildConfig].
 */
data class AppAboutInfo(
    val appName: String,
    val tagline: String,
    val aboutDescription: String,
    val versionName: String,
    val buildNumber: Int,
    val packageId: String,
    val playStoreUrl: String,
    val privacyPolicyUrl: String,
    val founder: FounderProfile,
    val privacySections: List<PrivacySection>
) {
    val formattedVersionLine: String
        get() = "Version $versionName (Build $buildNumber)"

    val shareText: String
        get() = "Try $appName — $tagline. Scan, edit, convert and manage PDFs securely on Android.\n$playStoreUrl\nMore tools: ${founder.websiteUrl}"
}

/**
 * Single source of truth for all Settings -> About PDF Scan, Contact, Share,
 * Rate Us, and Privacy Policy details.
 */
object AppAboutConfig {
    val current: AppAboutInfo = AppAboutInfo(
        appName = "PDF Scan",
        tagline = "Scan \u2022 Edit \u2022 Convert \u2022 Manage PDFs",
        aboutDescription = "PDF Scan is a powerful and easy-to-use document scanner and PDF tool to scan, edit, convert and manage your PDF files securely on your Android device.",
        versionName = BuildConfig.VERSION_NAME,
        buildNumber = BuildConfig.VERSION_CODE,
        packageId = BuildConfig.APPLICATION_ID,
        playStoreUrl = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
        privacyPolicyUrl = "https://netoday.in/privacy-policy",
        founder = FounderProfile(
            fullName = "Bishudeb Debbarma",
            designation = "Founder & Developer",
            organizationName = "NEToday.in",
            organizationHeadline = "Founder of NEToday.in",
            bioDescription = "Building useful digital tools for everyone.",
            websiteUrl = "https://netoday.in",
            country = "India",
            contactEmail = "support@netoday.in",
            avatarRes = R.drawable.ic_founder_avatar_placeholder
        ),
        privacySections = listOf(
            PrivacySection(
                title = "100% On-Device Processing",
                description = "All document scanning, edge detection, perspective cropping, image enhancement, OCR text recognition, PDF merging, splitting, compression, and signing are performed locally on your Android device."
            ),
            PrivacySection(
                title = "No Mandatory Account or Tracking",
                description = "PDF Scan works offline without requiring sign-in or personal registration. Your scanned documents remain in private app storage unless you choose to export, back up, or share them."
            ),
            PrivacySection(
                title = "Camera & Media Permissions",
                description = "Camera permission is requested solely to capture document pages and ID cards. Importing images uses Android's zero-permission Photo Picker so PDF Scan never requests broad gallery access."
            ),
            PrivacySection(
                title = "Backup & Data Control",
                description = "When you use Backup & Restore, backups are saved directly to the location you choose via Android's system file picker. You stay in complete control of your files at all times."
            ),
            PrivacySection(
                title = "Contact & Privacy Requests",
                description = "If you have questions about how PDF Scan protects your data, contact support@netoday.in or visit https://netoday.in."
            )
        )
    )
}
