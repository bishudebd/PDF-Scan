package com.example

import com.example.config.AppAboutConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutAppConfigTest {

    @Test
    fun `centralized about config contains accurate founder and app metadata`() {
        val info = AppAboutConfig.current
        assertEquals("PDF Scan", info.appName)
        assertEquals("Scan \u2022 Edit \u2022 Convert \u2022 Manage PDFs", info.tagline)
        assertEquals(
            "PDF Scan is a powerful and easy-to-use document scanner and PDF tool to scan, edit, convert and manage your PDF files securely on your Android device.",
            info.aboutDescription
        )
        assertEquals("Bishudeb Debbarma", info.founder.fullName)
        assertEquals("Founder & Developer", info.founder.designation)
        assertEquals("NEToday.in", info.founder.organizationName)
        assertEquals("Founder of NEToday.in", info.founder.organizationHeadline)
        assertEquals("Building useful digital tools for everyone.", info.founder.bioDescription)
        assertEquals("https://netoday.in", info.founder.websiteUrl)
        assertEquals("India", info.founder.country)
        assertEquals("support@netoday.in", info.founder.contactEmail)
        assertEquals(BuildConfig.VERSION_NAME, info.versionName)
        assertEquals(BuildConfig.VERSION_CODE, info.buildNumber)
        assertEquals(
            "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
            info.formattedVersionLine
        )
        assertEquals(
            "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
            info.playStoreUrl
        )
        assertTrue(info.shareText.contains("PDF Scan"))
        assertTrue(info.shareText.contains("https://netoday.in"))
        assertTrue(info.privacyPolicyUrl.startsWith("https://"))
        assertTrue(info.privacySections.isNotEmpty())
    }
}
