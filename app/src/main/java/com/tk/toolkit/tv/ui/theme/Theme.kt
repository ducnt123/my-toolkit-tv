package com.tk.toolkit.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.tk.toolkit.tv.launcher.data.AppThemePalette

object ToolkitColors {
    var currentPalette by mutableStateOf(AppThemePalette.DEFAULT)
    var focusedAmbientColor by mutableStateOf<Color?>(null)

    val Background: Color get() = currentPalette.background
    val GlowA: Color get() = currentPalette.glowA
    val GlowB: Color get() = currentPalette.glowB
    val Surface: Color get() = currentPalette.surface
    val SurfaceRaised: Color get() = currentPalette.surfaceRaised
    val OnSurface = Color(0xFFF2F3F7)
    val OnSurfaceMuted = Color(0xFF9097A6)
    val Accent: Color get() = currentPalette.accent
    val Danger = Color(0xFFFF7A7A)
    val FocusRing = Color(0xFFF5F6FA)
}

@Composable
fun ToolkitTheme(content: @Composable () -> Unit) {
    val palette = ToolkitColors.currentPalette
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = palette.accent,
            onPrimary = Color(0xFF0B1030),
            secondary = palette.glowB,
            background = palette.background,
            onBackground = ToolkitColors.OnSurface,
            surface = palette.surface,
            onSurface = ToolkitColors.OnSurface,
            surfaceVariant = palette.surfaceRaised,
            onSurfaceVariant = ToolkitColors.OnSurfaceMuted,
            error = ToolkitColors.Danger,
            border = ToolkitColors.FocusRing,
        ),
        typography = Typography,
        content = content,
    )
}
