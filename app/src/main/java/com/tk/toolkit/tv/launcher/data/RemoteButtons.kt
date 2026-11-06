package com.tk.toolkit.tv.launcher.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart

enum class RemoteButton(
    val id: String,
    val title: String,
    val defaultActionLabel: String,
    val defaultKeyCodes: Set<Int>,
) {
    GUIDE(
        id = "guide",
        title = "Phím Guide",
        defaultActionLabel = "Mặc định (Mở Truyền hình / Live TV)",
        defaultKeyCodes = setOf(
            KeyEvent.KEYCODE_GUIDE,
            KeyEvent.KEYCODE_TV,
            KeyEvent.KEYCODE_TV_CONTENTS_MENU,
            KeyEvent.KEYCODE_DVR,
        ),
    ),
    YOUTUBE(
        id = "youtube",
        title = "Phím YouTube",
        defaultActionLabel = "Mặc định (Mở YouTube / SmartTube)",
        defaultKeyCodes = setOf(
            289, // KEYCODE_VIDEO_APP_1 (Android 12+ TV dedicated video app button 1)
            297, // KEYCODE_FEATURED_APP_1 (Android 12+ TV featured app button 1)
            KeyEvent.KEYCODE_BUTTON_3,
        ),
    ),
    SETTINGS(
        id = "settings",
        title = "Phím Cài đặt",
        defaultActionLabel = "Mặc định (Mở Cài đặt hệ thống)",
        defaultKeyCodes = setOf(
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_NOTIFICATION,
        ),
    ),
    PROFILE(
        id = "profile",
        title = "Phím Chuyển người dùng",
        defaultActionLabel = "Mặc định (Mở Tài khoản & Đăng nhập)",
        defaultKeyCodes = setOf(
            KeyEvent.KEYCODE_PROFILE_SWITCH,
        ),
    ),
}

data class RemoteButtonConfig(
    val button: RemoteButton,
    /** [TARGET_DEFAULT], [TARGET_DISABLED], or an app package name. */
    val target: String = RemoteButtons.TARGET_DEFAULT,
    /** Optional user-learned Android keyCode (0 if not set). */
    val customKeyCode: Int = 0,
    /** Optional user-learned hardware scanCode (0 if not set). */
    val customScanCode: Int = 0,
)

data class CustomKeyMapping(
    val keyCode: Int,
    val scanCode: Int,
    /** An app package name, or one of [RemoteButtons.ACTION_SYSTEM_SETTINGS], [RemoteButtons.ACTION_SYSTEM_ACCOUNTS], [RemoteButtons.ACTION_LIVE_TV]. */
    val target: String,
) {
    val id: String get() = "${keyCode}_$scanCode"
}

object RemoteButtons {
    const val TARGET_DEFAULT = "default"
    const val TARGET_DISABLED = "disabled"

    const val ACTION_SYSTEM_SETTINGS = "action:system_settings"
    const val ACTION_SYSTEM_ACCOUNTS = "action:system_accounts"
    const val ACTION_LIVE_TV = "action:live_tv"

    /**
     * Active listener when the user is inside the "Nhận diện phím" dialog in CustomizeDialog.
     * Receives (keyCode, scanCode) and returns true if captured.
     */
    @Volatile
    var keyCaptureListener: ((Int, Int) -> Unit)? = null

