package com.tk.toolkit.tv.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tk.toolkit.tv.MainActivity
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.AppLockManager
import com.tk.toolkit.tv.launcher.data.LauncherPrefs
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.launcher.data.RemoteButtons
import com.tk.toolkit.tv.launcher.ui.PinVerifyCard
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import com.tk.toolkit.tv.ui.theme.ToolkitTheme

/**
 * Full-screen PIN prompt used when a locked app or System Settings is launched outside the main
 * launcher grid (for example via System Settings -> App info -> Open, Google Play, or remote keys).
 */
class PinLockActivity : ComponentActivity() {

    private var lockedPackage: String = ""
    private var actionType: String = ACTION_VERIFY_AND_RESUME
    private var unlocked = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        if (lockedPackage.isBlank() && actionType != ACTION_OPEN_SETTINGS) {
            finish()
            return
        }

        val prefs = LauncherPrefs(this)
        ToolkitColors.currentPalette = prefs.themePalette()

        val subtitle = when {
            actionType == ACTION_OPEN_SETTINGS || lockedPackage in AppLockManager.SETTINGS_PACKAGES ->
                getString(R.string.pin_verify_subtitle_settings)
            else -> {
                val appLabel = resolveAppLabel(lockedPackage)
                getString(R.string.pin_verify_subtitle_app, appLabel)
            }
        }

        setContent {
            ToolkitTheme {
                LocaleHelper.ProvideAppLocale {
                    BackHandler { cancelAndReturnHome() }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ToolkitColors.Background),
                        contentAlignment = Alignment.Center,
                    ) {
                        PinVerifyCard(
                            subtitle = subtitle,
                            onVerified = ::onPinVerified,
                            onDismiss = ::cancelAndReturnHome,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        lockedPackage = intent?.getStringExtra(EXTRA_LOCKED_PACKAGE).orEmpty()
        actionType = intent?.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_VERIFY_AND_RESUME
        unlocked = false
        if (actionType == ACTION_VERIFY_AND_RESUME && lockedPackage.isNotBlank()) {
            AppLockManager.onGuardPromptOpened(lockedPackage)
        }
    }

    private fun onPinVerified() {
        unlocked = true
        when (actionType) {
            ACTION_OPEN_SETTINGS -> {
                AppLockManager.grantTempUnlockSettings()
                AppActions.openSystemSettings(this)
            }
            ACTION_VERIFY_AND_LAUNCH -> {
                AppLockManager.grantTempUnlock(lockedPackage)
                RemoteButtons.launchPackageUnchecked(this, lockedPackage)
            }
            else -> {
                AppLockManager.onGuardPromptClosed(lockedPackage, unlocked = true)
            }
        }
        finish()
    }

    private fun cancelAndReturnHome() {
        if (actionType == ACTION_VERIFY_AND_RESUME && lockedPackage.isNotBlank()) {
            AppLockManager.onGuardPromptClosed(lockedPackage, unlocked = false)
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            )
        }
        finish()
    }

    override fun onDestroy() {
        if (!unlocked && actionType == ACTION_VERIFY_AND_RESUME && lockedPackage.isNotBlank()) {
            AppLockManager.onGuardPromptClosed(lockedPackage, unlocked = false)
        }
        super.onDestroy()
    }

    private fun resolveAppLabel(pkg: String): String = runCatching {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString().ifBlank { pkg }
    }.getOrDefault(pkg)

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
        const val EXTRA_ACTION_TYPE = "extra_action_type"

        const val ACTION_VERIFY_AND_RESUME = "verify_and_resume"
        const val ACTION_VERIFY_AND_LAUNCH = "verify_and_launch"
        const val ACTION_OPEN_SETTINGS = "open_settings"

        fun createIntent(context: Context, packageName: String, actionType: String): Intent =
            Intent(context, PinLockActivity::class.java).apply {
                putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                putExtra(EXTRA_ACTION_TYPE, actionType)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
            }
    }
}
