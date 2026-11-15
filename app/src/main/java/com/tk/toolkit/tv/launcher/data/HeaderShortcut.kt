package com.tk.toolkit.tv.launcher.data

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.toolkit.tv.R

enum class HeaderShortcut(
    val id: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    INPUTS("inputs", R.string.header_shortcut_inputs, Icons.AutoMirrored.Outlined.Input),
    NETWORK("network", R.string.header_shortcut_network, Icons.Outlined.Wifi),
    AUDIO_OUTPUT("audio_output", R.string.header_shortcut_audio_output, Icons.Outlined.Speaker);

    companion object {
        val DEFAULT_ORDER = listOf(INPUTS, NETWORK, AUDIO_OUTPUT)

        fun fromId(id: String): HeaderShortcut? =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
