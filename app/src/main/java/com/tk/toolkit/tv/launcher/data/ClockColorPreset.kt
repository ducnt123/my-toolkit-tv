package com.tk.toolkit.tv.launcher.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.tk.toolkit.tv.R

enum class ClockColorPreset(
    val id: String,
    @param:StringRes val labelRes: Int,
    val primaryColor: Color,
    val previewGradient: List<Color>,
) {
    DEFAULT(
        id = "default",
        labelRes = R.string.color_preset_theme,
        primaryColor = Color.Unspecified,
        previewGradient = emptyList(),
    ),
    WHITE(
        id = "white",
        labelRes = R.string.color_preset_white,
        primaryColor = Color(0xFFFFFFFF),
        previewGradient = listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1)),
    ),
    GOLD(
        id = "gold",
        labelRes = R.string.color_preset_gold,
        primaryColor = Color(0xFFFBBF24),
        previewGradient = listOf(Color(0xFFFEF3C7), Color(0xFFF59E0B)),
    ),
    CYAN(
        id = "cyan",
        labelRes = R.string.color_preset_cyan,
        primaryColor = Color(0xFF38BDF8),
        previewGradient = listOf(Color(0xFFE0F2FE), Color(0xFF0284C7)),
    ),
    MINT(
        id = "mint",
        labelRes = R.string.color_preset_mint,
        primaryColor = Color(0xFF34D399),
        previewGradient = listOf(Color(0xFFD1FAE5), Color(0xFF059669)),
    ),
    PURPLE(
        id = "purple",
        labelRes = R.string.color_preset_purple,
        primaryColor = Color(0xFFA78BFA),
        previewGradient = listOf(Color(0xFFEDE9FE), Color(0xFF7C3AED)),
    ),
    PINK(
        id = "pink",
        labelRes = R.string.color_preset_pink,
        primaryColor = Color(0xFFF472B6),
        previewGradient = listOf(Color(0xFFFCE7F3), Color(0xFFDB2777)),
    ),
    CORAL(
        id = "coral",
        labelRes = R.string.color_preset_coral,
        primaryColor = Color(0xFFFB7185),
        previewGradient = listOf(Color(0xFFFFE4E6), Color(0xFFE11D48)),
    ),
    ORANGE(
        id = "orange",
        labelRes = R.string.color_preset_orange,
        primaryColor = Color(0xFFFB923C),
        previewGradient = listOf(Color(0xFFFFEDD5), Color(0xFFEA580C)),
    ),
    ;

    companion object {
        fun fromId(id: String?): ClockColorPreset =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
