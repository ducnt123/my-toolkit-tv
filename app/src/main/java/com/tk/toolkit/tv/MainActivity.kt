package com.tk.toolkit.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.tk.toolkit.tv.core.ToolkitAccessibilityService
import com.tk.toolkit.tv.core.ToolkitApp
import com.tk.toolkit.tv.launcher.LauncherViewModel
import com.tk.toolkit.tv.launcher.data.AutoLaunchManager
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.launcher.data.RemoteButtons

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private val launcherViewModel: LauncherViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launcherViewModel.refresh()
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = launcherViewModel.refresh()
    }

    private val screenWakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> AutoLaunchManager.onScreenOff()
                Intent.ACTION_SCREEN_ON -> AutoLaunchManager.onScreenOn(context)
            }
        }
    }

    private val homeKeyReceiver = object : BroadcastReceiver() {
        @Suppress("DEPRECATION")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS &&
                intent.getStringExtra("reason") == "homekey"
            ) {
                launcherViewModel.onHomePressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tvListingsPerm = "android.permission.READ_TV_LISTINGS"
        if (ContextCompat.checkSelfPermission(this, tvListingsPerm) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            runCatching { permissionLauncher.launch(tvListingsPerm) }
        }
        ContextCompat.registerReceiver(
            this,
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        @Suppress("DEPRECATION")
        ContextCompat.registerReceiver(
            this,
            homeKeyReceiver,
            IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS),
            ContextCompat.RECEIVER_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            screenWakeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        AutoLaunchManager.checkBootLaunch(this)
        setContent { ToolkitApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launcherViewModel.onHomePressed()
    }

    override fun onResume() {
        super.onResume()
        launcherViewModel.refresh()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ToolkitAccessibilityService.instance == null && RemoteButtons.handleKeyEvent(this, event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        unregisterReceiver(homeKeyReceiver)
        unregisterReceiver(screenWakeReceiver)
        super.onDestroy()
    }
}
