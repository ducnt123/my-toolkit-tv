package com.tk.toolkit.tv.launcher.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalBackupEntry(
    val id: String,
    val name: String,
    val folderLabel: String,
    val file: File? = null,
    val contentUri: Uri? = null,
    val sizeBytes: Long = 0L,
    val lastModifiedMillis: Long = 0L,
) {
    fun formattedSubtitle(): String {
        val sizeStr = if (sizeBytes > 0L) formatBackupFileSize(sizeBytes) else null
        val dateStr = if (lastModifiedMillis > 0L) {
            runCatching {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastModifiedMillis))
            }.getOrNull()
        } else null
        return listOfNotNull(
            folderLabel.takeIf { it.isNotBlank() },
            sizeStr,
            dateStr,
        ).joinToString(" · ")
    }
}

data class BackupFolderBrowserState(
    val currentDir: File,
    val parentDir: File?,
    val directories: List<File>,
    val files: List<LocalBackupEntry>,
)

private fun formatBackupFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}

object BackupManager {
    const val DEFAULT_FILE_NAME = "toolkit_launcher_backup.json"
    private val BACKUP_EXTENSIONS = setOf("json")

    private fun internalBackupFile(context: Context): File =
        File(context.filesDir, DEFAULT_FILE_NAME)

    private fun publicDownloadFile(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DEFAULT_FILE_NAME)

    fun normalizeFileName(rawName: String): String {
        val cleaned = rawName.trim()
            .replace('/', '_')
            .replace('\\', '_')
            .ifBlank { DEFAULT_FILE_NAME }
        return if (cleaned.lowercase(Locale.ROOT).endsWith(".json")) cleaned else "$cleaned.json"
    }

