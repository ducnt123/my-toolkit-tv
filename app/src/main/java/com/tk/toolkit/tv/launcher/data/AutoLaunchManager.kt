package com.tk.toolkit.tv.launcher.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart

object AutoLaunchManager {
    const val TARGET_OFF = ""
    const val TARGET_LIVE_TV = "action:live_tv"
    const val INPUT_PREFIX = "input:"

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var bootHandledInProcess = false

    @Volatile
    private var lastScreenOffElapsed = 0L

    @Volatile
    private var lastLaunchElapsed = 0L

    /**
     * Triggered from [com.tk.toolkit.tv.core.BootReceiver], [com.tk.toolkit.tv.MainActivity],
     * or [com.tk.toolkit.tv.core.ToolkitAccessibilityService] shortly after device boot.
     */
    fun checkBootLaunch(context: Context, fromBootBroadcast: Boolean = false) {
        if (bootHandledInProcess) return
        val prefs = LauncherPrefs(context)
        val target = prefs.autoLaunchTarget()
        if (target.isBlank() || !prefs.isAutoLaunchOnBootEnabled()) {
            bootHandledInProcess = true
            return
        }
        // Only treat as a fresh boot if triggered by BOOT_COMPLETED or within 3 minutes of system boot.
        val uptimeMs = SystemClock.elapsedRealtime()
        if (!fromBootBroadcast && uptimeMs > 180_000L) {
            bootHandledInProcess = true
            return
        }
        bootHandledInProcess = true
        val delayMs = if (fromBootBroadcast) 1_500L else 800L
        handler.postDelayed({
            launchTargetIfCooldownPassed(context.applicationContext, target)
        }, delayMs)
    }

    fun onScreenOff() {
        lastScreenOffElapsed = SystemClock.elapsedRealtime()
    }

    fun onScreenOn(context: Context) {
        val offTime = lastScreenOffElapsed
        if (offTime <= 0L) return
        lastScreenOffElapsed = 0L
        val prefs = LauncherPrefs(context)
        val target = prefs.autoLaunchTarget()
        if (target.isBlank() || !prefs.isAutoLaunchOnWakeEnabled()) return
        handler.postDelayed({
            launchTargetIfCooldownPassed(context.applicationContext, target)
        }, 700L)
    }

    private fun launchTargetIfCooldownPassed(context: Context, target: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunchElapsed < 4_000L) return
        lastLaunchElapsed = now
        executeTarget(context, target)
    }

    fun executeTarget(context: Context, target: String) {
        if (target.isBlank()) return
        when {
            target == TARGET_LIVE_TV -> {
                RemoteButtons.executeCustomTarget(context, RemoteButtons.ACTION_LIVE_TV, "Live TV")
            }
            target.startsWith(INPUT_PREFIX) -> {
                val inputId = target.removePrefix(INPUT_PREFIX)
                val matched = TvInputs.loadInputs(context).firstOrNull { it.id == inputId }
                if (matched != null) {
                    TvInputs.openInput(context, matched)
                } else {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        TvContract.buildChannelUriForPassthroughInput(inputId),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.safeStart(intent)
                }
            }
            else -> {
                RemoteButtons.launchPackage(context, target)
            }
        }
    }

    fun formatTargetLabel(
        context: Context,
        target: String,
        appsByPkg: Map<String, LauncherApp>,
        inputs: List<TvInputEntry>,
    ): String {
        if (target.isBlank()) return context.getString(R.string.auto_launch_off)
        if (target == TARGET_LIVE_TV) return context.getString(R.string.remote_action_live_tv)
        if (target.startsWith(INPUT_PREFIX)) {
            val id = target.removePrefix(INPUT_PREFIX)
            val input = inputs.firstOrNull { it.id == id }
            return input?.displayTitle(context) ?: id
        }
        return appsByPkg[target]?.label ?: target
    }
}

