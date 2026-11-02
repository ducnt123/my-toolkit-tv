package com.tk.toolkit.tv.launcher.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.tv.TvContract
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads and manages Android TV / Google TV "Play Next" (Watch Next) programs from the system
 * `android.media.tv` provider (`content://android.media.tv/watch_next_program`).
 */
class WatchNextRepository(private val context: Context) {

    private val resolver = context.contentResolver
    private val artworkCache = HashMap<String, ImageBitmap>()

    /** Watch Next programs URI. */
    val watchNextUri: Uri = TvContract.WatchNextPrograms.CONTENT_URI

    /**
     * Queries all browsable Play Next items from installed apps, sorted by most recent engagement.
     */
    fun loadWatchNext(appsByPackage: Map<String, LauncherApp>): List<WatchItem> {
        val rows = queryRows()
        val result = ArrayList<WatchItem>(rows.size)
        for (row in rows) {
            if (!row.browsable) continue
            val app = appsByPackage[row.packageName]
            val appLabel = app?.label ?: resolveAppLabel(row.packageName) ?: continue
            val accent = app?.accent ?: Color(0xFF38BDF8)
            val artUrl = row.thumbnailUri ?: row.posterArtUri
            val bitmap = artUrl?.let { loadArtwork(it) } ?: app?.banner
            val progress = if (row.durationMillis > 0L && row.positionMillis > 0L) {
                (row.positionMillis.toFloat() / row.durationMillis.toFloat()).coerceIn(0.02f, 1f)
            } else {
                null
            }
            val key = "watch_next:${row.id}"
            result += WatchItem(
                key = key,
                title = row.title.ifBlank { appLabel },
                subtitle = buildSubtitle(row, appLabel),
                artworkBitmap = bitmap,
                glow = accent,
                packageName = row.packageName,
                appName = appLabel,
                uri = row.intentUri,
                progress = progress,
                watchNextId = row.id,
                removable = true,
            )
        }
        return result
    }

    /**
     * Removes a Play Next item from the system TV Provider (by deleting or marking it non-browsable).
     */
    fun removeFromSystem(id: Long) {
        val itemUri = ContentUris.withAppendedId(watchNextUri, id)
        val deleted = runCatching { resolver.delete(itemUri, null, null) }.getOrDefault(0)
        if (deleted <= 0) {
            runCatching {
                val values = ContentValues().apply {
                    put(TvContract.WatchNextPrograms.COLUMN_BROWSABLE, 0)
                }
                resolver.update(itemUri, values, null, null)
            }
        }
    }