    fun generateTimestampedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "toolkit_backup_$stamp.json"
    }

    /**
     * Saves the backup JSON to `/sdcard/Download/toolkit_launcher_backup.json` (via MediaStore on API 29+
     * and direct file write) plus an internal backup copy. Returns the display path on success.
     */
    fun exportDefaultBackup(context: Context): String? {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return exportToDirectory(context, downloadDir, DEFAULT_FILE_NAME)
    }

    /**
     * Saves the backup JSON into [dir] with [rawFileName] (normalized to `.json`), plus an internal backup copy.
     * Returns the human-readable saved path on success, or `null` if writing failed.
     */
    fun exportToDirectory(context: Context, dir: File, rawFileName: String): String? {
        val fileName = normalizeFileName(rawFileName)
        val json = LauncherPrefs(context).exportToJson()
        var savedTarget = false

        // 1. Always save a local copy in app storage
        runCatching {
            internalBackupFile(context).writeText(json, Charsets.UTF_8)
        }

        // 2. Try writing directly to the target directory
        val targetFile = File(dir, fileName)
        runCatching {
            dir.mkdirs()
            targetFile.writeText(json, Charsets.UTF_8)
            savedTarget = true
        }

        // 3. On Android 10+ (API 29+), if target is Downloads and direct write wasn't permitted, use MediaStore.Downloads
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val isDownloadDir = downloadDir != null && dir.absolutePath == downloadDir.absolutePath
        if (!savedTarget && isDownloadDir && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                resolver.delete(
                    collection,
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                    arrayOf(fileName),
                )
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(collection, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    savedTarget = true
                }
            }
        }

        val dirLabel = dir.name.ifBlank { dir.absolutePath }
        return if (savedTarget) {
            "$dirLabel/$fileName"
        } else if (isDownloadDir && internalBackupFile(context).exists()) {
            internalBackupFile(context).name
        } else {
            null
        }
    }

    fun exportToUri(context: Context, uri: Uri): Boolean = runCatching {
        val json = LauncherPrefs(context).exportToJson()
        internalBackupFile(context).writeText(json, Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } != null
    }.getOrDefault(false)

    /**
     * Restores the backup JSON from `/sdcard/Download/toolkit_launcher_backup.json` (or MediaStore.Downloads,
     * or the internal backup copy).
     */
    fun importDefaultBackup(context: Context): Boolean {
        val json = readPublicDownloadBackup(context)
            ?: runCatching { internalBackupFile(context).takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull()
            ?: return false
        return LauncherPrefs(context).importFromJson(json)
    }

    fun importFromFile(context: Context, file: File): Boolean = runCatching {
        if (!file.exists() || !file.canRead()) return false
        val json = file.readText(Charsets.UTF_8)
        LauncherPrefs(context).importFromJson(json)
    }.getOrDefault(false)

    fun importFromUri(context: Context, uri: Uri): Boolean = runCatching {
        val json = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: return false
        LauncherPrefs(context).importFromJson(json)
    }.getOrDefault(false)

    fun importFromEntry(context: Context, entry: LocalBackupEntry): Boolean {
        val file = entry.file?.takeIf { it.exists() && it.canRead() }
        if (file != null && importFromFile(context, file)) return true
        val uri = entry.contentUri
        if (uri != null && importFromUri(context, uri)) return true
        return false
    }

    fun hasAvailableBackup(context: Context): Boolean {
        if (publicDownloadFile().exists() || internalBackupFile(context).exists()) return true
        return readPublicDownloadBackup(context) != null
    }

    /** Returns common quick-access folders on Android TV + any mounted USB volumes for backup/restore. */
    fun getQuickFolders(): List<Pair<String, File>> {
        val result = ArrayList<Pair<String, File>>()
        val seen = HashSet<String>()

        fun addIfValid(label: String, dir: File?) {
            if (dir != null && seen.add(dir.absolutePath) && (dir.exists() || dir.parentFile?.exists() == true)) {
                result += label to dir
            }
        }

        addIfValid("Download", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        addIfValid("Documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
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

    /** Lists subfolders and `.json` backup files inside [dir] for the built-in folder browser. */
    fun browseDirectory(dir: File): BackupFolderBrowserState {
        val children = runCatching { dir.listFiles() }.getOrNull().orEmpty()
        val subDirs = children
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }
        val files = children
            .filter { it.isFile && it.extension.lowercase() in BACKUP_EXTENSIONS }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                LocalBackupEntry(
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
        return BackupFolderBrowserState(
            currentDir = dir,
            parentDir = parent,
            directories = subDirs,
            files = files,
        )
    }

    /**
     * Discovers `.json` backup files across [MediaStore.Downloads], common directories
     * (`Download`, `Documents`, `/sdcard`, and USB drives), and internal app storage.
     */
    fun queryAllLocalBackups(context: Context): List<LocalBackupEntry> {
        val results = ArrayList<LocalBackupEntry>()
        val seenPaths = HashSet<String>()
        val seenNamesAndSizes = HashSet<String>()

        // 1. Query MediaStore.Downloads on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                )
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%.json")
                val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val bucketCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext() && results.size < 80) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                        val file = path?.takeIf { it.isNotBlank() }?.let(::File)
                        val name = (if (nameCol >= 0) cursor.getString(nameCol) else null)
                            ?.takeIf { it.isNotBlank() }
                            ?: file?.name
                            ?: continue
                        if (!name.lowercase(Locale.ROOT).endsWith(".json")) continue
                        val bucket = (if (bucketCol >= 0) cursor.getString(bucketCol) else null)
                            ?.takeIf { it.isNotBlank() }
                            ?: file?.parentFile?.name
                            ?: "Download"
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else (file?.length() ?: 0L)
                        val dateSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

                        if (path != null) seenPaths.add(path)
                        seenNamesAndSizes.add("${name.lowercase()}:$size")

                        results += LocalBackupEntry(
                            id = "dl:$id",
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
        }

        // 2. Scan filesystem folders (Download, Documents, /sdcard, USB)
        val roots = getQuickFolders().map { it.second }
        for (dir in roots) {
            val files = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (file in files) {
                if (file.isFile && file.extension.lowercase() in BACKUP_EXTENSIONS) {
                    val key = "${file.name.lowercase()}:${file.length()}"
                    if (seenPaths.add(file.absolutePath) && seenNamesAndSizes.add(key)) {
                        results += LocalBackupEntry(
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
                        if (sub.isFile && sub.extension.lowercase() in BACKUP_EXTENSIONS) {
                            val key = "${sub.name.lowercase()}:${sub.length()}"
                            if (seenPaths.add(sub.absolutePath) && seenNamesAndSizes.add(key)) {
                                results += LocalBackupEntry(
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

        // 3. Also include internal app backup if present and not already represented
        val internal = internalBackupFile(context)
        if (internal.exists() && internal.canRead()) {
            val key = "${internal.name.lowercase()}:${internal.length()}"
            if (seenPaths.add(internal.absolutePath) && seenNamesAndSizes.add(key)) {
                results += LocalBackupEntry(
                    id = "internal:${internal.absolutePath}",
                    name = internal.name,
                    folderLabel = "Toolkit Internal",
                    file = internal,
                    sizeBytes = internal.length(),
                    lastModifiedMillis = internal.lastModified(),
                )
            }
        }

        return results
            .sortedByDescending { it.lastModifiedMillis }
            .take(80)
    }

    private fun readPublicDownloadBackup(context: Context): String? {
        runCatching {
            val file = publicDownloadFile()
            if (file.exists() && file.canRead()) {
                return file.readText(Charsets.UTF_8)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                    arrayOf(DEFAULT_FILE_NAME),
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val uri = Uri.withAppendedPath(collection, id.toString())
                        return resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }
                }
            }
        }
        return null
    }
}
