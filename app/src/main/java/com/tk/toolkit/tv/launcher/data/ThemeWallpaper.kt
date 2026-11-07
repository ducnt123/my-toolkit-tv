package com.tk.toolkit.tv.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tk.toolkit.tv.R
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class AppThemePalette(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val background: Color,
    val glowA: Color,
    val glowB: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val accent: Color,
    val isOled: Boolean = false,
) {
    OCEAN_BLUE(
        id = "ocean_blue",
        titleRes = R.string.theme_ocean_blue,
        subtitleRes = R.string.theme_ocean_blue_subtitle,
        background = Color(0xFF07080C),
        glowA = Color(0xFF3B3FD8),
        glowB = Color(0xFF0E8F8A),
        surface = Color(0xFF14161D),
        surfaceRaised = Color(0xFF1C1F28),
        accent = Color(0xFF8FA8FF),
    ),
    MIDNIGHT_PURPLE(
        id = "midnight_purple",
        titleRes = R.string.theme_midnight_purple,
        subtitleRes = R.string.theme_midnight_purple_subtitle,
        background = Color(0xFF090612),
        glowA = Color(0xFF6D28D9),
        glowB = Color(0xFFDB2777),
        surface = Color(0xFF161224),
        surfaceRaised = Color(0xFF211B34),
        accent = Color(0xFFC4B5FD),
    ),
    EMERALD_TEAL(
        id = "emerald_teal",
        titleRes = R.string.theme_emerald_teal,
        subtitleRes = R.string.theme_emerald_teal_subtitle,
        background = Color(0xFF040D0B),
        glowA = Color(0xFF0D9488),
        glowB = Color(0xFF0284C7),
        surface = Color(0xFF0F1D1A),
        surfaceRaised = Color(0xFF162925),
        accent = Color(0xFF5EEAD4),
    ),
    OLED_BLACK(
        id = "oled_black",
        titleRes = R.string.theme_oled_black,
        subtitleRes = R.string.theme_oled_black_subtitle,
        background = Color(0xFF000000),
        glowA = Color(0xFF000000),
        glowB = Color(0xFF000000),
        surface = Color(0xFF0C0C0E),
        surfaceRaised = Color(0xFF15161A),
        accent = Color(0xFF93C5FD),
        isOled = true,
    ),
    ;

    companion object {
        val DEFAULT = OCEAN_BLUE

        fun fromId(id: String?): AppThemePalette =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}

object WallpaperStore {
    private const val FILE_NAME = "custom_wallpaper.jpg"
    private const val MAX_W = 1920
    private const val MAX_H = 1080

    fun wallpaperFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasCustomWallpaper(context: Context): Boolean = wallpaperFile(context).exists()

    fun loadWallpaper(context: Context): ImageBitmap? {
        val file = wallpaperFile(context)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    fun saveFromUri(context: Context, uri: Uri): Boolean = runCatching {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return false
        saveBitmap(context, bitmap)
    }.getOrDefault(false)

    fun saveFromFile(context: Context, file: File): Boolean = runCatching {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        saveBitmap(context, bitmap)
    }.getOrDefault(false)

    fun saveFromUrl(context: Context, urlString: String): Boolean = runCatching {
        val conn = (URL(urlString.trim()).openConnection() as? HttpURLConnection) ?: return false
        val bitmap = try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conn.disconnect()
        } ?: return false
        saveBitmap(context, bitmap)
    }.getOrDefault(false)

    fun clearWallpaper(context: Context) {
        runCatching { wallpaperFile(context).delete() }
    }

    private fun saveBitmap(context: Context, src: Bitmap): Boolean {
        val scaled = scaleDown(src, MAX_W, MAX_H)
        FileOutputStream(wallpaperFile(context)).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return true
    }

    private fun scaleDown(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        if (src.width <= maxW && src.height <= maxH) return src
        val ratio = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height)
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    /** Scans common public directories on Android TV for image files when no system picker is available. */
    fun scanLocalImages(): List<File> {
        val extensions = setOf("jpg", "jpeg", "png", "webp")
        val roots = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStorageDirectory(),
        ).distinctBy { it.absolutePath }

        val found = ArrayList<File>()
        val seen = HashSet<String>()
        for (dir in roots) {
            val files = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (file in files) {
                if (file.isFile && file.extension.lowercase() in extensions && seen.add(file.absolutePath)) {
                    found += file
                } else if (file.isDirectory && !file.name.startsWith(".") && dir != Environment.getExternalStorageDirectory()) {
                    val subFiles = runCatching { file.listFiles() }.getOrNull().orEmpty()
                    for (sub in subFiles) {
                        if (sub.isFile && sub.extension.lowercase() in extensions && seen.add(sub.absolutePath)) {
                            found += sub
                        }
                    }
                }
            }
        }
        return found.sortedByDescending { it.lastModified() }.take(50)
    }
}

