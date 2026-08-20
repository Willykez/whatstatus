package com.willykez.wastatus.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand seed — used to build the static fallback scheme on devices/API
// levels where Material You dynamic color isn't available (< Android 12
// or the user turned it off in Settings).
val BrandSeed = Color(0xFF6750A4)

val StaticLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DEF8),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E3FD),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

val StaticDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCBC2DB),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF001D35),
    onSecondaryContainer = Color(0xFFD3E3FD),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B)
)

/**
 * Extra semantic roles Material 3's base ColorScheme doesn't provide
 * (success / warning / vault-accent states), kept theme- and
 * dark/light-aware so badges, cleaner completion states, and the Vault's
 * accent adapt with everything else.
 */
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val vaultAccent: Color,
    val onVaultAccent: Color,
    val vaultAccentContainer: Color,
    val onVaultAccentContainer: Color
)

val LightExtendedColors = ExtendedColors(
    success = Color(0xFF146C2E),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFA6F5B4),
    onSuccessContainer = Color(0xFF00210A),
    warning = Color(0xFF8A5300),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDDB3),
    onWarningContainer = Color(0xFF2A1700),
    vaultAccent = Color(0xFF8E4A9E),
    onVaultAccent = Color(0xFFFFFFFF),
    vaultAccentContainer = Color(0xFFF8D8FF),
    onVaultAccentContainer = Color(0xFF390740)
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF8BD99C),
    onSuccess = Color(0xFF00391A),
    successContainer = Color(0xFF00522B),
    onSuccessContainer = Color(0xFFA6F5B4),
    warning = Color(0xFFFFB86B),
    onWarning = Color(0xFF472A00),
    warningContainer = Color(0xFF663D00),
    onWarningContainer = Color(0xFFFFDDB3),
    vaultAccent = Color(0xFFFFAAFF),
    onVaultAccent = Color(0xFF56065F),
    vaultAccentContainer = Color(0xFF702979),
    onVaultAccentContainer = Color(0xFFF8D8FF)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
