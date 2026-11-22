package com.tk.toolkit.tv.launcher.data

import androidx.annotation.StringRes
import com.tk.toolkit.tv.R

enum class LauncherLayoutMode(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
) {
    GRID(
        id = "grid",
        titleRes = R.string.layout_mode_grid,
        subtitleRes = R.string.layout_mode_grid_subtitle,
    ),
    TIZEN_BAR(
        id = "tizen_bar",
        titleRes = R.string.layout_mode_tizen_bar,
        subtitleRes = R.string.layout_mode_tizen_bar_subtitle,
    ),
    ;

    companion object {
        val DEFAULT = GRID

        fun fromId(id: String?): LauncherLayoutMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
