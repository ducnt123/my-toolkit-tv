package com.tk.toolkit.tv.launcher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.core.ToolkitAccessibilityService

/** System-level actions the launcher can perform on an app. */
object AppActions {

    fun appInfoIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun openAppInfo(context: Context, packageName: String) =
        context.safeStart(appInfoIntent(packageName))

    fun uninstall(context: Context, packageName: String) =
        context.safeStart(
            Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    fun openSystemSettings(context: Context) =
        context.safeStart(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /** Starts the Android TV / Google TV screensaver (Ambient Mode / Daydream). */
    fun startScreensaver(context: Context) {
        val candidates = listOf(
            Intent("com.google.android.pano.action.SLEEP")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Intent.ACTION_MAIN)
                .setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Settings.ACTION_DREAM_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val match = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: candidates.first()
        context.safeStart(match)
    }

    /** Opens Android TV's Accounts & Sign-in settings (for Profile Switch button when stock launcher is disabled). */
    fun openAccountsSettings(context: Context) {
        val candidates = listOf(
            Intent("com.google.android.apps.tv.launcherx.profile.LOCK_SCREEN_MGT")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Settings.ACTION_SYNC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Settings.ACTION_ADD_ACCOUNT).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val match = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: candidates.last()
        context.safeStart(match)
    }

    /** Opens Android TV's Network & Internet settings (Wi-Fi and Ethernet). */
    fun openNetworkSettings(context: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val match = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: candidates.last()
        context.safeStart(match)
    }

    /**
     * Google TV's own "Audio output" picker (TV speakers, HDMI, Bluetooth). It lives in SystemUI;
     * if a build doesn't have it, fall back to Display & Sound settings.
     */
    fun openAudioOutput(context: Context) {
        val picker = Intent("com.android.systemui.TV_MEDIA_OUTPUT_DIALOG")
            .setPackage("com.android.systemui")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (picker.resolveActivity(context.packageManager) != null) {
            context.safeStart(picker)
        } else {
            context.safeStart(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /**
     * Third-party apps can't force-stop other apps directly. When the accessibility helper is
     * enabled it taps "Force stop" in system Settings for us; otherwise we open App info.
     */
    fun forceStop(context: Context, packageName: String) {
        val service = ToolkitAccessibilityService.instance
        if (service != null) {
            service.forceStop(packageName)
        } else {
            val localized = LocaleHelper.wrapContext(context)
            Toast.makeText(
                localized,
                localized.getString(R.string.toast_enable_accessibility_force_stop),
                Toast.LENGTH_LONG,
            ).show()
            openAppInfo(context, packageName)
        }
    }

    fun Context.safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val localized = LocaleHelper.wrapContext(this)
            Toast.makeText(localized, localized.getString(R.string.toast_could_not_open), Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            val localized = LocaleHelper.wrapContext(this)
            Toast.makeText(localized, localized.getString(R.string.toast_not_allowed), Toast.LENGTH_SHORT).show()
        }
    }
}
