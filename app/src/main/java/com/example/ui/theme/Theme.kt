package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val PdfScanLightColorScheme = lightColorScheme(
    primary = CobaltBlue,
    onPrimary = PureWhite,
    primaryContainer = CobaltBlueContainer,
    onPrimaryContainer = DeepNavy,
    secondary = MidnightNavy,
    onSecondary = PureWhite,
    secondaryContainer = SoftIceSurface,
    onSecondaryContainer = DeepNavy,
    tertiary = AccentTeal,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = DeepNavy,
    background = PureWhite,
    onBackground = DeepNavy,
    surface = PureWhite,
    onSurface = DeepNavy,
    surfaceVariant = AlabasterSurface,
    onSurfaceVariant = SlateInk,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = AlabasterSurface,
    surfaceContainer = SoftIceSurface,
    surfaceContainerHigh = Color(0xFFE8EEF8),
    surfaceContainerHighest = Color(0xFFDFE7F4),
    outline = SubtleBorder,
    outlineVariant = HairlineDivider,
    error = AccentCrimson,
    onError = PureWhite,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val PdfScanDarkColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF93C5FD),
    onSecondary = DeepNavy,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF38BDF8),
    onTertiary = DeepNavy,
    background = DarkCanvas,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = DarkCanvas,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val PdfScanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PdfScanTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PdfScanDarkColorScheme else PdfScanLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PdfScanTypography,
        shapes = PdfScanShapes,
        content = content
    )
}
