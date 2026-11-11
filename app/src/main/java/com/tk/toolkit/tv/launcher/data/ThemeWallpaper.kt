package com.tk.toolkit.tv.launcher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.tk.toolkit.tv.R
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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

data class LocalImageEntry(
    val id: String,
    val name: String,
    val folderLabel: String,
    val file: File? = null,
    val contentUri: Uri? = null,
    val sizeBytes: Long = 0L,
    val lastModifiedMillis: Long = 0L,
) {
    fun formattedSubtitle(): String {
        val sizeStr = if (sizeBytes > 0L) formatFileSize(sizeBytes) else null
        return when {
            folderLabel.isNotBlank() && sizeStr != null -> "$folderLabel · $sizeStr"
            folderLabel.isNotBlank() -> folderLabel
            sizeStr != null -> sizeStr
            else -> ""
        }
    }
}

data class FolderBrowserState(
    val currentDir: File,
    val parentDir: File?,
    val directories: List<File>,
    val images: List<LocalImageEntry>,
)

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}

object WallpaperStore {
    private const val FILE_NAME = "custom_wallpaper.jpg"
    private const val MAX_W = 1920
    private const val MAX_H = 1080
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")

    fun wallpaperFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasCustomWallpaper(context: Context): Boolean = wallpaperFile(context).exists()

    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

