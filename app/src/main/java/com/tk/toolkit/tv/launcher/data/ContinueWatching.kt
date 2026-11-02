package com.tk.toolkit.tv.launcher.data

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import com.tk.toolkit.tv.R

/**
 * A Continue Watching / Play Next entry shown on the home screen.
 * Can come from the system TV Provider (`watch_next_programs`) or from pinned items.
 */
@Immutable
data class WatchItem(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    @param:DrawableRes val artwork: Int? = null,
    val artworkBitmap: ImageBitmap? = null,
    val glow: Color,
    /** App that opens [uri]; the item is only shown while it's installed. */
    val packageName: String,
    val appName: String = "",
    val uri: String? = null,
    /** Caption drawn over the artwork, for artwork that is only a logo. */
    val caption: String? = null,
    /** Playback progress in 0f..1f when known (from last_playback_position_millis / duration_millis). */
    val progress: Float? = null,
    /** System WatchNextPrograms row ID when backed by TvProvider. */
    val watchNextId: Long? = null,
    val removable: Boolean = true,
) {
    fun intent(fallbackLaunch: (() -> Intent?)? = null): Intent? {
        val rawUri = uri
        if (!rawUri.isNullOrBlank()) {
            val parsed = runCatching {
                if (rawUri.startsWith("intent:", ignoreCase = true)) {
                    Intent.parseUri(rawUri, Intent.URI_INTENT_SCHEME)
                } else {
                    Intent(Intent.ACTION_VIEW, rawUri.toUri())
                }
            }.getOrNull()
            if (parsed != null) {
                if (parsed.`package` == null && parsed.component == null) {
                    parsed.setPackage(packageName)
                }
                parsed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return parsed
            }
        }
        return fallbackLaunch?.invoke()
    }
}

object ContinueWatching {
    val pinnedItems = listOf(
        WatchItem(
            key = "youtube:watch-later",
            title = "YouTube Watch Later",
            artwork = R.drawable.continue_youtube_watch_later,
            glow = Color(0xFFFF4E45),
            packageName = "com.google.android.youtube.tv",
            appName = "YouTube",
            // The TV app plays a /playlist link; its own "browse" route opens the playlist page.
            uri = "https://www.youtube.com/tv?c=VLWL",
            caption = "Watch Later",
            removable = true,
        ),
        WatchItem(
            key = "disney:the-simpsons",
            title = "The Simpsons",
            artwork = R.drawable.continue_the_simpsons,
            glow = Color(0xFFFFE14A),
            packageName = "com.disney.disneyplus",
            appName = "Disney+",
            uri = "https://www.disneyplus.com/browse/entity-cac75c8f-a9e2-4d95-ac73-1cf1cc7b9568",
        ),
        WatchItem(
            key = "disney:futurama",
            title = "Futurama",
            artwork = R.drawable.continue_futurama,
            glow = Color(0xFF9BE15D),
            packageName = "com.disney.disneyplus",
            appName = "Disney+",
            uri = "https://www.disneyplus.com/browse/entity-85bf4cc1-cd8b-4469-ad87-7289217a0b74",
        ),
    )
}
