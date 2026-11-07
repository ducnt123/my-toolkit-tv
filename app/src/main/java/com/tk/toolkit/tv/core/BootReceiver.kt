package com.tk.toolkit.tv.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tk.toolkit.tv.launcher.data.AutoLaunchManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            AutoLaunchManager.checkBootLaunch(context, fromBootBroadcast = true)
        }
    }
}