    private val RESERVED_KEYS = setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_SLEEP,
        KeyEvent.KEYCODE_WAKEUP,
    )

    fun isCapturableKey(event: KeyEvent): Boolean {
        if (event.keyCode in RESERVED_KEYS) return false
        return event.keyCode != KeyEvent.KEYCODE_UNKNOWN || event.scanCode > 0
    }

    fun formatKeyLabel(keyCode: Int, scanCode: Int): String {
        return if (keyCode != KeyEvent.KEYCODE_UNKNOWN && keyCode > 0) {
            KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        } else if (scanCode > 0) {
            "SCANCODE_$scanCode"
        } else {
            "UNKNOWN"
        }
    }

    /**
     * Handles a [KeyEvent] from AccessibilityService or MainActivity.
     * Returns true if the event was consumed.
     */
    fun handleKeyEvent(context: Context, event: KeyEvent): Boolean {
        val capture = keyCaptureListener
        if (capture != null) {
            if (isCapturableKey(event)) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    capture(event.keyCode, event.scanCode)
                }
                return true
            }
            return false
        }

        if (event.keyCode in RESERVED_KEYS) return false

        val prefs = LauncherPrefs(context)
        val customMatched = findMatchingCustomMapping(prefs, event)
        if (customMatched != null) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
                executeCustomTarget(context, customMatched.target, formatKeyLabel(customMatched.keyCode, customMatched.scanCode))
            }
            return true
        }

        val matched = findMatchingButton(prefs, event) ?: return false
        val target = prefs.remoteButtonTarget(matched)
        if (target == TARGET_DISABLED) return false

        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
            executeAction(context, matched, target)
        }
        return true
    }

    private fun findMatchingCustomMapping(prefs: LauncherPrefs, event: KeyEvent): CustomKeyMapping? {
        val mappings = prefs.customKeyMappings()
        if (mappings.isEmpty()) return null
        if (event.keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            mappings.firstOrNull { it.keyCode > 0 && it.keyCode == event.keyCode }?.let { return it }
        } else if (event.scanCode > 0 && event.scanCode != 240) {
            mappings.firstOrNull { it.keyCode <= 0 && it.scanCode > 0 && it.scanCode == event.scanCode }?.let { return it }
        }
        return null
    }

    private fun findMatchingButton(prefs: LauncherPrefs, event: KeyEvent): RemoteButton? {
        // 1. Prefer matching by explicit learned Android keyCode first.
        if (event.keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            for (button in RemoteButton.entries) {
                val customKey = prefs.remoteButtonCustomKeyCode(button)
                if (customKey > 0 && event.keyCode == customKey) return button
            }
        } else if (event.scanCode > 0 && event.scanCode != 240) {
            // 2. Only fall back to hardware scanCode when Android keyCode is UNKNOWN
            //    and scanCode is not the generic Linux KEY_UNKNOWN (240).
            for (button in RemoteButton.entries) {
                val customKey = prefs.remoteButtonCustomKeyCode(button)
                val customScan = prefs.remoteButtonCustomScanCode(button)
                if (customKey <= 0 && customScan > 0 && event.scanCode == customScan) return button
            }
        }
        // 3. Fall back to standard keyCodes for each button.
        if (event.keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            for (button in RemoteButton.entries) {
                if (event.keyCode in button.defaultKeyCodes) return button
            }
        }
        return null
    }

    fun executeAction(context: Context, button: RemoteButton, target: String) {
        when (target) {
            TARGET_DISABLED -> Unit
            TARGET_DEFAULT -> executeDefaultAction(context, button)
            else -> executeCustomTarget(context, target, button.title)
        }
    }

    fun executeCustomTarget(context: Context, target: String, buttonName: String) {
        when (target) {
            ACTION_SYSTEM_SETTINGS -> AppActions.openSystemSettings(context)
            ACTION_SYSTEM_ACCOUNTS -> AppActions.openAccountsSettings(context)
            ACTION_LIVE_TV -> openDefaultGuide(context)
            else -> {
                if (!launchPackage(context, target)) {
                    Toast.makeText(
                        context,
                        "Không mở được ứng dụng đã gán cho $buttonName",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun formatCustomTargetLabel(target: String, appsByPkg: Map<String, LauncherApp>): String =
        when (target) {
            ACTION_SYSTEM_SETTINGS -> "Mở Cài đặt hệ thống"
            ACTION_SYSTEM_ACCOUNTS -> "Mở Tài khoản & Đăng nhập"
            ACTION_LIVE_TV -> "Mở Truyền hình / Live TV"
            else -> "Mở ứng dụng: ${appsByPkg[target]?.label ?: target}"
        }

    private fun executeDefaultAction(context: Context, button: RemoteButton) {
        when (button) {
            RemoteButton.SETTINGS -> AppActions.openSystemSettings(context)
            RemoteButton.PROFILE -> AppActions.openAccountsSettings(context)
            RemoteButton.YOUTUBE -> openDefaultYouTube(context)
            RemoteButton.GUIDE -> openDefaultGuide(context)
        }
    }

    private fun openDefaultYouTube(context: Context) {
        val candidates = listOf(
            "com.google.android.youtube.tv",
            "com.liskovsoft.smarttubetv.beta",
            "com.liskovsoft.smarttubetv",
            "com.google.android.youtube",
            "com.google.android.youtube.tvkids",
        )
        for (pkg in candidates) {
            if (launchPackage(context, pkg)) return
        }
        Toast.makeText(
            context,
            "Không tìm thấy YouTube. Hãy chọn ứng dụng trong Tùy chỉnh › Gán phím điều khiển",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun openDefaultGuide(context: Context) {
        val inputs = TvInputs.loadInputs(context)
        val liveTvInput = inputs.firstOrNull { !it.isPassthrough } ?: inputs.firstOrNull()
        if (liveTvInput != null) {
            TvInputs.openInput(context, liveTvInput)
            return
        }
        val tvPackages = listOf(
            "com.google.android.tv",
            "com.android.tv",
        )
        for (pkg in tvPackages) {
            if (launchPackage(context, pkg)) return
        }
        Toast.makeText(
            context,
            "Hãy gán ứng dụng cho Phím Guide trong Tùy chỉnh › Gán phím điều khiển",
            Toast.LENGTH_LONG,
        ).show()
    }

    fun launchPackage(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.safeStart(intent)
        return true
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.safeStart(intent)
        } else {
            AppActions.openSystemSettings(context)
        }
    }

    /**
     * Whether AccessibilityService should intercept KEYCODE_HOME.
     * Defaults to false when stock launcher (launcherx) is disabled (Toolkit is already default home),
     * or true when stock launcher is still enabled, unless the user explicitly toggled the preference.
     */
    fun shouldOverrideHomeKey(context: Context): Boolean {
        val prefs = LauncherPrefs(context)
        if (prefs.hasOverrideHomeKeyPref()) {
            return prefs.isOverrideHomeKeyEnabled()
        }
        val stockEnabled = runCatching {
            context.packageManager.getApplicationInfo("com.google.android.apps.tv.launcherx", 0).enabled
        }.getOrDefault(false)
        return stockEnabled
    }
}

