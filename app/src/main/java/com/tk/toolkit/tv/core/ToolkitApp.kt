package com.tk.toolkit.tv.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.toolkit.tv.launcher.LauncherViewModel
import com.tk.toolkit.tv.launcher.data.AppThemePalette
import com.tk.toolkit.tv.launcher.ui.HomeScreen
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import com.tk.toolkit.tv.ui.theme.ToolkitTheme

/**
 * App shell. The launcher home is the root screen; future tools get their own
 * screens here and are reached from the home header.
 */
@Composable
fun ToolkitApp(viewModel: LauncherViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ToolkitColors.currentPalette = state.themePalette

    ToolkitTheme {
        val palette = state.themePalette
        val wallpaper = state.customWallpaper
        val baseBackdrop = remember(palette) { renderBackdrop(palette) }
        val targetAmbient = if (state.dynamicAmbientColor) {
            ToolkitColors.focusedAmbientColor ?: palette.glowA
        } else {
            Color.Transparent
        }
        val animatedAmbient by animateColorAsState(
            targetValue = targetAmbient,
            animationSpec = tween(durationMillis = 650),
            label = "dynamicAmbientGlow",
        )

        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val dst = IntSize(size.width.toInt(), size.height.toInt())
                    if (wallpaper != null) {
                        drawImage(
                            wallpaper,
                            dstSize = dst,
                            filterQuality = FilterQuality.Low,
                        )
                        drawRect(Color.Black.copy(alpha = 0.44f))
                    } else {
                        drawImage(
                            baseBackdrop,
                            dstSize = dst,
                            filterQuality = FilterQuality.Low,
                        )
                    }
                    if (state.dynamicAmbientColor && animatedAmbient.alpha > 0.01f) {
                        drawRect(
                            Brush.radialGradient(
                                listOf(
                                    animatedAmbient.copy(alpha = if (palette.isOled) 0.22f else 0.28f),
                                    animatedAmbient.copy(alpha = 0f),
                                ),
                                center = Offset(size.width * 0.32f, size.height * 0.28f),
                                radius = size.width * 0.68f,
                            )
                        )
                    }
                }
        ) {
            HomeScreen(viewModel = viewModel)
        }
    }
}

/**
 * The ambient glow, rendered once at low resolution and stretched to fill the screen.
 * Drawing live full-screen gradients every frame is too slow for TV GPUs.
 */
private fun renderBackdrop(palette: AppThemePalette): ImageBitmap {
    val w = 320
    val h = 180
    val bitmap = ImageBitmap(w, h)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat())) {
        drawRect(palette.background)
        if (!palette.isOled) {
            drawRect(
                Brush.radialGradient(
                    listOf(palette.glowA.copy(alpha = 0.22f), palette.glowA.copy(alpha = 0f)),
                    center = Offset(w * 0.08f, h * 0.02f),
                    radius = w * 0.55f,
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(palette.glowB.copy(alpha = 0.16f), palette.glowB.copy(alpha = 0f)),
                    center = Offset(w * 0.95f, h * 1.05f),
                    radius = w * 0.6f,
                )
            )
        }
    }
    return bitmap
}
