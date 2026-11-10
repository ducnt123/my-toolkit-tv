package com.tk.toolkit.tv.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.coroutineContext

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkUrl: String,
    val apkName: String,
    val apkSizeBytes: Long,
)

sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class UpdateAvailable(val currentVersion: String, val release: ReleaseInfo) : UpdateCheckResult
    data class NoReleasePublished(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

enum class InstallOutcome {
    LAUNCHED,
    NEEDS_UNKNOWN_SOURCES_PERMISSION,
    FAILED,
}

object AppUpdateManager {
    const val GITHUB_OWNER = "ducnt123"
    const val GITHUB_REPO = "my-toolkit-tv"
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    fun currentVersionName(context: Context): String = runCatching {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pkgInfo.versionName ?: "1.0"
    }.getOrDefault("1.0")

    fun currentVersionCode(context: Context): Long = runCatching {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
    }.getOrDefault(1L)

    suspend fun checkForUpdate(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVer = currentVersionName(context)
        runCatching {
            val conn = openFollowedConnection(
                urlStr = LATEST_RELEASE_API,
                accept = "application/vnd.github+json",
            )
            try {
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    return@withContext UpdateCheckResult.NoReleasePublished(currentVer)
                }
                if (code !in 200..299) {
                    return@withContext UpdateCheckResult.Error("HTTP $code")
                }
                val bodyText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(bodyText)
                val tagName = json.optString("tag_name", "").trim()
                val releaseName = json.optString("name", tagName).trim()
                val changelog = json.optString("body", "").trim()
                val normalizedRemoteVersion = normalizeVersion(tagName)

                val assets = json.optJSONArray("assets")
                var apkUrl = ""
                var apkName = "toolkit-launcher.apk"
                var apkSize = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                            apkUrl = downloadUrl
                            apkName = name
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (tagName.isBlank() || apkUrl.isBlank()) {
                    return@withContext UpdateCheckResult.NoReleasePublished(currentVer)
                }

                val release = ReleaseInfo(
                    tagName = tagName,
                    versionName = normalizedRemoteVersion.ifBlank { tagName },
                    releaseTitle = releaseName.ifBlank { tagName },
                    changelog = changelog,
                    apkUrl = apkUrl,
                    apkName = apkName,
                    apkSizeBytes = apkSize,
                )

                if (isRemoteNewer(remoteVersion = normalizedRemoteVersion, localVersion = normalizeVersion(currentVer))) {
                    UpdateCheckResult.UpdateAvailable(currentVer, release)
                } else {
                    UpdateCheckResult.UpToDate(currentVer)
                }
            } finally {
                conn.disconnect()
            }
        }.getOrElse { e ->
            UpdateCheckResult.Error(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }

    suspend fun downloadReleaseApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { it.delete() }
            val targetFile = File(updatesDir, "toolkit-update-${release.versionName}.apk")

            val conn = openFollowedConnection(
                urlStr = release.apkUrl,
                accept = "application/octet-stream",
            )
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    error("HTTP $code")
                }
                val contentLength = conn.contentLengthLong.takeIf { it > 0L } ?: release.apkSizeBytes
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var lastReportedPercent = -1
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val percent = if (contentLength > 0L) {
                                ((downloaded * 100L) / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(percent, downloaded, contentLength)
                            }
                        }
                        output.flush()
                    }
                }
                if (!targetFile.exists() || targetFile.length() < 10_000L) {
                    targetFile.delete()
                    error("Invalid APK file")
                }
                targetFile
            } finally {
                conn.disconnect()
            }
        }
    }

    fun installApk(context: Context, apkFile: File): InstallOutcome {
        if (!apkFile.exists()) return InstallOutcome.FAILED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = runCatching {
                context.packageManager.canRequestPackageInstalls()
            }.getOrDefault(true)
            if (!canInstall) {
                openUnknownSourcesSettings(context)
                return InstallOutcome.NEEDS_UNKNOWN_SOURCES_PERMISSION
            }
        }

        return runCatching {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            @Suppress("DEPRECATION")
            val installPkgIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, false)
            }
            val chosen = listOf(viewIntent, installPkgIntent).firstOrNull {
                it.resolveActivity(context.packageManager) != null
            } ?: viewIntent
            context.safeStart(chosen)
            InstallOutcome.LAUNCHED
        }.getOrDefault(InstallOutcome.FAILED)
    }

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pkgIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val generalIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val securityIntent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val target = listOf(pkgIntent, generalIntent, securityIntent).firstOrNull {
                it.resolveActivity(context.packageManager) != null
            } ?: securityIntent
            context.safeStart(target)
        }
    }

    fun formatSizeMb(bytes: Long): String {
        if (bytes <= 0L) return ""
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    private fun openFollowedConnection(urlStr: String, accept: String): HttpURLConnection {
        var currentUrl = urlStr
        repeat(6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
                requestMethod = "GET"
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", "ToolkitLauncher-TV")
            }
            conn.connect()
            val code = conn.responseCode
            if (code in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308,
                )
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (!location.isNullOrBlank()) {
                    currentUrl = location
                    return@repeat
                }
            }
            return conn
        }
        error("Too many redirects")
    }

    private fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").trim()

    internal fun isRemoteNewer(remoteVersion: String, localVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        if (remoteVersion == localVersion) return false
        val remoteParts = remoteVersion.split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }
        val localParts = localVersion.split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r != l) return r > l
        }
        return remoteVersion != localVersion && remoteParts.isEmpty()
    }
}

