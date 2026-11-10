package com.tk.toolkit.tv.core

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tk.toolkit.tv.MainActivity
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.AutoLaunchManager
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.launcher.data.RemoteButtons

/**
 * System-level helpers that need an accessibility service:
 * - Sends the Home button (and the stock Google TV home screen) to Toolkit, so it works as
 *   the launcher without disabling the stock one.
 * - Drives the system App info screen to press "Force stop" → "OK" after [forceStop].
 */
class ToolkitAccessibilityService : AccessibilityService() {

    private enum class Step { PressForceStop, Confirm }

    private val handler = Handler(Looper.getMainLooper())
    private var target: String? = null
    private var step = Step.PressForceStop
    private val timeout = Runnable { finish(null) }

    private val screenWakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> AutoLaunchManager.onScreenOff()
                Intent.ACTION_SCREEN_ON -> AutoLaunchManager.onScreenOn(context)
            }
        }
    }

    override fun onServiceConnected() {
        instance = this
        runCatching {
            ContextCompat.registerReceiver(
                this,
                screenWakeReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenWakeReceiver) }
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    fun forceStop(packageName: String) {
        target = packageName
        step = Step.PressForceStop
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MS)
        startActivity(
            AppActions.appInfoIntent(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        )
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val keyEvent = event ?: return false
        return runCatching {
            if (keyEvent.keyCode == KeyEvent.KEYCODE_HOME) {
                if (!RemoteButtons.shouldOverrideHomeKey(this)) return false
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    val held = keyEvent.eventTime - keyEvent.downTime
                    if (held >= LONG_PRESS_MS) performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) else openToolkit()
                }
                return true
            }
            RemoteButtons.handleKeyEvent(this, keyEvent)
        }.getOrDefault(false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val accEvent = event ?: return
        runCatching {
            // Catches the stock home showing up some other way (boot, an app exiting to home).
            if (accEvent.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                accEvent.packageName == STOCK_LAUNCHER &&
                accEvent.className?.toString() == STOCK_HOME_ACTIVITY &&
                RemoteButtons.shouldOverrideHomeKey(this)
            ) {
                openToolkit()
                return
            }
            if (target == null) return
            val root = rootInActiveWindow ?: return
            when (step) {
                Step.PressForceStop -> {
                    val node = root.findByText(FORCE_STOP_LABELS) ?: return
                    val clickable = node.clickableAncestor() ?: return
                    if (!clickable.isEnabled || !node.isEnabled) {
                        finish(R.string.toast_already_stopped)
                    } else if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        step = Step.Confirm
                    }
                }
                Step.Confirm -> {
                    val node = root.findByText(CONFIRM_LABELS) ?: return
                    val clickable = node.clickableAncestor() ?: return
                    if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        finish(R.string.toast_force_stopped)
                    }
                }
            }
        }
    }

    private fun openToolkit() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
    }

    private fun finish(messageRes: Int?) {
        handler.removeCallbacks(timeout)
        val wasRunning = target != null
        target = null
        if (!wasRunning) return
        handler.postDelayed({ openToolkit() }, 250)
        val localized = LocaleHelper.wrapContext(this)
        val msg = localized.getString(messageRes ?: R.string.toast_force_stop_failed)
        Toast.makeText(localized, msg, Toast.LENGTH_SHORT).show()
    }

    private fun AccessibilityNodeInfo.findByText(labels: List<String>): AccessibilityNodeInfo? {
        for (label in labels) {
            findAccessibilityNodeInfosByText(label)
                .firstOrNull { it.text?.toString().equals(label, ignoreCase = true) }
                ?.let { return it }
        }
        return null
    }

    private fun AccessibilityNodeInfo.clickableAncestor(): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = this
        while (node != null && !node.isClickable) node = node.parent
        return node
    }

    companion object {
        @Volatile
        var instance: ToolkitAccessibilityService? = null
            private set

        private const val TIMEOUT_MS = 6_000L
        private const val LONG_PRESS_MS = 600L
        private const val STOCK_LAUNCHER = "com.google.android.apps.tv.launcherx"
        private const val STOCK_HOME_ACTIVITY = "com.google.android.apps.tv.launcherx.home.HomeActivity"
        private val FORCE_STOP_LABELS = listOf("Force stop", "Buộc dừng", "Bắt buộc dừng")
        private val CONFIRM_LABELS = listOf("OK", "Ok", "Đồng ý")
    }
}