    fun hasMediaPermission(context: Context): Boolean {
        if (hasAllFilesAccess()) return true
        val readExternal = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
        val readMediaImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        return readExternal || readMediaImages
    }

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun openAllFilesAccessSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(appIntent) }.isSuccess) return true

            val allIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(allIntent) }.isSuccess) return true
        }
        val detailsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(detailsIntent) }.isSuccess
    }

    fun loadWallpaper(context: Context): ImageBitmap? {
        val file = wallpaperFile(context)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    fun saveFromEntry(context: Context, entry: LocalImageEntry): Boolean {
        entry.file?.takeIf { it.exists() && it.canRead() }?.let { file ->
            if (saveFromFile(context, file)) return true
        }
        entry.contentUri?.let { uri ->
            if (saveFromUri(context, uri)) return true
        }
        return false
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

    fun loadThumbnail(context: Context, entry: LocalImageEntry, maxSide: Int = 144): ImageBitmap? = runCatching {
        val file = entry.file?.takeIf { it.exists() && it.canRead() }
        if (file != null) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            return@runCatching BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
        }
        val uri = entry.contentUri ?: return@runCatching null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap()
        }
    }.getOrNull()

    private fun computeInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var inSampleSize = 1
        val longest = maxOf(width, height)
        while (longest / inSampleSize > maxSide * 2) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
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

    /** Returns common quick-access folders on Android TV + any mounted USB volumes. */
    fun getQuickFolders(): List<Pair<String, File>> {
        val result = ArrayList<Pair<String, File>>()
        val seen = HashSet<String>()

        fun addIfValid(label: String, dir: File?) {
            if (dir != null && seen.add(dir.absolutePath) && (dir.exists() || dir.parentFile?.exists() == true)) {
                result += label to dir
            }
        }

        addIfValid("Download", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        addIfValid("Pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
        addIfValid("DCIM", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
        addIfValid("/sdcard", Environment.getExternalStorageDirectory())

        val storageRoot = File("/storage")
        val volumes = runCatching { storageRoot.listFiles() }.getOrNull().orEmpty()
        for (vol in volumes) {
            if (vol.isDirectory && vol.name != "emulated" && vol.name != "self" && vol.canRead()) {
                addIfValid("USB (${vol.name})", vol)
            }
        }
        return result
    }

    /** Lists subfolders and image files inside [dir] for the built-in folder browser. */
    fun browseDirectory(dir: File): FolderBrowserState {
        val children = runCatching { dir.listFiles() }.getOrNull().orEmpty()
        val subDirs = children
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
        val images = children
            .filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                LocalImageEntry(
                    id = "file:${file.absolutePath}",
                    name = file.name,
                    folderLabel = dir.name.ifBlank { dir.absolutePath },
                    file = file,
                    sizeBytes = file.length(),
                    lastModifiedMillis = file.lastModified(),
                )
            }
        val extRoot = Environment.getExternalStorageDirectory()?.absolutePath
        val parent = dir.parentFile?.takeIf { p ->
            p.canRead() || (extRoot != null && dir.absolutePath.startsWith(extRoot) && dir.absolutePath != extRoot)
        }
        return FolderBrowserState(
            currentDir = dir,
            parentDir = parent,
            directories = subDirs,
            images = images,
        )
    }

    /**
     * Discovers local images across both [MediaStore.Images] and common directories
     * (`Download`, `Pictures`, `DCIM`, `/sdcard`, and USB drives).
     */
    fun queryAllLocalImages(context: Context): List<LocalImageEntry> {
        val results = ArrayList<LocalImageEntry>()
        val seenPaths = HashSet<String>()
        val seenNamesAndSizes = HashSet<String>()

        // 1. Query MediaStore.Images
        runCatching {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED,
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext() && results.size < 80) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val file = path?.takeIf { it.isNotBlank() }?.let(::File)
                    val name = (if (nameCol >= 0) cursor.getString(nameCol) else null)
                        ?.takeIf { it.isNotBlank() }
                        ?: file?.name
                        ?: "image_$id.jpg"
                    val bucket = (if (bucketCol >= 0) cursor.getString(bucketCol) else null)
                        ?.takeIf { it.isNotBlank() }
                        ?: file?.parentFile?.name
                        ?: ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else (file?.length() ?: 0L)
                    val dateSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

                    if (path != null) seenPaths.add(path)
                    seenNamesAndSizes.add("${name.lowercase()}:$size")

                    results += LocalImageEntry(
                        id = "media:$id",
                        name = name,
                        folderLabel = bucket,
                        file = file?.takeIf { it.exists() },
                        contentUri = contentUri,
                        sizeBytes = size,
                        lastModifiedMillis = dateSec * 1000L,
                    )
                }
            }
        }

        // 2. Also scan filesystem folders (Download, Pictures, DCIM, /sdcard, USB) for unindexed files
        val roots = getQuickFolders().map { it.second }
        for (dir in roots) {
            val files = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (file in files) {
                if (file.isFile && file.extension.lowercase() in IMAGE_EXTENSIONS) {
                    val key = "${file.name.lowercase()}:${file.length()}"
                    if (seenPaths.add(file.absolutePath) && seenNamesAndSizes.add(key)) {
                        results += LocalImageEntry(
                            id = "file:${file.absolutePath}",
                            name = file.name,
                            folderLabel = file.parentFile?.name ?: "",
                            file = file,
                            sizeBytes = file.length(),
                            lastModifiedMillis = file.lastModified(),
                        )
                    }
                } else if (file.isDirectory && !file.name.startsWith(".") && dir != Environment.getExternalStorageDirectory()) {
                    val subFiles = runCatching { file.listFiles() }.getOrNull().orEmpty()
                    for (sub in subFiles) {
                        if (sub.isFile && sub.extension.lowercase() in IMAGE_EXTENSIONS) {
                            val key = "${sub.name.lowercase()}:${sub.length()}"
                            if (seenPaths.add(sub.absolutePath) && seenNamesAndSizes.add(key)) {
                                results += LocalImageEntry(
                                    id = "file:${sub.absolutePath}",
                                    name = sub.name,
                                    folderLabel = "${dir.name}/${file.name}",
                                    file = sub,
                                    sizeBytes = sub.length(),
                                    lastModifiedMillis = sub.lastModified(),
                                )
                            }
                        }
                    }
                }
            }
        }

        return results
            .sortedByDescending { it.lastModifiedMillis }
            .take(80)
    }
}

