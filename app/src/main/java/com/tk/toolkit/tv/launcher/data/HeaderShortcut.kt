package com.tk.toolkit.tv.launcher.data

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.toolkit.tv.R

enum class HeaderShortcut(
    val id: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val defaultVisible: Boolean = true,
) {
    SEARCH("search", R.string.header_shortcut_search, Icons.Outlined.Search, defaultVisible = false),
    INPUTS("inputs", R.string.header_shortcut_inputs, Icons.AutoMirrored.Outlined.Input, defaultVisible = true),
    NETWORK("network", R.string.header_shortcut_network, Icons.Outlined.Wifi, defaultVisible = true),
    AUDIO_OUTPUT("audio_output", R.string.header_shortcut_audio_output, Icons.Outlined.Speaker, defaultVisible = true);

    companion object {
        val DEFAULT_ORDER = listOf(SEARCH, INPUTS, NETWORK, AUDIO_OUTPUT)
        val DEFAULT_VISIBLE_IDS: Set<String> = entries.filter { it.defaultVisible }.map { it.id }.toSet()

        fun fromId(id: String): HeaderShortcut? =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
