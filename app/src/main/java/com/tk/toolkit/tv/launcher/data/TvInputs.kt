package com.tk.toolkit.tv.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart

data class TvInputEntry(
    val id: String,
    val label: String,
    val customLabel: String?,
    val type: Int,
    val isPassthrough: Boolean,
    val isLiveTv: Boolean,
    val state: Int,
    val packageName: String,
    val icon: ImageBitmap?,
) {
    val displayTitle: String
        get() = when {
            !customLabel.isNullOrBlank() && !customLabel.equals(label, ignoreCase = true) ->
                "$customLabel ($label)"
            label.isNotBlank() -> label
            isLiveTv -> "Truyền hình (Live TV)"
            type == TvInputInfo.TYPE_HDMI -> "HDMI"
            type == TvInputInfo.TYPE_COMPOSITE -> "AV"
            type == TvInputInfo.TYPE_COMPONENT -> "Component"
            else -> "Ngõ vào"
        }

    val stateLabel: String
        get() = when {
            isLiveTv -> "Truyền hình trực tiếp"
            state == TvInputManager.INPUT_STATE_CONNECTED -> "Đã kết nối"
            state == TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> "Thiết bị đang chờ"
            else -> "Chưa kết nối tín hiệu"
        }
}

object TvInputs {

    /**
     * Loads available TV inputs via [TvInputManager].
     * Returns a non-empty list only on actual TV devices that have hardware/passthrough inputs,
     * with Live TV (Tuner / Truyền hình) inputs prioritized at the top.
     */
    fun loadInputs(context: Context): List<TvInputEntry> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            ?: return emptyList()
        val rawList = runCatching { manager.tvInputList }.getOrNull().orEmpty()
        if (rawList.isEmpty()) return emptyList()

        // A device is a TV with inputs only if it exposes at least one hardware/passthrough input
        // (TV boxes only have HDMI output, never passthrough inputs).
        val hasPassthrough = rawList.any { it.isPassthroughInput }
        if (!hasPassthrough) return emptyList()

        val entries = rawList
            .filter { info ->
                val notHidden = runCatching { !info.isHidden(context) }.getOrDefault(true)
                val pkg = info.serviceInfo?.packageName.orEmpty()
                val cls = info.serviceInfo?.name.orEmpty()
                notHidden &&
                    pkg !in IGNORED_PACKAGES &&
                    !cls.contains("Preview", ignoreCase = true)
            }
            .map { info ->
                val label = runCatching { info.loadLabel(context)?.toString()?.trim().orEmpty() }.getOrDefault("")
                val customLabel = runCatching { info.loadCustomLabel(context)?.toString()?.trim() }.getOrNull()
                val state = runCatching { manager.getInputState(info.id) }
                    .getOrDefault(TvInputManager.INPUT_STATE_CONNECTED)
                val icon = runCatching { info.loadIcon(context)?.toBitmapSafely() }.getOrNull()
                val isLive = isLiveTvInput(info, label, customLabel)
                TvInputEntry(
                    id = info.id,
                    label = label,
                    customLabel = customLabel,
                    type = info.type,
                    isPassthrough = info.isPassthroughInput,
                    isLiveTv = isLive,
                    state = state,
                    packageName = info.serviceInfo?.packageName.orEmpty(),
                    icon = icon,
                )
            }

        return entries.sortedWith(
            compareBy<TvInputEntry> { !it.isLiveTv }
                .thenBy { stateRank(it.state) }
                .thenBy { typeRank(it.type) }
                .thenBy { it.displayTitle.lowercase() },
        )
    }

    fun openInput(context: Context, input: TvInputEntry) {
        val candidates = buildList {
            if (input.isPassthrough) {
                add(
                    Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } else {
                add(
                    Intent(Intent.ACTION_VIEW, TvContract.buildChannelsUriForInput(input.id))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                add(
                    Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                if (input.packageName.isNotBlank()) {
                    context.packageManager.getLaunchIntentForPackage(input.packageName)?.let {
                        add(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                add(
                    Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        val resolved = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: candidates.firstOrNull()
        if (resolved != null) {
            context.safeStart(resolved)
        }
    }

    private fun isLiveTvInput(info: TvInputInfo, label: String, customLabel: String?): Boolean {
        if (info.type == TvInputInfo.TYPE_TUNER) return true
        if (!info.isPassthroughInput) return true
        val combined = "$label ${customLabel.orEmpty()}".lowercase()
        return LIVE_TV_KEYWORDS.any { combined.contains(it) }
    }

    private fun stateRank(state: Int): Int = when (state) {
        TvInputManager.INPUT_STATE_CONNECTED -> 0
        TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> 1
        else -> 2
    }

    private fun typeRank(type: Int): Int = when (type) {
        TvInputInfo.TYPE_TUNER -> 0
        TvInputInfo.TYPE_HDMI -> 1
        TvInputInfo.TYPE_DISPLAY_PORT -> 2
        TvInputInfo.TYPE_COMPONENT -> 3
        TvInputInfo.TYPE_COMPOSITE -> 4
        else -> 5
    }

    private fun Drawable.toBitmapSafely(): ImageBitmap? = runCatching {
        val w = intrinsicWidth.takeIf { it in 1..256 } ?: 72
        val h = intrinsicHeight.takeIf { it in 1..256 } ?: 72
        toBitmap(w, h).asImageBitmap()
    }.getOrNull()

    private val IGNORED_PACKAGES = setOf(
        "com.google.android.videos",
    )

    private val LIVE_TV_KEYWORDS = listOf(
        "live tv",
        "truyền hình",
        "tuner",
        "dtv",
        "atv",
        "anten",
        "kênh",
    )
}

@Composable
fun rememberTvInputs(context: Context): List<TvInputEntry> {
    var inputs by remember { mutableStateOf(TvInputs.loadInputs(context)) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            ?: return@DisposableEffect onDispose { }
        val update = { inputs = TvInputs.loadInputs(context) }
        val callback = object : TvInputManager.TvInputCallback() {
            override fun onInputAdded(inputId: String) = update()
            override fun onInputRemoved(inputId: String) = update()
            override fun onInputUpdated(inputId: String) = update()
            override fun onInputStateChanged(inputId: String, state: Int) = update()
            override fun onTvInputInfoUpdated(inputInfo: TvInputInfo) = update()
        }
        runCatching { manager.registerCallback(callback, Handler(Looper.getMainLooper())) }
        update()
        onDispose {
            runCatching { manager.unregisterCallback(callback) }
        }
    }
    return inputs
}

