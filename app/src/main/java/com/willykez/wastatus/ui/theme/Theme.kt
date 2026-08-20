package com.willykez.wastatus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.willykez.wastatus.model.AppThemeMode

/**
 * Full Material You theming: dynamic color sampled live from the device
 * wallpaper on Android 12+, a hand-tuned static fallback everywhere else,
 * and true light/dark adaptivity driven by either the system setting or an
 * explicit in-app choice.
 *
 * Note: this intentionally builds on the stable [MaterialTheme] rather than
 * `MaterialExpressiveTheme` / `MotionScheme.expressive()`. Those entry
 * points moved package (`androidx.compose.material3.expressive` in some
 * releases) and are marked internal in others depending on the exact
 * Material3 version this module resolves — referencing them broke the
 * build. Every dynamic-color and dark/light-adaptive behavior below is
 * unaffected; only the expressive-specific motion/shape defaults are not
 * wired in yet. Re-add `MaterialExpressiveTheme` once the resolved
 * `androidx.compose.material3:material3` version (check with
 * `./gradlew :app:dependencies | grep material3`) confirms which package
 * exposes it publicly.
 */
@Composable
fun WaStatusTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> StaticDarkColorScheme
        else -> StaticLightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WaStatusShapes,
            content = content
        )
    }
}
