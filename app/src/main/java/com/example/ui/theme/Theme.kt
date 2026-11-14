package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPalette
import com.example.data.model.ThemeMode

@Immutable
data class FlexDeskChromeTokens(
    val isDark: Boolean,
    val isAmoled: Boolean,
    val highContrast: Boolean,
    val shelfBackground: Color,
    val launcherSheetBackground: Color,
    val controlCenterBackground: Color,
    val windowChromeBackground: Color,
    val pillSurface: Color,
    val borderColor: Color,
    val syncGreen: Color,
    val offlineAmber: Color,
    val alertRed: Color
)

val LocalFlexDeskTokens = staticCompositionLocalOf {
    FlexDeskChromeTokens(
        isDark = true,
        isAmoled = false,
        highContrast = false,
        shelfBackground = Color(0xE6141A24),
        launcherSheetBackground = Color(0xF0171D26),
        controlCenterBackground = Color(0xF51A212C),
        windowChromeBackground = Color(0xFF1B2330),
        pillSurface = Color(0xFF283244),
        borderColor = Color(0x33FFFFFF),
        syncGreen = SyncGreen,
        offlineAmber = OfflineAmber,
        alertRed = AlertRed
    )
}

private val FlexDeskShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlexDeskTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentPalette: AccentPalette = AccentPalette.CHROME_BLUE,
    fontScale: Float = 1.0f,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
    }
    val isAmoled = themeMode == ThemeMode.AMOLED

    val colorScheme = buildColorScheme(
        isDark = useDark,
        isAmoled = isAmoled,
        accentPalette = accentPalette,
        highContrast = highContrast
    )

    val chromeTokens = if (useDark) {
        FlexDeskChromeTokens(
            isDark = true,
            isAmoled = isAmoled,
            highContrast = highContrast,
            shelfBackground = if (isAmoled) Color(0xF205070A) else Color(0xEB131922),
            launcherSheetBackground = if (isAmoled) Color(0xF7080A0F) else Color(0xF2161D28),
            controlCenterBackground = if (isAmoled) Color(0xFA0C0F16) else Color(0xF51A2230),
            windowChromeBackground = if (isAmoled) Color(0xFF0E1219) else Color(0xFF1A222E),
            pillSurface = if (isAmoled) Color(0xFF181F2A) else Color(0xFF253042),
            borderColor = if (highContrast) Color(0xCCFFFFFF) else Color(0x28FFFFFF),
            syncGreen = SyncGreen,
            offlineAmber = OfflineAmber,
            alertRed = AlertRed
        )
    } else {
        FlexDeskChromeTokens(
            isDark = false,
            isAmoled = false,
            highContrast = highContrast,
            shelfBackground = Color(0xEBFFFFFF),
            launcherSheetBackground = Color(0xF5F7FAFF),
            controlCenterBackground = Color(0xFAFFFFFF),
            windowChromeBackground = Color(0xFFFFFFFF),
            pillSurface = Color(0xFFE6EDF7),
            borderColor = if (highContrast) Color(0xCC0F172A) else Color(0x1F0F172A),
            syncGreen = Color(0xFF059669),
            offlineAmber = Color(0xFFD97706),
            alertRed = Color(0xFFDC2626)
        )
    }

    CompositionLocalProvider(LocalFlexDeskTokens provides chromeTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildFlexDeskTypography(fontScale),
            shapes = FlexDeskShapes,
            content = content
        )
    }
}

private fun buildColorScheme(
    isDark: Boolean,
    isAmoled: Boolean,
    accentPalette: AccentPalette,
    highContrast: Boolean
): ColorScheme {
    return if (isDark) {
        val (primary, onPrimary, container) = when (accentPalette) {
            AccentPalette.CHROME_BLUE -> Triple(ChromeBluePrimaryDark, ChromeBlueOnPrimaryDark, ChromeBlueContainerDark)
            AccentPalette.AURORA_MINT -> Triple(AuroraMintPrimaryDark, AuroraMintOnPrimaryDark, AuroraMintContainerDark)
            AccentPalette.AMBER_DUSK -> Triple(AmberDuskPrimaryDark, AmberDuskOnPrimaryDark, AmberDuskContainerDark)
            AccentPalette.ROSE_QUARTZ -> Triple(RoseQuartzPrimaryDark, RoseQuartzOnPrimaryDark, RoseQuartzContainerDark)
            AccentPalette.SLATE_MONO -> Triple(SlateMonoPrimaryDark, SlateMonoOnPrimaryDark, SlateMonoContainerDark)
        }
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = Color(0xFFF1F5F9),
            secondary = primary.copy(alpha = 0.85f),
            onSecondary = onPrimary,
            secondaryContainer = if (isAmoled) AmoledSurfaceVariant else DarkSurfaceVariant,
            onSecondaryContainer = DarkOnSurface,
            background = if (isAmoled) AmoledBackground else DarkBackground,
            onBackground = if (highContrast) Color.White else DarkOnSurface,
            surface = if (isAmoled) AmoledSurface else DarkSurface,
            onSurface = if (highContrast) Color.White else DarkOnSurface,
            surfaceVariant = if (isAmoled) AmoledSurfaceVariant else DarkSurfaceVariant,
            onSurfaceVariant = if (highContrast) Color(0xFFE2E8F0) else DarkOnSurfaceVariant,
            outline = if (highContrast) Color(0xB3FFFFFF) else Color(0x3D94A3B8),
            error = AlertRed
        )
    } else {
        val (primary, container) = when (accentPalette) {
            AccentPalette.CHROME_BLUE -> ChromeBluePrimaryLight to ChromeBlueContainerLight
            AccentPalette.AURORA_MINT -> AuroraMintPrimaryLight to AuroraMintContainerLight
            AccentPalette.AMBER_DUSK -> AmberDuskPrimaryLight to AmberDuskContainerLight
            AccentPalette.ROSE_QUARTZ -> RoseQuartzPrimaryLight to RoseQuartzContainerLight
            AccentPalette.SLATE_MONO -> SlateMonoPrimaryLight to SlateMonoContainerLight
        }
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = container,
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = primary.copy(alpha = 0.85f),
            onSecondary = Color.White,
            secondaryContainer = LightSurfaceVariant,
            onSecondaryContainer = LightOnSurface,
            background = LightBackground,
            onBackground = if (highContrast) Color.Black else LightOnSurface,
            surface = LightSurface,
            onSurface = if (highContrast) Color.Black else LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = if (highContrast) Color(0xFF1E293B) else LightOnSurfaceVariant,
            outline = if (highContrast) Color(0xB30F172A) else Color(0x33475569),
            error = Color(0xFFDC2626)
        )
    }
}
