package com.tk.toolkit.tv.core

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.tk.toolkit.tv.MainActivity
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart
import com.tk.toolkit.tv.launcher.data.AppChannelGroup
import com.tk.toolkit.tv.launcher.data.AppLanguage
import com.tk.toolkit.tv.launcher.data.AppLockManager
import com.tk.toolkit.tv.launcher.data.AppRepository
import com.tk.toolkit.tv.launcher.data.ContinueWatching
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.LauncherPrefs
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.launcher.data.RemoteButtons
import com.tk.toolkit.tv.launcher.data.TvInputEntry
import com.tk.toolkit.tv.launcher.data.TvInputs
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.launcher.data.WatchNextRepository
import com.tk.toolkit.tv.launcher.ui.ClockDateDisplay
import com.tk.toolkit.tv.launcher.ui.TizenBarLayout
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import com.tk.toolkit.tv.ui.theme.ToolkitTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

object FloatBarOverlayManager {

    private var rootContainer: FrameLayout? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var overlayScope: CoroutineScope? = null

    fun isShowing(): Boolean = rootContainer != null

    fun show(service: AccessibilityService, autoHideAfterMs: Long = 0L) {
        if (isShowing()) return
        val wm = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val prefs = LauncherPrefs(service)
        ToolkitColors.currentPalette = prefs.themePalette()
        val localizedContext = LocaleHelper.wrapContext(service, prefs.language())

        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        var userInteracted by mutableStateOf(false)

        val container = object : FrameLayout(localizedContext) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                userInteracted = true
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    val consumed = super.dispatchKeyEvent(event)
                    if (!consumed) {
                        dismiss(service)
                        return true
                    }
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        container.setViewTreeLifecycleOwner(owner)
        container.setViewTreeSavedStateRegistryOwner(owner)

        val composeView = ComposeView(localizedContext).apply {
            setContent {
                val language = remember { AppLanguage.fromCode(prefs.language()) }
                val showWatchRow = remember { prefs.isWatchRowVisible() }
                val clockColorPreset = remember { prefs.clockColorPreset() }
                val dateColorPreset = remember { prefs.dateColorPreset() }
                val clockGradientEnabled = remember { prefs.isClockGradientEnabled() }
                val searchTarget = remember { prefs.searchShortcutTarget() }
                val lockActive = remember { prefs.isAppLockEnabled() && prefs.hasAppLockPin() }
                val lockedPackages = remember {
                    buildSet {
                        if (lockActive) {
                            addAll(prefs.lockedPackages())
                            if (prefs.isLockSystemSettingsEnabled()) {
                                addAll(AppLockManager.SETTINGS_PACKAGES)
                            }
                        }
                    }
                }

                var apps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
                var hiddenApps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
                var watchItems by remember { mutableStateOf<List<WatchItem>>(emptyList()) }
                var previewChannels by remember { mutableStateOf<Map<String, List<AppChannelGroup>>>(emptyMap()) }
                var tvInputs by remember { mutableStateOf<List<TvInputEntry>>(emptyList()) }
                var homeReq by remember { mutableIntStateOf(1) }

                LaunchedEffect(Unit) {
                    val repo = AppRepository(localizedContext)
                    val watchRepo = WatchNextRepository(localizedContext)
                    val loadedApps = withContext(Dispatchers.IO) { prefs.sort(repo.loadApps()) }
                    val (hidden, visible) = loadedApps.partition { prefs.isAppHidden(it) }
                    val appsByPkg = loadedApps.associateBy { it.packageName }
                    val removed = prefs.removedWatchItems()
                    val disabledPkgs = prefs.disabledWatchPackages()
                    val loadedWatch = withContext(Dispatchers.IO) {
                        val sys = watchRepo.loadWatchNext(appsByPkg).filter { it.key !in removed }
                        val pinned = prefs.sortWatchItems(ContinueWatching.pinnedItems)
                            .filter { it.key !in removed && (it.packageName in appsByPkg || repo.isInstalled(it.packageName)) }
                            .map { item ->
                                val label = appsByPkg[item.packageName]?.label
                                if (label != null && item.appName.isBlank()) item.copy(appName = label) else item
                            }
                        (sys + pinned).filter { it.packageName !in disabledPkgs }
                    }
                    val loadedChannels = withContext(Dispatchers.IO) {
                        watchRepo.loadPreviewChannels(appsByPkg)
                    }
                    val loadedInputs = withContext(Dispatchers.IO) {
                        TvInputs.loadInputs(localizedContext)
                    }
                    apps = visible
                    hiddenApps = hidden
                    watchItems = loadedWatch
                    previewChannels = loadedChannels
                    tvInputs = loadedInputs
                    homeReq++
                }

                // Auto-hide on wake if user doesn't press any remote button within autoHideAfterMs
                if (autoHideAfterMs > 0L) {
                    LaunchedEffect(userInteracted) {
                        if (!userInteracted) {
                            delay(autoHideAfterMs)
                            if (!userInteracted) {
                                dismiss(service)
                            }
                        }
                    }
                }

                LocaleHelper.ProvideAppLocale(language.code) {
                    ToolkitTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        0.42f to Color.Black.copy(alpha = 0.15f),
                                        0.72f to Color.Black.copy(alpha = 0.62f),
                                        1.0f to Color.Black.copy(alpha = 0.88f),
                                    )
                                ),
                        ) {
                            // Compact clock & date pill in top-right corner of the Float overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(horizontal = 42.dp, vertical = 26.dp),
                            ) {
                                ClockDateDisplay(
                                    language = language,
                                    clockColorPreset = clockColorPreset,
                                    dateColorPreset = dateColorPreset,
                                    clockGradientEnabled = clockGradientEnabled,
                                )
                            }

                            TizenBarLayout(
                                apps = apps,
                                hasMoreApps = hiddenApps.isNotEmpty(),
                                watchItems = watchItems,
                                previewChannelsByPackage = previewChannels,
                                showWatchRow = showWatchRow,
                                tvInputs = tvInputs,
                                lockedPackages = lockedPackages,
                                movingKey = null,
                                availableUpdateVersion = prefs.availableUpdateVersion(),
                                homeRequestCount = homeReq,
                                onLaunchApp = { app ->
                                    dismiss(service)
                                    RemoteButtons.launchPackage(localizedContext, app.packageName)
                                },
                                onAppLongPress = { _, _ ->
                                    // Switch to MainActivity for full app management dialog
                                    dismiss(service)
                                    openMainHome(service, forceGrid = false)
                                },
                                onLaunchWatchItem = { item ->
                                    dismiss(service)
                                    if (item.packageName in lockedPackages &&
                                        !AppLockManager.isTempUnlocked(item.packageName)
                                    ) {
                                        AppLockManager.openGuardForPackage(
                                            context = localizedContext,
                                            packageName = item.packageName,
                                            actionType = PinLockActivity.ACTION_VERIFY_AND_LAUNCH,
                                        )
                                    } else {
                                        val repo = AppRepository(localizedContext)
                                        val app = (apps + hiddenApps).firstOrNull { it.packageName == item.packageName }
                                        val intent = item.intent(fallbackLaunch = { app?.let { repo.launchIntent(it) } })
                                        if (intent != null) {
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            localizedContext.safeStart(intent)
                                        }
                                    }
                                },
                                onWatchItemLongPress = { _ -> },
                                onMoveAppBy = { _ -> },
                                onEndMove = { },
                                onOpenHiddenApps = {
                                    dismiss(service)
                                    openMainHome(service, forceGrid = true)
                                },
                                onOpenCustomize = {
                                    dismiss(service)
                                    openMainHome(service, forceGrid = false)
                                },
                                onOpenSettings = {
                                    dismiss(service)
                                    if (lockActive && prefs.isLockSystemSettingsEnabled()) {
                                        AppLockManager.openGuardForSystemSettings(localizedContext)
                                    } else {
                                        AppActions.openSystemSettings(localizedContext)
                                    }
                                },
                                onOpenAudioOutput = {
                                    dismiss(service)
                                    AppActions.openAudioOutput(localizedContext)
                                },
                                onOpenNetwork = {
                                    dismiss(service)
                                    if (lockActive && prefs.isLockSystemSettingsEnabled()) {
                                        AppLockManager.openGuardForSystemSettings(localizedContext)
                                    } else {
                                        AppActions.openNetworkSettings(localizedContext)
                                    }
                                },
                                onOpenBluetooth = {
                                    dismiss(service)
                                    AppActions.openBluetoothSettings(localizedContext)
                                },
                                onOpenSearch = {
                                    dismiss(service)
                                    AppActions.openSearch(localizedContext, searchTarget)
                                },
                                onStartScreensaver = {
                                    dismiss(service)
                                    AppActions.startScreensaver(localizedContext)
                                },
                                onSelectInput = { input ->
                                    dismiss(service)
                                    TvInputs.openInput(localizedContext, input)
                                },
                                onSwitchToGrid = {
                                    dismiss(service)
                                    openMainHome(service, forceGrid = true)
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        container.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }

        runCatching {
            wm.addView(container, params)
            rootContainer = container
            lifecycleOwner = owner
            overlayScope = scope
        }.onFailure {
            owner.onDestroy()
            scope.cancel()
        }
    }

    fun dismiss(context: Context) {
        val container = rootContainer ?: return
        rootContainer = null
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        runCatching { wm?.removeView(container) }
        runCatching { lifecycleOwner?.onDestroy() }
        lifecycleOwner = null
        runCatching { overlayScope?.cancel() }
        overlayScope = null
    }

    fun openMainHome(context: Context, forceGrid: Boolean) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_FORCE_GRID_ONCE, forceGrid)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
    }
}