    /** Registers a [ContentObserver] that fires [onChange] when Play Next changes. */
    fun registerObserver(onChange: () -> Unit): ContentObserver? {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = onChange()
        }
        return runCatching {
            resolver.registerContentObserver(watchNextUri, true, observer)
            observer
        }.getOrNull()
    }

    fun unregisterObserver(observer: ContentObserver?) {
        if (observer != null) {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    private fun queryRows(): List<RawWatchNext> {
        val list = ArrayList<RawWatchNext>()
        runCatching {
            resolver.query(
                watchNextUri,
                PROJECTION,
                null,
                null,
                "${TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS} DESC",
            )?.use { cursor ->
                while (cursor.moveToNext() && list.size < MAX_ITEMS) {
                    parseRow(cursor)?.let { list += it }
                }
            }
        }
        return list
    }

    private fun parseRow(cursor: Cursor): RawWatchNext? = runCatching {
        val id = cursor.getLongOrNull(TvContract.WatchNextPrograms._ID) ?: return null
        val pkg = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME)
            ?.takeIf { it.isNotBlank() } ?: return null
        val browsable = (cursor.getIntOrNull(TvContract.WatchNextPrograms.COLUMN_BROWSABLE) ?: 1) != 0
        val title = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_TITLE).orEmpty().trim()
        val episodeTitle = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_EPISODE_TITLE)?.trim()
        val seasonNumber = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER)?.trim()
        val episodeNumber = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER)?.trim()
        val shortDesc = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION)?.trim()
        val posterUri = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI)?.trim()
        val thumbUri = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI)?.trim()
        val intentUri = cursor.getStringOrNull(TvContract.WatchNextPrograms.COLUMN_INTENT_URI)?.trim()
        val position = cursor.getLongOrNull(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS) ?: 0L
        val duration = cursor.getLongOrNull(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS) ?: 0L
        RawWatchNext(
            id = id,
            packageName = pkg,
            title = title,
            episodeTitle = episodeTitle,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            shortDescription = shortDesc,
            posterArtUri = posterUri,
            thumbnailUri = thumbUri,
            intentUri = intentUri,
            positionMillis = position,
            durationMillis = duration,
            browsable = browsable,
        )
    }.getOrNull()

    private fun buildSubtitle(row: RawWatchNext, appLabel: String): String {
        val epParts = ArrayList<String>(2)
        if (!row.seasonNumber.isNullOrBlank() && !row.episodeNumber.isNullOrBlank()) {
            epParts += "T${row.seasonNumber}:E${row.episodeNumber}"
        } else if (!row.episodeNumber.isNullOrBlank()) {
            epParts += "Tập ${row.episodeNumber}"
        }
        if (!row.episodeTitle.isNullOrBlank() && row.episodeTitle != row.title) {
            epParts += row.episodeTitle
        }
        if (epParts.isNotEmpty()) return epParts.joinToString(" · ")
        if (!row.shortDescription.isNullOrBlank() && row.shortDescription != row.title) {
            return row.shortDescription
        }
        return appLabel
    }

    private fun resolveAppLabel(packageName: String): String? = runCatching {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        if (!info.enabled) return null
        pm.getApplicationLabel(info).toString().trim()
    }.getOrNull()

    private fun loadArtwork(uriString: String): ImageBitmap? {
        artworkCache[uriString]?.let { return it }
        val bitmap = runCatching {
            val uri = uriString.toUri()
            when (uri.scheme?.lowercase()) {
                "http", "https" -> downloadBitmap(uriString)
                "content", "android.resource", "file" -> {
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                else -> null
            }?.let { scaleDown(it, MAX_W, MAX_H).asImageBitmap() }
        }.getOrNull()
        if (bitmap != null) {
            if (artworkCache.size > 60) artworkCache.clear()
            artworkCache[uriString] = bitmap
        }
        return bitmap
    }

    private fun downloadBitmap(urlStr: String): Bitmap? {
        val conn = (URL(urlStr).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.connectTimeout = 4_000
            conn.readTimeout = 4_000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conn.disconnect()
        }
    }

    private fun scaleDown(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (src.width <= maxW && src.height <= maxH) return src
        val ratio = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height)
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun Cursor.getStringOrNull(column: String): String? {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getString(idx) else null
    }

    private fun Cursor.getLongOrNull(column: String): Long? {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getLong(idx) else null
    }

    private fun Cursor.getIntOrNull(column: String): Int? {
        val idx = getColumnIndex(column)
        return if (idx >= 0 && !isNull(idx)) getInt(idx) else null
    }

    private data class RawWatchNext(
        val id: Long,
        val packageName: String,
        val title: String,
        val episodeTitle: String?,
        val seasonNumber: String?,
        val episodeNumber: String?,
        val shortDescription: String?,
        val posterArtUri: String?,
        val thumbnailUri: String?,
        val intentUri: String?,
        val positionMillis: Long,
        val durationMillis: Long,
        val browsable: Boolean,
    )

    private companion object {
        const val MAX_ITEMS = 40
        const val MAX_W = 480
        const val MAX_H = 270

        val PROJECTION = arrayOf(
            TvContract.WatchNextPrograms._ID,
            TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
            TvContract.WatchNextPrograms.COLUMN_TITLE,
            TvContract.WatchNextPrograms.COLUMN_EPISODE_TITLE,
            TvContract.WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
            TvContract.WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
            TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION,
            TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
            TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.WatchNextPrograms.COLUMN_INTENT_URI,
            TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
            TvContract.WatchNextPrograms.COLUMN_BROWSABLE,
        )
    }
}

