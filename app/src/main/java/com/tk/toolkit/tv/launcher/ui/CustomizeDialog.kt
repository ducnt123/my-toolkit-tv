package com.tk.toolkit.tv.launcher.ui

import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Input
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.core.ToolkitAccessibilityService
import com.tk.toolkit.tv.launcher.data.AppLanguage
import com.tk.toolkit.tv.launcher.data.AppThemePalette
import com.tk.toolkit.tv.launcher.data.AppUpdateManager
import com.tk.toolkit.tv.launcher.data.AutoLaunchManager
import com.tk.toolkit.tv.launcher.data.BackupManager
import com.tk.toolkit.tv.launcher.data.CustomKeyMapping
import com.tk.toolkit.tv.launcher.data.FolderBrowserState
import com.tk.toolkit.tv.launcher.data.InstallOutcome
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.LauncherPrefs
import com.tk.toolkit.tv.launcher.data.LocalImageEntry
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.launcher.data.ReleaseInfo
import com.tk.toolkit.tv.launcher.data.RemoteButton
import com.tk.toolkit.tv.launcher.data.RemoteButtonConfig
import com.tk.toolkit.tv.launcher.data.RemoteButtons
import com.tk.toolkit.tv.launcher.data.TvInputEntry
import com.tk.toolkit.tv.launcher.data.UpdateCheckInterval
import com.tk.toolkit.tv.launcher.data.UpdateCheckResult
import com.tk.toolkit.tv.launcher.data.WallpaperStore
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val SheetShape = RoundedCornerShape(24.dp)
private val ItemShape = RoundedCornerShape(14.dp)
private val IconBoxShape = RoundedCornerShape(10.dp)
private val SHEET_WIDTH = 420.dp
private val SAFE_AREA_HORIZONTAL = 48.dp
private val SAFE_AREA_VERTICAL = 28.dp

private sealed interface CustomizePage {
    val depth: Int
    data object Main : CustomizePage { override val depth = 0 }

    // Group pages (level 1)
    data object AppearanceGroup : CustomizePage { override val depth = 1 }
    data object ControlsGroup : CustomizePage { override val depth = 1 }
    data object Language : CustomizePage { override val depth = 1 }
    data object SystemGroup : CustomizePage { override val depth = 1 }

    // Appearance & Layout
    data object ThemeWallpaper : CustomizePage { override val depth = 2 }
    data object WallpaperLocalFile : CustomizePage { override val depth = 3 }
    data object WallpaperUrl : CustomizePage { override val depth = 3 }
    data object WatchSources : CustomizePage { override val depth = 2 }

    // Controls & Device behavior
    data object AutoLaunch : CustomizePage { override val depth = 2 }
    data object RemoteButtonsList : CustomizePage { override val depth = 2 }
    data class RemoteButtonDetail(val button: RemoteButton) : CustomizePage { override val depth = 3 }
    data class CaptureKey(val button: RemoteButton) : CustomizePage { override val depth = 4 }
    data object CaptureNewCustomKey : CustomizePage { override val depth = 3 }
    data class CustomKeyDetail(val keyCode: Int, val scanCode: Int, val isNew: Boolean) : CustomizePage { override val depth = 3 }

    // System & Updates
    data object AppUpdate : CustomizePage { override val depth = 2 }
    data object BackupRestore : CustomizePage { override val depth = 2 }
    data object AdvancedSettings : CustomizePage { override val depth = 2 }
}

private class PanelFocusMemory {
    private val focusedKeys = mutableMapOf<Any, String>()
    private val scrollPositions = mutableMapOf<Any, Pair<Int, Int>>()

    fun keyFor(pageKey: Any): String? = focusedKeys[pageKey]

    fun saveKey(pageKey: Any, itemKey: String) {
        focusedKeys[pageKey] = itemKey
    }

    fun scrollFor(pageKey: Any): Pair<Int, Int>? = scrollPositions[pageKey]

    fun saveScroll(pageKey: Any, index: Int, offset: Int) {
        scrollPositions[pageKey] = index to offset
    }

    fun clear(pageKey: Any) {
        focusedKeys.remove(pageKey)
        scrollPositions.remove(pageKey)
    }
}

private class PanelFocusController(
    private val pageKey: Any,
    private val memory: PanelFocusMemory,
    val listState: LazyListState,
    private val fallbackFirstKey: String?,
) {
    private val requesters = mutableMapOf<String, FocusRequester>()

    fun requesterFor(itemKey: String): FocusRequester =
        requesters.getOrPut(itemKey) { FocusRequester() }

    /**
     * Attaches a [FocusRequester] for [itemKey] and remembers it when focused so returning
     * via Back restores focus (and list scroll position) to the same item.
     */
    fun modifierFor(itemKey: String): Modifier = Modifier
        .focusRequester(requesterFor(itemKey))
        .onFocusChanged { focusState ->
            if (focusState.hasFocus) {
                memory.saveKey(pageKey, itemKey)
            }
        }

    fun saveNow(itemKey: String) {
        memory.saveKey(pageKey, itemKey)
        memory.saveScroll(
            pageKey,
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset,
        )
    }

    suspend fun restoreFocus() {
        val savedKey = memory.keyFor(pageKey)
        if (savedKey != null) {
            val target = requesters[savedKey]
            if (target != null && target.tryRequestFocusWhenReady(maxFrames = 8)) {
                return
            }
        }
        val first = fallbackFirstKey?.let { requesters[it] } ?: requesters.values.firstOrNull()
        first?.requestFocusWhenReady()
    }
}

@Composable
private fun rememberPanelFocus(
    pageKey: Any,
    memory: PanelFocusMemory,
    fallbackFirstKey: String? = null,
): PanelFocusController {
    val savedScroll = remember(pageKey) { memory.scrollFor(pageKey) }
    val listState = remember(pageKey) {
        LazyListState(
            firstVisibleItemIndex = savedScroll?.first ?: 0,
            firstVisibleItemScrollOffset = savedScroll?.second ?: 0,
        )
    }
    val controller = remember(pageKey, memory, listState, fallbackFirstKey) {
        PanelFocusController(
            pageKey = pageKey,
            memory = memory,
            listState = listState,
            fallbackFirstKey = fallbackFirstKey,
        )
    }
    DisposableEffect(pageKey, listState) {
        onDispose {
            memory.saveScroll(
                pageKey,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
            )
        }
    }
    return controller
}

@Composable
fun CustomizeDialog(
    showWatchRow: Boolean,
    appColumns: Int,
    backToAmbient: Boolean,
    language: AppLanguage,
    themePalette: AppThemePalette,
    dynamicAmbientColor: Boolean,
    hasCustomWallpaper: Boolean,
    wallpaperDim: Int,
    autoLaunchTarget: String,
    autoLaunchOnBoot: Boolean,
    autoLaunchOnWake: Boolean,
    updateCheckInterval: UpdateCheckInterval,
    availableUpdateVersion: String,
    tvInputs: List<TvInputEntry>,
    apps: List<LauncherApp>,
    disabledWatchPackages: Set<String>,
    activeWatchPackages: Set<String>,
    remoteButtons: List<RemoteButtonConfig>,
    customKeyMappings: List<CustomKeyMapping>,
    overrideHomeKey: Boolean,
    onToggleWatchRow: (Boolean) -> Unit,
    onSetAppColumns: (Int) -> Unit,
    onToggleBackToAmbient: (Boolean) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetThemePalette: (AppThemePalette) -> Unit,
    onSetDynamicAmbient: (Boolean) -> Unit,
    onSetWallpaperFromUri: (Uri, (Boolean) -> Unit) -> Unit,
    onSetWallpaperFromFile: (File, (Boolean) -> Unit) -> Unit,
    onSetWallpaperFromUrl: (String, (Boolean) -> Unit) -> Unit,
    onClearWallpaper: () -> Unit,
    onSetWallpaperDim: (Int) -> Unit,
    onSetAutoLaunchTarget: (String) -> Unit,
    onSetAutoLaunchOnBoot: (Boolean) -> Unit,
    onSetAutoLaunchOnWake: (Boolean) -> Unit,
    onSetUpdateCheckInterval: (UpdateCheckInterval) -> Unit,
    onUpdateCheckResult: (UpdateCheckResult) -> Unit,
    onRestoreCompleted: () -> Unit,
    onToggleWatchPackage: (String, Boolean) -> Unit,
    onSetRemoteButtonTarget: (RemoteButton, String) -> Unit,
    onSetRemoteButtonCustomKey: (RemoteButton, Int, Int) -> Unit,
    onUpsertCustomKeyMapping: (Int, Int, String) -> Unit,
    onRemoveCustomKeyMapping: (Int, Int) -> Unit,
    onToggleOverrideHomeKey: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf<CustomizePage>(CustomizePage.Main) }
    val focusMemory = remember { PanelFocusMemory() }

    fun navigateForward(target: CustomizePage) {
        focusMemory.clear(target)
        page = target
    }

    fun navigateBack() {
        page = when (val current = page) {
            CustomizePage.Main -> {
                onDismiss()
                return
            }
            CustomizePage.AppearanceGroup -> CustomizePage.Main
            CustomizePage.ControlsGroup -> CustomizePage.Main
            CustomizePage.Language -> CustomizePage.Main
            CustomizePage.SystemGroup -> CustomizePage.Main
            CustomizePage.ThemeWallpaper -> CustomizePage.AppearanceGroup
            CustomizePage.WallpaperLocalFile -> CustomizePage.ThemeWallpaper
            CustomizePage.WallpaperUrl -> CustomizePage.ThemeWallpaper
            CustomizePage.WatchSources -> CustomizePage.AppearanceGroup
            CustomizePage.AutoLaunch -> CustomizePage.ControlsGroup
            CustomizePage.RemoteButtonsList -> CustomizePage.ControlsGroup
            is CustomizePage.RemoteButtonDetail -> CustomizePage.RemoteButtonsList
            is CustomizePage.CaptureKey -> CustomizePage.RemoteButtonDetail(current.button)
            CustomizePage.CaptureNewCustomKey -> CustomizePage.RemoteButtonsList
            is CustomizePage.CustomKeyDetail -> CustomizePage.RemoteButtonsList
            CustomizePage.AppUpdate -> CustomizePage.SystemGroup
            CustomizePage.BackupRestore -> CustomizePage.SystemGroup
            CustomizePage.AdvancedSettings -> CustomizePage.SystemGroup
        }
    }

    Dialog(
        onDismissRequest = ::navigateBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setDimAmount(0f)
        }

        LocaleHelper.ProvideAppLocale(language.code) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to Color.Black.copy(alpha = 0.18f),
                            0.45f to Color.Black.copy(alpha = 0.45f),
                            1.0f to Color.Black.copy(alpha = 0.82f),
                        )
                    )
                    .padding(
                        start = SAFE_AREA_HORIZONTAL,
                        end = SAFE_AREA_HORIZONTAL,
                        top = SAFE_AREA_VERTICAL,
                        bottom = SAFE_AREA_VERTICAL,
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Column(
                    Modifier
                        .width(SHEET_WIDTH)
                        .fillMaxHeight()
                        .clip(SheetShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ToolkitColors.SurfaceRaised,
                                    ToolkitColors.Surface,
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), SheetShape)
                        .ignoreHeldSelectKey(),
                ) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            val forward = targetState.depth >= initialState.depth
                            val slideIn = slideInHorizontally(tween(180)) { if (forward) it / 4 else -it / 4 } +
                                fadeIn(tween(180))
                            val slideOut = slideOutHorizontally(tween(150)) { if (forward) -it / 4 else it / 4 } +
                                fadeOut(tween(150))
                            slideIn togetherWith slideOut
                        },
                        label = "customizeSideSheetPage",
                        modifier = Modifier.fillMaxSize(),
                    ) { current ->
                        when (current) {
                            CustomizePage.Main -> {
                                MainCustomizePanel(
                                    focusMemory = focusMemory,
                                    appColumns = appColumns,
                                    language = language,
                                    themePalette = themePalette,
                                    availableUpdateVersion = availableUpdateVersion,
                                    onSetLanguage = onSetLanguage,
                                    onOpenAppearance = { navigateForward(CustomizePage.AppearanceGroup) },
                                    onOpenControls = { navigateForward(CustomizePage.ControlsGroup) },
                                    onOpenLanguage = { navigateForward(CustomizePage.Language) },
                                    onOpenSystem = { navigateForward(CustomizePage.SystemGroup) },
                                    onRefresh = onRefresh,
                                )
                            }
                            CustomizePage.AppearanceGroup -> {
                                AppearanceGroupPanel(
                                    focusMemory = focusMemory,
                                    showWatchRow = showWatchRow,
                                    appColumns = appColumns,
                                    themePalette = themePalette,
                                    enabledCount = apps.count { it.packageName !in disabledWatchPackages },
                                    totalCount = apps.size,
                                    onToggleWatchRow = onToggleWatchRow,
                                    onSetAppColumns = onSetAppColumns,
                                    onOpenThemeWallpaper = { navigateForward(CustomizePage.ThemeWallpaper) },
                                    onOpenSources = { navigateForward(CustomizePage.WatchSources) },
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.ControlsGroup -> {
                                ControlsGroupPanel(
                                    focusMemory = focusMemory,
                                    backToAmbient = backToAmbient,
                                    autoLaunchTarget = autoLaunchTarget,
                                    apps = apps,
                                    tvInputs = tvInputs,
                                    onToggleBackToAmbient = onToggleBackToAmbient,
                                    onOpenAutoLaunch = { navigateForward(CustomizePage.AutoLaunch) },
                                    onOpenRemoteButtons = { navigateForward(CustomizePage.RemoteButtonsList) },
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.SystemGroup -> {
                                SystemGroupPanel(
                                    focusMemory = focusMemory,
                                    availableUpdateVersion = availableUpdateVersion,
                                    onOpenAppUpdate = { navigateForward(CustomizePage.AppUpdate) },
                                    onOpenBackupRestore = { navigateForward(CustomizePage.BackupRestore) },
                                    onOpenAdvanced = { navigateForward(CustomizePage.AdvancedSettings) },
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.AdvancedSettings -> {
                                AdvancedSettingsPanel(onBack = ::navigateBack)
                            }
                            CustomizePage.ThemeWallpaper -> {
                                ThemeWallpaperPanel(
                                    focusMemory = focusMemory,
                                    currentPalette = themePalette,
                                    dynamicAmbient = dynamicAmbientColor,
                                    hasCustomWallpaper = hasCustomWallpaper,
                                    wallpaperDim = wallpaperDim,
                                    onSelectPalette = onSetThemePalette,
                                    onToggleDynamicAmbient = onSetDynamicAmbient,
                                    onSetWallpaperDim = onSetWallpaperDim,
                                    onOpenLocalFile = { navigateForward(CustomizePage.WallpaperLocalFile) },
                                    onOpenUrl = { navigateForward(CustomizePage.WallpaperUrl) },
                                    onClearWallpaper = onClearWallpaper,
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.WallpaperLocalFile -> {
                                val context = LocalContext.current
                                WallpaperLocalFilePanel(
                                    focusMemory = focusMemory,
                                    onSelectUri = { uri ->
                                        onSetWallpaperFromUri(uri) { ok ->
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    if (ok) R.string.toast_wallpaper_applied else R.string.toast_wallpaper_failed
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            if (ok) page = CustomizePage.ThemeWallpaper
                                        }
                                    },
                                    onSelectFile = { file ->
                                        onSetWallpaperFromFile(file) { ok ->
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    if (ok) R.string.toast_wallpaper_applied else R.string.toast_wallpaper_failed
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            if (ok) page = CustomizePage.ThemeWallpaper
                                        }
                                    },
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.WallpaperUrl -> {
                                val context = LocalContext.current
                                WallpaperUrlPanel(
                                    onApplyUrl = { url ->
                                        onSetWallpaperFromUrl(url) { ok ->
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    if (ok) R.string.toast_wallpaper_applied else R.string.toast_wallpaper_failed
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            if (ok) page = CustomizePage.ThemeWallpaper
                                        }
                                    },
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.Language -> {
                                LanguagePanel(
                                    currentLanguage = language,
                                    onSelectLanguage = onSetLanguage,
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.WatchSources -> {
                                WatchSourcesPanel(
                                    apps = apps,
                                    disabledWatchPackages = disabledWatchPackages,
                                    activeWatchPackages = activeWatchPackages,
                                    onToggleWatchPackage = onToggleWatchPackage,
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.AutoLaunch -> {
                                AutoLaunchPanel(
                                    currentTarget = autoLaunchTarget,
                                    onBoot = autoLaunchOnBoot,
                                    onWake = autoLaunchOnWake,
                                    tvInputs = tvInputs,
                                    apps = apps,
                                    onToggleBoot = onSetAutoLaunchOnBoot,
                                    onToggleWake = onSetAutoLaunchOnWake,
                                    onSelectTarget = onSetAutoLaunchTarget,
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.BackupRestore -> {
                                BackupRestorePanel(
                                    onRestoreCompleted = onRestoreCompleted,
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.AppUpdate -> {
                                AppUpdatePanel(
                                    updateCheckInterval = updateCheckInterval,
                                    onSetUpdateCheckInterval = onSetUpdateCheckInterval,
                                    onUpdateCheckResult = onUpdateCheckResult,
                                    onBack = ::navigateBack,
                                )
                            }
                            CustomizePage.RemoteButtonsList -> {
                                RemoteButtonsPanel(
                                    focusMemory = focusMemory,
                                    configs = remoteButtons,
                                    customMappings = customKeyMappings,
                                    apps = apps,
                                    overrideHomeKey = overrideHomeKey,
                                    onToggleOverrideHomeKey = onToggleOverrideHomeKey,
                                    onSelectButton = { button -> navigateForward(CustomizePage.RemoteButtonDetail(button)) },
                                    onSelectCustomMapping = { mapping ->
                                        navigateForward(
                                            CustomizePage.CustomKeyDetail(
                                                keyCode = mapping.keyCode,
                                                scanCode = mapping.scanCode,
                                                isNew = false,
                                            ),
                                        )
                                    },
                                    onAddCustomKey = { navigateForward(CustomizePage.CaptureNewCustomKey) },
                                    onBack = ::navigateBack,
                                )
                            }
                            is CustomizePage.RemoteButtonDetail -> {
                                val config = remoteButtons.firstOrNull { it.button == current.button }
                                    ?: RemoteButtonConfig(current.button)
                                RemoteButtonDetailPanel(
                                    pageKey = current,
                                    focusMemory = focusMemory,
                                    config = config,
                                    apps = apps,
                                    onSelectTarget = { target ->
                                        onSetRemoteButtonTarget(current.button, target)
                                        page = CustomizePage.RemoteButtonsList
                                    },
                                    onStartCaptureKey = { navigateForward(CustomizePage.CaptureKey(current.button)) },
                                    onClearCustomKey = {
                                        onSetRemoteButtonCustomKey(current.button, 0, 0)
                                    },
                                    onBack = ::navigateBack,
                                )
                            }
                            is CustomizePage.CaptureKey -> {
                                val context = LocalContext.current
                                val buttonTitle = stringResource(current.button.titleRes)
                                CaptureKeyPanel(
                                    breadcrumb = stringResource(R.string.customize_remote_buttons),
                                    title = stringResource(R.string.remote_capture_button_title, buttonTitle),
                                    promptText = stringResource(R.string.remote_capture_button_prompt, buttonTitle),
                                    onKeyCaptured = { keyCode, scanCode ->
                                        val label = RemoteButtons.formatKeyLabel(keyCode, scanCode)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_assigned_key_to_button, label, buttonTitle),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        onSetRemoteButtonCustomKey(current.button, keyCode, scanCode)
                                        page = CustomizePage.RemoteButtonDetail(current.button)
                                    },
                                    onCancel = ::navigateBack,
                                )
                            }
                            CustomizePage.CaptureNewCustomKey -> {
                                CaptureKeyPanel(
                                    breadcrumb = stringResource(R.string.customize_remote_buttons),
                                    title = stringResource(R.string.remote_add_custom_key),
                                    promptText = stringResource(R.string.remote_capture_custom_prompt),
                                    onKeyCaptured = { keyCode, scanCode ->
                                        val effectiveScan = if (keyCode > 0 || scanCode == 240) 0 else scanCode
                                        page = CustomizePage.CustomKeyDetail(
                                            keyCode = keyCode,
                                            scanCode = effectiveScan,
                                            isNew = true,
                                        )
                                    },
                                    onCancel = ::navigateBack,
                                )
                            }
                            is CustomizePage.CustomKeyDetail -> {
                                val existingTarget = customKeyMappings.firstOrNull {
                                    (current.keyCode > 0 && it.keyCode == current.keyCode) ||
                                        (current.keyCode <= 0 && current.scanCode > 0 && it.scanCode == current.scanCode)
                                }?.target
                                CustomKeyDetailPanel(
                                    keyCode = current.keyCode,
                                    scanCode = current.scanCode,
                                    currentTarget = existingTarget,
                                    canRemove = !current.isNew || existingTarget != null,
                                    apps = apps,
                                    onSelectTarget = { target ->
                                        onUpsertCustomKeyMapping(current.keyCode, current.scanCode, target)
                                        page = CustomizePage.RemoteButtonsList
                                    },
                                    onRemove = {
                                        onRemoveCustomKeyMapping(current.keyCode, current.scanCode)
                                        page = CustomizePage.RemoteButtonsList
                                    },
                                    onBack = ::navigateBack,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainCustomizePanel(
    focusMemory: PanelFocusMemory,
    appColumns: Int,
    language: AppLanguage,
    themePalette: AppThemePalette,
    availableUpdateVersion: String,
    onSetLanguage: (AppLanguage) -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenSystem: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val focusController = rememberPanelFocus(
        pageKey = CustomizePage.Main,
        memory = focusMemory,
        fallbackFirstKey = "group_appearance",
    )
    val versionName = remember(context) { AppUpdateManager.currentVersionName(context) }
    val hasUpdate = availableUpdateVersion.isNotBlank()
    LaunchedEffect(Unit) { focusController.restoreFocus() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            title = stringResource(R.string.customize_title),
            subtitle = stringResource(R.string.customize_subtitle),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "group_appearance") {
                TvSettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.group_appearance),
                    subtitle = stringResource(
                        R.string.group_appearance_subtitle,
                        stringResource(themePalette.titleRes),
                        appColumns,
                    ),
                    onClick = {
                        focusController.saveNow("group_appearance")
                        onOpenAppearance()
                    },
                    modifier = focusController.modifierFor("group_appearance"),
                    showChevron = true,
                )
            }
            item(key = "group_controls") {
                TvSettingsItem(
                    icon = Icons.Outlined.SettingsRemote,
                    title = stringResource(R.string.group_controls),
                    subtitle = stringResource(R.string.group_controls_subtitle),
                    onClick = {
                        focusController.saveNow("group_controls")
                        onOpenControls()
                    },
                    modifier = focusController.modifierFor("group_controls"),
                    showChevron = true,
                )
            }
            item(key = "group_language") {
                LanguageQuickItem(
                    title = stringResource(R.string.group_language),
                    language = language,
                    onSetLanguage = onSetLanguage,
                    onClick = {
                        focusController.saveNow("group_language")
                        onOpenLanguage()
                    },
                    modifier = focusController.modifierFor("group_language"),
                )
            }
            item(key = "group_system") {
                TvSettingsItem(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.group_system),
                    subtitle = if (hasUpdate) {
                        stringResource(R.string.update_subtitle_available, availableUpdateVersion)
                    } else {
                        stringResource(R.string.group_system_subtitle, versionName)
                    },
                    onClick = {
                        focusController.saveNow("group_system")
                        onOpenSystem()
                    },
                    modifier = focusController.modifierFor("group_system"),
                    showChevron = !hasUpdate,
                    trailingContent = if (hasUpdate) {
                        { NewBadgeWithChevron() }
                    } else null,
                )
            }
            item(key = "refresh") {
                TvSettingsItem(
                    icon = Icons.Outlined.Refresh,
                    title = stringResource(R.string.customize_refresh),
                    subtitle = stringResource(R.string.customize_refresh_subtitle),
                    onClick = {
                        onRefresh()
                        Toast.makeText(context, context.getString(R.string.toast_refreshed), Toast.LENGTH_SHORT).show()
                    },
                    modifier = focusController.modifierFor("refresh"),
                )
            }
        }
    }
}

@Composable
private fun AppearanceGroupPanel(
    focusMemory: PanelFocusMemory,
    showWatchRow: Boolean,
    appColumns: Int,
    themePalette: AppThemePalette,
    enabledCount: Int,
    totalCount: Int,
    onToggleWatchRow: (Boolean) -> Unit,
    onSetAppColumns: (Int) -> Unit,
    onOpenThemeWallpaper: () -> Unit,
    onOpenSources: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val focusController = rememberPanelFocus(
        pageKey = CustomizePage.AppearanceGroup,
        memory = focusMemory,
        fallbackFirstKey = "theme_wallpaper",
    )
    LaunchedEffect(Unit) { focusController.restoreFocus() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_title),
            title = stringResource(R.string.group_appearance),
            subtitle = stringResource(R.string.customize_theme_wallpaper_subtitle),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "theme_wallpaper") {
                TvSettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.customize_theme_wallpaper),
                    subtitle = stringResource(themePalette.titleRes),
                    onClick = {
                        focusController.saveNow("theme_wallpaper")
                        onOpenThemeWallpaper()
                    },
                    modifier = focusController.modifierFor("theme_wallpaper"),
                    showChevron = true,
                )
            }
            item(key = "app_columns") {
                AppGridSizeItem(
                    appColumns = appColumns,
                    onSetAppColumns = onSetAppColumns,
                    modifier = focusController.modifierFor("app_columns"),
                )
            }
            item(key = "show_watch_row") {
                TvSettingsItem(
                    icon = Icons.Outlined.PlayCircleOutline,
                    title = stringResource(R.string.customize_show_watch_row),
                    subtitle = if (showWatchRow) stringResource(R.string.customize_show_watch_row_on)
                    else stringResource(R.string.customize_show_watch_row_off),
                    onClick = { onToggleWatchRow(!showWatchRow) },
                    modifier = focusController.modifierFor("show_watch_row"),
                    trailingContent = { TvSwitch(checked = showWatchRow) },
                )
            }
            item(key = "watch_sources") {
                TvSettingsItem(
                    icon = Icons.Outlined.VideoLibrary,
                    title = stringResource(R.string.customize_watch_sources),
                    subtitle = stringResource(R.string.customize_watch_sources_subtitle, enabledCount, totalCount),
                    onClick = {
                        focusController.saveNow("watch_sources")
                        onOpenSources()
                    },
                    modifier = focusController.modifierFor("watch_sources"),
                    showChevron = true,
                )
            }
        }
    }
}

@Composable
private fun ControlsGroupPanel(
    focusMemory: PanelFocusMemory,
    backToAmbient: Boolean,
    autoLaunchTarget: String,
    apps: List<LauncherApp>,
    tvInputs: List<TvInputEntry>,
    onToggleBackToAmbient: (Boolean) -> Unit,
    onOpenAutoLaunch: () -> Unit,
    onOpenRemoteButtons: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val focusController = rememberPanelFocus(
        pageKey = CustomizePage.ControlsGroup,
        memory = focusMemory,
        fallbackFirstKey = "auto_launch",
    )
    val appsByPkg = remember(apps) { apps.associateBy { it.packageName } }
    LaunchedEffect(Unit) { focusController.restoreFocus() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_title),
            title = stringResource(R.string.group_controls),
            subtitle = stringResource(R.string.group_controls_subtitle),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "auto_launch") {
                TvSettingsItem(
                    icon = Icons.Outlined.PowerSettingsNew,
                    title = stringResource(R.string.customize_auto_launch),
                    subtitle = AutoLaunchManager.formatTargetLabel(context, autoLaunchTarget, appsByPkg, tvInputs),
                    onClick = {
                        focusController.saveNow("auto_launch")
                        onOpenAutoLaunch()
                    },
                    modifier = focusController.modifierFor("auto_launch"),
                    showChevron = true,
                )
            }
            item(key = "remote_buttons") {
                TvSettingsItem(
                    icon = Icons.Outlined.SettingsRemote,
                    title = stringResource(R.string.customize_remote_buttons),
                    subtitle = stringResource(R.string.customize_remote_buttons_subtitle),
                    onClick = {
                        focusController.saveNow("remote_buttons")
                        onOpenRemoteButtons()
                    },
                    modifier = focusController.modifierFor("remote_buttons"),
                    showChevron = true,
                )
            }
            item(key = "back_to_ambient") {
                TvSettingsItem(
                    icon = Icons.Outlined.Bedtime,
                    title = stringResource(R.string.customize_back_to_ambient),
                    subtitle = if (backToAmbient) stringResource(R.string.customize_back_to_ambient_on)
                    else stringResource(R.string.customize_back_to_ambient_off),
                    onClick = { onToggleBackToAmbient(!backToAmbient) },
                    modifier = focusController.modifierFor("back_to_ambient"),
                    trailingContent = { TvSwitch(checked = backToAmbient) },
                )
            }
        }
    }
}

private const val ADVANCED_TAP_TARGET = 5
private const val ADVANCED_TAP_RESET_MS = 3_000L

@Composable
private fun SystemGroupPanel(
    focusMemory: PanelFocusMemory,
    availableUpdateVersion: String,
    onOpenAppUpdate: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val prefs = remember(context) { LauncherPrefs(context) }
    val focusController = rememberPanelFocus(
        pageKey = CustomizePage.SystemGroup,
        memory = focusMemory,
        fallbackFirstKey = "version_info",
    )
    val versionName = remember(context) { AppUpdateManager.currentVersionName(context) }
    val versionCode = remember(context) { AppUpdateManager.currentVersionCode(context) }
    val hasUpdate = availableUpdateVersion.isNotBlank()

    var advancedVisible by remember { mutableStateOf(prefs.isAdvancedSettingsVisible()) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var activeToast by remember { mutableStateOf<Toast?>(null) }

    fun showToast(message: String) {
        activeToast?.cancel()
        activeToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).also { it.show() }
    }

    fun onVersionTapped() {
        val now = System.currentTimeMillis()
        tapCount = if (now - lastTapAt > ADVANCED_TAP_RESET_MS) 1 else tapCount + 1
        lastTapAt = now
        val remaining = ADVANCED_TAP_TARGET - tapCount
        when {
            remaining <= 0 -> {
                advancedVisible = !advancedVisible
                prefs.setAdvancedSettingsVisible(advancedVisible)
                tapCount = 0
                showToast(
                    context.getString(
                        if (advancedVisible) R.string.toast_advanced_shown else R.string.toast_advanced_hidden,
                    ),
                )
            }
            tapCount >= 2 -> showToast(
                context.getString(
                    if (advancedVisible) R.string.toast_advanced_taps_to_hide else R.string.toast_advanced_taps_to_show,
                    remaining,
                ),
            )
        }
    }

    LaunchedEffect(Unit) { focusController.restoreFocus() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_title),
            title = stringResource(R.string.group_system),
            subtitle = stringResource(R.string.group_system_subtitle, versionName),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "version_info") {
                TvSettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.system_version_title),
                    subtitle = stringResource(R.string.system_version_subtitle, versionName, versionCode),
                    onClick = ::onVersionTapped,
                    modifier = focusController.modifierFor("version_info"),
                )
            }
            item(key = "check_update") {
                TvSettingsItem(
                    icon = Icons.Outlined.SystemUpdateAlt,
                    title = stringResource(R.string.system_check_update),
                    subtitle = if (hasUpdate) {
                        stringResource(R.string.update_subtitle_available, availableUpdateVersion)
                    } else {
                        stringResource(R.string.update_check_now_subtitle)
                    },
                    onClick = {
                        focusController.saveNow("check_update")
                        onOpenAppUpdate()
                    },
                    modifier = focusController.modifierFor("check_update"),
                    showChevron = !hasUpdate,
                    trailingContent = if (hasUpdate) {
                        { NewBadgeWithChevron() }
                    } else null,
                )
            }
            item(key = "backup_restore") {
                TvSettingsItem(
                    icon = Icons.Outlined.Backup,
                    title = stringResource(R.string.customize_backup_restore),
                    subtitle = stringResource(R.string.customize_backup_restore_subtitle),
                    onClick = {
                        focusController.saveNow("backup_restore")
                        onOpenBackupRestore()
                    },
                    modifier = focusController.modifierFor("backup_restore"),
                    showChevron = true,
                )
            }
            if (advancedVisible) {
                item(key = "advanced_settings") {
                    TvSettingsItem(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(R.string.advanced_title),
                        subtitle = stringResource(R.string.advanced_subtitle),
                        onClick = {
                            focusController.saveNow("advanced_settings")
                            onOpenAdvanced()
                        },
                        modifier = focusController.modifierFor("advanced_settings"),
                        showChevron = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsPanel(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocusWhenReady() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.group_system),
            title = stringResource(R.string.advanced_title),
            subtitle = stringResource(R.string.advanced_subtitle),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.advanced_empty),
                style = MaterialTheme.typography.bodySmall,
                color = ToolkitColors.OnSurfaceMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            // Placeholder focus target so D-pad/Back keep working on an empty page.
            TvSettingsItem(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                title = stringResource(R.string.common_back),
                subtitle = stringResource(R.string.group_system),
                onClick = onBack,
                modifier = Modifier.focusRequester(focus),
            )
        }
    }
}

@Composable
private fun AppGridSizeItem(
    appColumns: Int,
    onSetAppColumns: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TvSettingsItem(
        icon = Icons.Outlined.GridView,
        title = stringResource(R.string.customize_app_grid),
        subtitle = stringResource(R.string.customize_app_grid_subtitle, appColumns),
        onClick = {
            val next = if (appColumns >= 6) 4 else appColumns + 1
            onSetAppColumns(next)
        },
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (appColumns > 4) onSetAppColumns(appColumns - 1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (appColumns < 6) onSetAppColumns(appColumns + 1)
                    true
                }
                else -> false
            }
        },
        trailingContent = {
            PillSelector(
                labels = listOf("4", "5", "6"),
                activeIndex = (appColumns - 4).coerceIn(0, 2),
            )
        },
    )
}

@Composable
private fun LanguageQuickItem(
    title: String,
    language: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val languages = AppLanguage.entries
    TvSettingsItem(
        icon = Icons.Outlined.Language,
        title = title,
        subtitle = language.nativeName,
        onClick = onClick,
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val idx = languages.indexOf(language).coerceAtLeast(0)
            when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    onSetLanguage(languages[(idx - 1 + languages.size) % languages.size])
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    onSetLanguage(languages[(idx + 1) % languages.size])
                    true
                }
                else -> false
            }
        },
        trailingContent = {
            PillSelector(
                labels = languages.map { it.code.uppercase() },
                activeIndex = languages.indexOf(language),
            )
        },
    )
}

@Composable
private fun PillSelector(labels: List<String>, activeIndex: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == activeIndex
            val pillShape = RoundedCornerShape(6.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        if (active) ToolkitColors.Accent else ToolkitColors.OnSurface.copy(alpha = 0.08f),
                        pillShape,
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) ToolkitColors.Background else ToolkitColors.OnSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun NewBadgeWithChevron() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(ToolkitColors.Accent, CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.update_badge_new),
                style = MaterialTheme.typography.labelSmall,
                color = ToolkitColors.Background,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AppUpdatePanel(
    updateCheckInterval: UpdateCheckInterval,
    onSetUpdateCheckInterval: (UpdateCheckInterval) -> Unit,
    onUpdateCheckResult: (UpdateCheckResult) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firstItem = remember { FocusRequester() }

    val currentVer = remember(context) { AppUpdateManager.currentVersionName(context) }
    val currentCode = remember(context) { AppUpdateManager.currentVersionCode(context) }

    var checking by remember { mutableStateOf(true) }
    var checkResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadPercent by remember { mutableIntStateOf(-1) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    fun runUpdateCheck() {
        if (downloading) return
        checking = true
        downloadError = null
        scope.launch {
            val res = AppUpdateManager.checkForUpdate(context)
            checkResult = res
            onUpdateCheckResult(res)
            checking = false
        }
    }

    fun triggerInstall(file: File) {
        when (AppUpdateManager.installApk(context, file)) {
            InstallOutcome.LAUNCHED -> Unit
            InstallOutcome.NEEDS_UNKNOWN_SOURCES_PERMISSION -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_update_allow_unknown_sources),
                    Toast.LENGTH_LONG,
                ).show()
            }
            InstallOutcome.FAILED -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_update_installer_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun startDownloadAndInstall(release: ReleaseInfo) {
        if (downloading) return
        downloading = true
        downloadPercent = 0
        downloadError = null
        scope.launch {
            val res = AppUpdateManager.downloadReleaseApk(context, release) { percent, _, _ ->
                downloadPercent = percent
            }
            downloading = false
            res.onSuccess { file ->
                downloadedApk = file
                triggerInstall(file)
            }.onFailure { err ->
                downloadError = err.localizedMessage ?: err.javaClass.simpleName
            }
        }
    }

    LaunchedEffect(Unit) {
        firstItem.requestFocusWhenReady()
        val res = AppUpdateManager.checkForUpdate(context)
        checkResult = res
        onUpdateCheckResult(res)
        checking = false
    }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.group_system),
            title = stringResource(R.string.update_title),
            subtitle = stringResource(
                R.string.update_panel_subtitle,
                "${AppUpdateManager.GITHUB_OWNER}/${AppUpdateManager.GITHUB_REPO}",
            ),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "status_box") {
                val statusText = when {
                    downloading -> {
                        if (downloadPercent >= 0) {
                            stringResource(R.string.update_downloading, downloadPercent)
                        } else {
                            stringResource(R.string.update_downloading_indeterminate)
                        }
                    }
                    checking -> stringResource(R.string.update_checking)
                    downloadError != null -> stringResource(R.string.update_download_failed, downloadError.orEmpty())
                    else -> when (val res = checkResult) {
                        is UpdateCheckResult.UpToDate -> stringResource(R.string.update_up_to_date, res.currentVersion)
                        is UpdateCheckResult.NoReleasePublished -> stringResource(R.string.update_no_release_published, res.currentVersion)
                        is UpdateCheckResult.UpdateAvailable -> stringResource(R.string.update_available_title, res.release.versionName)
                        is UpdateCheckResult.Error -> stringResource(R.string.update_error_prefix, res.message)
                        null -> stringResource(R.string.update_checking)
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .clip(ItemShape)
                        .background(ToolkitColors.OnSurface.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), ItemShape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(R.string.update_current_version_label, currentVer, currentCode),
                        style = MaterialTheme.typography.labelMedium,
                        color = ToolkitColors.OnSurfaceMuted,
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (checkResult is UpdateCheckResult.UpdateAvailable) ToolkitColors.Accent else ToolkitColors.OnSurface,
                    )
                    if (downloading && downloadPercent in 0..100) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ToolkitColors.OnSurface.copy(alpha = 0.12f)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(fraction = (downloadPercent / 100f).coerceIn(0.02f, 1f))
                                    .fillMaxHeight()
                                    .background(ToolkitColors.Accent, RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }

            val availableRelease = (checkResult as? UpdateCheckResult.UpdateAvailable)?.release
            if (availableRelease != null) {
                val readyFile = downloadedApk?.takeIf { it.exists() }
                item(key = "install_action") {
                    val sizeText = AppUpdateManager.formatSizeMb(availableRelease.apkSizeBytes)
                        .let { if (it.isNotBlank()) " · $it" else "" }
                    TvSettingsItem(
                        icon = Icons.Outlined.SystemUpdateAlt,
                        title = if (readyFile != null && !downloading) {
                            stringResource(R.string.update_ready_to_install, availableRelease.versionName)
                        } else {
                            stringResource(R.string.update_download_and_install, availableRelease.versionName)
                        },
                        subtitle = if (readyFile != null && !downloading) {
                            stringResource(R.string.update_ready_subtitle)
                        } else {
                            stringResource(R.string.update_download_subtitle, availableRelease.apkName, sizeText)
                        },
                        onClick = {
                            if (readyFile != null && !downloading) {
                                triggerInstall(readyFile)
                            } else {
                                startDownloadAndInstall(availableRelease)
                            }
                        },
                        modifier = Modifier.focusRequester(firstItem),
                        showChevron = true,
                    )
                }
            }

            item(key = "check_again") {
                TvSettingsItem(
                    icon = Icons.Outlined.Refresh,
                    title = stringResource(R.string.update_check_now),
                    subtitle = stringResource(R.string.update_check_now_subtitle),
                    onClick = ::runUpdateCheck,
                    modifier = if (availableRelease == null) Modifier.focusRequester(firstItem) else Modifier,
                )
            }

            item(key = "auto_check_interval") {
                val intervals = UpdateCheckInterval.entries
                TvSettingsItem(
                    icon = Icons.Outlined.AutoAwesome,
                    title = stringResource(R.string.update_auto_check_title),
                    subtitle = stringResource(updateCheckInterval.titleRes),
                    onClick = {
                        val idx = intervals.indexOf(updateCheckInterval).coerceAtLeast(0)
                        val next = intervals[(idx + 1) % intervals.size]
                        onSetUpdateCheckInterval(next)
                    },
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val idx = intervals.indexOf(updateCheckInterval).coerceAtLeast(0)
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                val prev = intervals[(idx - 1 + intervals.size) % intervals.size]
                                onSetUpdateCheckInterval(prev)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                val next = intervals[(idx + 1) % intervals.size]
                                onSetUpdateCheckInterval(next)
                                true
                            }
                            else -> false
                        }
                    },
                    trailingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(ToolkitColors.Accent, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = stringResource(updateCheckInterval.titleRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = ToolkitColors.Background,
                            )
                        }
                    },
                )
            }

            if (availableRelease != null && availableRelease.changelog.isNotBlank()) {
                item(key = "changelog") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .clip(ItemShape)
                            .background(ToolkitColors.OnSurface.copy(alpha = 0.04f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            stringResource(R.string.update_changelog_header),
                            style = MaterialTheme.typography.labelLarge,
                            color = ToolkitColors.Accent,
                        )
                        Text(
                            availableRelease.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = ToolkitColors.OnSurfaceMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeWallpaperPanel(
    focusMemory: PanelFocusMemory,
    currentPalette: AppThemePalette,
    dynamicAmbient: Boolean,
    hasCustomWallpaper: Boolean,
    wallpaperDim: Int,
    onSelectPalette: (AppThemePalette) -> Unit,
    onToggleDynamicAmbient: (Boolean) -> Unit,
    onSetWallpaperDim: (Int) -> Unit,
    onOpenLocalFile: () -> Unit,
    onOpenUrl: () -> Unit,
    onClearWallpaper: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val palettes = AppThemePalette.entries
    val focusController = rememberPanelFocus(
        pageKey = CustomizePage.ThemeWallpaper,
        memory = focusMemory,
        fallbackFirstKey = palettes.firstOrNull()?.id,
    )

    LaunchedEffect(Unit) { focusController.restoreFocus() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.group_appearance),
            title = stringResource(R.string.customize_theme_wallpaper),
            subtitle = stringResource(R.string.customize_theme_wallpaper_subtitle),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(palettes, key = { _, p -> p.id }) { _, palette ->
                val selected = palette == currentPalette
                ListItem(
                    selected = false,
                    onClick = { onSelectPalette(palette) },
                    modifier = focusController.modifierFor(palette.id),
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .background(palette.background, IconBoxShape)
                                .border(1.5.dp, palette.accent, IconBoxShape),
                        ) {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .background(palette.accent, CircleShape)
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            stringResource(palette.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(palette.subtitleRes),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = if (selected) {
                        {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(ToolkitColors.Accent, CircleShape),
                            ) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = stringResource(R.string.common_selected),
                                    tint = ToolkitColors.Background,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    } else null,
                    shape = ListItemDefaults.shape(ItemShape),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.78f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.14f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }
            item(key = "dynamic_ambient") {
                TvSettingsItem(
                    icon = Icons.Outlined.AutoAwesome,
                    title = stringResource(R.string.customize_dynamic_ambient),
                    subtitle = if (dynamicAmbient) stringResource(R.string.customize_dynamic_ambient_on)
                    else stringResource(R.string.customize_dynamic_ambient_off),
                    onClick = { onToggleDynamicAmbient(!dynamicAmbient) },
                    modifier = focusController.modifierFor("dynamic_ambient"),
                    trailingContent = {
                        TvSwitch(checked = dynamicAmbient)
                    },
                )
            }
            item(key = "wallpaper_file") {
                TvSettingsItem(
                    icon = Icons.Outlined.FolderOpen,
                    title = stringResource(R.string.wallpaper_from_file),
                    subtitle = stringResource(R.string.wallpaper_from_file_subtitle),
                    onClick = {
                        focusController.saveNow("wallpaper_file")
                        onOpenLocalFile()
                    },
                    modifier = focusController.modifierFor("wallpaper_file"),
                    showChevron = true,
                )
            }
            item(key = "wallpaper_url") {
                TvSettingsItem(
                    icon = Icons.Outlined.Link,
                    title = stringResource(R.string.wallpaper_from_url),
                    subtitle = stringResource(R.string.wallpaper_from_url_subtitle),
                    onClick = {
                        focusController.saveNow("wallpaper_url")
                        onOpenUrl()
                    },
                    modifier = focusController.modifierFor("wallpaper_url"),
                    showChevron = true,
                )
            }
            item(key = "wallpaper_dim") {
                WallpaperDimSliderItem(
                    dimPercent = wallpaperDim,
                    onChange = onSetWallpaperDim,
                    modifier = focusController.modifierFor("wallpaper_dim"),
                )
            }
            item(key = "wallpaper_dim_reset") {
                TvSettingsItem(
                    icon = Icons.Outlined.Restore,
                    title = stringResource(R.string.wallpaper_dim_reset),
                    subtitle = stringResource(
                        R.string.wallpaper_dim_reset_subtitle,
                        LauncherPrefs.DEFAULT_WALLPAPER_DIM,
                    ),
                    onClick = {
                        onSetWallpaperDim(LauncherPrefs.DEFAULT_WALLPAPER_DIM)
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_wallpaper_dim_reset, LauncherPrefs.DEFAULT_WALLPAPER_DIM),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = focusController.modifierFor("wallpaper_dim_reset"),
                )
            }
            if (hasCustomWallpaper) {
                item(key = "wallpaper_clear") {
                    TvSettingsItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = stringResource(R.string.wallpaper_clear),
                        subtitle = stringResource(R.string.wallpaper_clear_subtitle),
                        onClick = {
                            onClearWallpaper()
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_wallpaper_cleared),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = focusController.modifierFor("wallpaper_clear"),
                        danger = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperDimSliderItem(
    dimPercent: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clamped = dimPercent.coerceIn(0, 100)
    val fraction = clamped / 100f

    ListItem(
        selected = false,
        onClick = {
            val next = if (clamped >= 100) 0 else ((clamped + 10) / 10) * 10
            onChange(next.coerceIn(0, 100))
        },
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    onChange((clamped - 5).coerceIn(0, 100))
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    onChange((clamped + 5).coerceIn(0, 100))
                    true
                }
                else -> false
            }
        },
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .background(ToolkitColors.OnSurface.copy(alpha = 0.08f), IconBoxShape),
            ) {
                Icon(
                    Icons.Outlined.Brightness6,
                    contentDescription = null,
                    tint = ToolkitColors.Accent,
                    modifier = Modifier.size(19.dp),
                )
            }
        },
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.wallpaper_dim_title),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(ToolkitColors.Accent, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "$clamped%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ToolkitColors.Background,
                    )
                }
            }
        },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ToolkitColors.OnSurface.copy(alpha = 0.16f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceAtLeast(0.001f))
                                .fillMaxHeight()
                                .background(
                                    if (clamped > 0) ToolkitColors.Accent else Color.Transparent,
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (fraction > 0f) {
                            Spacer(Modifier.weight(fraction))
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(ToolkitColors.OnSurface, CircleShape)
                                .border(2.dp, ToolkitColors.Accent, CircleShape),
                        )
                        if (fraction < 1f) {
                            Spacer(Modifier.weight(1f - fraction))
                        }
                    }
                }
                Text(
                    stringResource(R.string.wallpaper_dim_hint),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        shape = ListItemDefaults.shape(ItemShape),
        scale = ListItemDefaults.scale(focusedScale = 1.02f),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = ToolkitColors.OnSurface.copy(alpha = 0.78f),
            focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.14f),
            focusedContentColor = ToolkitColors.OnSurface,
        ),
    )
}

@Composable
private fun WallpaperLocalFilePanel(
    focusMemory: PanelFocusMemory,
    onSelectUri: (Uri) -> Unit,
    onSelectFile: (File) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasMediaPerm by remember { mutableStateOf(WallpaperStore.hasMediaPermission(context)) }
    var hasAllFiles by remember { mutableStateOf(WallpaperStore.hasAllFilesAccess()) }
    var allImages by remember { mutableStateOf<List<LocalImageEntry>>(emptyList()) }
    var quickFolders by remember { mutableStateOf<List<Pair<String, File>>>(emptyList()) }
    var currentFolder by remember { mutableStateOf<File?>(null) }
    var folderState by remember { mutableStateOf<FolderBrowserState?>(null) }
    var scanTrigger by remember { mutableIntStateOf(0) }

    val activeFolder = currentFolder
    val browseKey = activeFolder?.let { "local_dir:${it.absolutePath}" } ?: "local_roots"
    val fallbackKey = remember(activeFolder, hasMediaPerm, hasAllFiles, quickFolders) {
        if (activeFolder != null) {
            "folder_back_roots"
        } else {
            when {
                !hasMediaPerm -> "grant_media_perm"
                !hasAllFiles -> "grant_all_files"
                quickFolders.isNotEmpty() -> "qfolder_${quickFolders.first().second.absolutePath}"
                else -> "rescan_storage"
            }
        }
    }
    val focusController = rememberPanelFocus(
        pageKey = browseKey,
        memory = focusMemory,
        fallbackFirstKey = fallbackKey,
    )

    val handleBack = {
        val browsing = currentFolder
        if (browsing != null) {
            val isQuickRoot = quickFolders.any { it.second.absolutePath == browsing.absolutePath }
            val parent = folderState?.parentDir
            currentFolder = if (!isQuickRoot && parent != null) parent else null
        } else {
            onBack()
        }
    }
    BackHandler(onBack = handleBack)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasMediaPerm = WallpaperStore.hasMediaPermission(context)
        hasAllFiles = WallpaperStore.hasAllFilesAccess()
        scanTrigger++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newMedia = WallpaperStore.hasMediaPermission(context)
                val newAll = WallpaperStore.hasAllFilesAccess()
                if (newMedia != hasMediaPerm || newAll != hasAllFiles) {
                    hasMediaPerm = newMedia
                    hasAllFiles = newAll
                    scanTrigger++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!WallpaperStore.hasMediaPermission(context)) {
            runCatching { permissionLauncher.launch(WallpaperStore.requiredPermissions()) }
        }
    }

    LaunchedEffect(scanTrigger, hasMediaPerm, hasAllFiles) {
        val (folders, images) = withContext(Dispatchers.IO) {
            WallpaperStore.getQuickFolders() to WallpaperStore.queryAllLocalImages(context)
        }
        quickFolders = folders
        allImages = images
    }

    LaunchedEffect(currentFolder, scanTrigger) {
        val target = currentFolder
        folderState = if (target != null) {
            withContext(Dispatchers.IO) { WallpaperStore.browseDirectory(target) }
        } else {
            null
        }
    }

    var lastRestoredKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(browseKey, folderState) {
        if (lastRestoredKey == browseKey) return@LaunchedEffect
        val target = currentFolder
        if (target == null || folderState?.currentDir?.absolutePath == target.absolutePath) {
            focusController.restoreFocus()
            lastRestoredKey = browseKey
        }
    }

    fun selectEntry(entry: LocalImageEntry) {
        val file = entry.file?.takeIf { it.exists() && it.canRead() }
        if (file != null) {
            onSelectFile(file)
        } else if (entry.contentUri != null) {
            onSelectUri(entry.contentUri)
        } else if (entry.file != null) {
            onSelectFile(entry.file)
        }
    }

    fun openSubfolder(itemKey: String, dir: File) {
        focusController.saveNow(itemKey)
        focusMemory.clear("local_dir:${dir.absolutePath}")
        currentFolder = dir
    }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_theme_wallpaper),
            title = activeFolder?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.wallpaper_from_file),
            subtitle = activeFolder?.absolutePath ?: stringResource(R.string.wallpaper_from_file_subtitle),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (activeFolder != null) {
                val state = folderState
                item(key = "folder_back_roots") {
                    TvSettingsItem(
                        icon = Icons.Outlined.Home,
                        title = stringResource(R.string.wallpaper_folder_back_roots),
                        subtitle = stringResource(R.string.wallpaper_folder_back_roots_subtitle),
                        onClick = { currentFolder = null },
                        modifier = focusController.modifierFor("folder_back_roots"),
                    )
                }
                val parent = state?.parentDir
                if (parent != null) {
                    val parentItemKey = "folder_up_${parent.absolutePath}"
                    item(key = parentItemKey) {
                        TvSettingsItem(
                            icon = Icons.Outlined.FolderOpen,
                            title = stringResource(R.string.wallpaper_folder_up),
                            subtitle = stringResource(
                                R.string.wallpaper_folder_up_subtitle,
                                parent.name.ifBlank { parent.absolutePath },
                            ),
                            onClick = { currentFolder = parent },
                            modifier = focusController.modifierFor(parentItemKey),
                        )
                    }
                }
                if (state != null) {
                    if (state.directories.isEmpty() && state.images.isEmpty()) {
                        item(key = "folder_empty") {
                            Text(
                                stringResource(R.string.wallpaper_folder_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = ToolkitColors.OnSurfaceMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            )
                        }
                    } else {
                        itemsIndexed(state.directories, key = { _, d -> "dir_${d.absolutePath}" }) { _, subDir ->
                            val dirKey = "dir_${subDir.absolutePath}"
                            TvSettingsItem(
                                icon = Icons.Outlined.FolderOpen,
                                title = subDir.name,
                                subtitle = stringResource(R.string.wallpaper_folder_item_subtitle, subDir.absolutePath),
                                onClick = { openSubfolder(dirKey, subDir) },
                                modifier = focusController.modifierFor(dirKey),
                                showChevron = true,
                            )
                        }
                        itemsIndexed(state.images, key = { _, img -> img.id }) { _, entry ->
                            LocalImagePickerItem(
                                entry = entry,
                                onClick = { selectEntry(entry) },
                                modifier = focusController.modifierFor(entry.id),
                            )
                        }
                    }
                }
            } else {
                if (!hasMediaPerm) {
                    item(key = "grant_media_perm") {
                        TvSettingsItem(
                            icon = Icons.Outlined.FolderOpen,
                            title = stringResource(R.string.wallpaper_permission_title),
                            subtitle = stringResource(R.string.wallpaper_permission_subtitle),
                            onClick = {
                                runCatching {
                                    permissionLauncher.launch(WallpaperStore.requiredPermissions())
                                }.onFailure {
                                    WallpaperStore.openAllFilesAccessSettings(context)
                                }
                            },
                            modifier = focusController.modifierFor("grant_media_perm"),
                            showChevron = true,
                        )
                    }
                }

                if (!hasAllFiles) {
                    item(key = "grant_all_files") {
                        TvSettingsItem(
                            icon = Icons.Outlined.Settings,
                            title = stringResource(R.string.wallpaper_all_files_title),
                            subtitle = stringResource(R.string.wallpaper_all_files_subtitle),
                            onClick = { WallpaperStore.openAllFilesAccessSettings(context) },
                            modifier = focusController.modifierFor("grant_all_files"),
                            showChevron = true,
                        )
                    }
                }

                if (quickFolders.isNotEmpty()) {
                    item(key = "section_folders") {
                        Text(
                            stringResource(R.string.wallpaper_quick_folders_section).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                            color = ToolkitColors.Accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    itemsIndexed(quickFolders, key = { _, pair -> "qfolder_${pair.second.absolutePath}" }) { _, (label, dir) ->
                        val qKey = "qfolder_${dir.absolutePath}"
                        TvSettingsItem(
                            icon = Icons.Outlined.FolderOpen,
                            title = label,
                            subtitle = dir.absolutePath,
                            onClick = { openSubfolder(qKey, dir) },
                            modifier = focusController.modifierFor(qKey),
                            showChevron = true,
                        )
                    }
                }

                item(key = "section_all_images") {
                    Text(
                        stringResource(R.string.wallpaper_all_images_section, allImages.size).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = ToolkitColors.Accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                item(key = "rescan_storage") {
                    TvSettingsItem(
                        icon = Icons.Outlined.Refresh,
                        title = stringResource(R.string.wallpaper_rescan),
                        subtitle = stringResource(R.string.wallpaper_rescan_subtitle),
                        onClick = { scanTrigger++ },
                        modifier = focusController.modifierFor("rescan_storage"),
                    )
                }

                if (allImages.isEmpty()) {
                    item(key = "empty_notice") {
                        Text(
                            stringResource(R.string.wallpaper_local_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = ToolkitColors.OnSurfaceMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    itemsIndexed(allImages, key = { _, entry -> entry.id }) { _, entry ->
                        LocalImagePickerItem(
                            entry = entry,
                            onClick = { selectEntry(entry) },
                            modifier = focusController.modifierFor(entry.id),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalImagePickerItem(
    entry: LocalImageEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbnail by remember(entry.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(entry.id) {
        thumbnail = withContext(Dispatchers.IO) {
            WallpaperStore.loadThumbnail(context, entry)
        }
    }

    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        leadingContent = {
            val bmp = thumbnail
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = entry.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(IconBoxShape)
                        .border(1.dp, Color.White.copy(alpha = 0.14f), IconBoxShape),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(ToolkitColors.OnSurface.copy(alpha = 0.08f), IconBoxShape),
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = ToolkitColors.Accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        headlineContent = {
            Text(
                entry.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                entry.formattedSubtitle(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shape = ListItemDefaults.shape(ItemShape),
        scale = ListItemDefaults.scale(focusedScale = 1.02f),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = ToolkitColors.OnSurface.copy(alpha = 0.78f),
            focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.14f),
            focusedContentColor = ToolkitColors.OnSurface,
        ),
    )
}

@Composable
private fun WallpaperUrlPanel(
    onApplyUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var urlText by remember { mutableStateOf("https://") }
    val applyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { applyFocus.requestFocusWhenReady() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_theme_wallpaper),
            title = stringResource(R.string.wallpaper_from_url),
            subtitle = stringResource(R.string.wallpaper_from_url_subtitle),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.wallpaper_url_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = ToolkitColors.OnSurfaceMuted,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(ToolkitColors.OnSurface.copy(alpha = 0.08f), ItemShape)
                    .border(1.dp, ToolkitColors.Accent.copy(alpha = 0.5f), ItemShape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = ToolkitColors.OnSurface),
                    cursorBrush = SolidColor(ToolkitColors.Accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (urlText.isNotBlank()) onApplyUrl(urlText) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TvSettingsItem(
                icon = Icons.Outlined.FileDownload,
                title = stringResource(R.string.wallpaper_url_apply),
                subtitle = stringResource(R.string.wallpaper_url_apply_subtitle),
                onClick = { if (urlText.isNotBlank()) onApplyUrl(urlText) },
                modifier = Modifier.focusRequester(applyFocus),
            )
            TvSettingsItem(
                icon = Icons.Outlined.Block,
                title = stringResource(R.string.common_cancel),
                subtitle = stringResource(R.string.remote_capture_cancel_subtitle),
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun AutoLaunchPanel(
    currentTarget: String,
    onBoot: Boolean,
    onWake: Boolean,
    tvInputs: List<TvInputEntry>,
    apps: List<LauncherApp>,
    onToggleBoot: (Boolean) -> Unit,
    onToggleWake: (Boolean) -> Unit,
    onSelectTarget: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val firstItem = remember { FocusRequester() }
    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }

    LaunchedEffect(Unit) { firstItem.requestFocusWhenReady() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.group_controls),
            title = stringResource(R.string.customize_auto_launch),
            subtitle = stringResource(R.string.customize_auto_launch_panel_subtitle),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "toggle_boot") {
                TvSettingsItem(
                    icon = Icons.Outlined.PowerSettingsNew,
                    title = stringResource(R.string.auto_launch_on_boot),
                    subtitle = if (onBoot) stringResource(R.string.auto_launch_on_boot_on)
                    else stringResource(R.string.auto_launch_on_boot_off),
                    onClick = { onToggleBoot(!onBoot) },
                    modifier = Modifier.focusRequester(firstItem),
                    trailingContent = { TvSwitch(checked = onBoot) },
                )
            }
            item(key = "toggle_wake") {
                TvSettingsItem(
                    icon = Icons.Outlined.Bedtime,
                    title = stringResource(R.string.auto_launch_on_wake),
                    subtitle = if (onWake) stringResource(R.string.auto_launch_on_wake_on)
                    else stringResource(R.string.auto_launch_on_wake_off),
                    onClick = { onToggleWake(!onWake) },
                    trailingContent = { TvSwitch(checked = onWake) },
                )
            }
            item(key = "target_off") {
                val selected = currentTarget.isBlank()
                ActionChoiceItem(
                    icon = Icons.Outlined.Block,
                    title = stringResource(R.string.auto_launch_off),
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.auto_launch_off_subtitle),
                    selected = selected,
                    onClick = { onSelectTarget(AutoLaunchManager.TARGET_OFF) },
                )
            }
            item(key = "target_live_tv") {
                val selected = currentTarget == AutoLaunchManager.TARGET_LIVE_TV
                ActionChoiceItem(
                    icon = Icons.Outlined.LiveTv,
                    title = stringResource(R.string.remote_action_live_tv),
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.remote_action_live_tv_subtitle),
                    selected = selected,
                    onClick = { onSelectTarget(AutoLaunchManager.TARGET_LIVE_TV) },
                )
            }
            itemsIndexed(tvInputs, key = { _, input -> "input_${input.id}" }) { _, input ->
                val targetKey = "${AutoLaunchManager.INPUT_PREFIX}${input.id}"
                val selected = currentTarget == targetKey
                val title = input.displayTitle(context)
                ActionChoiceItem(
                    icon = Icons.Outlined.Input,
                    appIcon = input.icon,
                    title = title,
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.auto_launch_input_subtitle, input.stateLabel(context)),
                    selected = selected,
                    onClick = { onSelectTarget(targetKey) },
                )
            }
            itemsIndexed(sortedApps, key = { _, app -> "app_${app.packageName}" }) { _, app ->
                val selected = currentTarget == app.packageName
                ActionChoiceItem(
                    appIcon = app.icon,
                    title = app.label,
                    subtitle = if (selected) stringResource(R.string.common_selected) else app.packageName,
                    selected = selected,
                    onClick = { onSelectTarget(app.packageName) },
                )
            }
        }
    }
}

@Composable
private fun BackupRestorePanel(
    onRestoreCompleted: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val firstItem = remember { FocusRequester() }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val ok = BackupManager.exportToUri(context, uri)
            Toast.makeText(
                context,
                if (ok) context.getString(R.string.toast_backup_saved, BackupManager.DEFAULT_FILE_NAME)
                else context.getString(R.string.toast_backup_failed),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val ok = BackupManager.importFromUri(context, uri)
            Toast.makeText(
                context,
                context.getString(if (ok) R.string.toast_restore_success else R.string.toast_restore_failed),
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) onRestoreCompleted()
        }
    }

    LaunchedEffect(Unit) { firstItem.requestFocusWhenReady() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.group_system),
            title = stringResource(R.string.customize_backup_restore),
            subtitle = stringResource(R.string.customize_backup_restore_subtitle),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "export_quick") {
                TvSettingsItem(
                    icon = Icons.Outlined.FileUpload,
                    title = stringResource(R.string.backup_export_quick),
                    subtitle = stringResource(R.string.backup_export_quick_subtitle),
                    onClick = {
                        val savedPath = BackupManager.exportDefaultBackup(context)
                        Toast.makeText(
                            context,
                            if (savedPath != null) context.getString(R.string.toast_backup_saved, savedPath)
                            else context.getString(R.string.toast_backup_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.focusRequester(firstItem),
                )
            }
            item(key = "import_quick") {
                TvSettingsItem(
                    icon = Icons.Outlined.FileDownload,
                    title = stringResource(R.string.backup_import_quick),
                    subtitle = stringResource(R.string.backup_import_quick_subtitle),
                    onClick = {
                        val ok = BackupManager.importDefaultBackup(context)
                        Toast.makeText(
                            context,
                            context.getString(if (ok) R.string.toast_restore_success else R.string.toast_restore_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                        if (ok) onRestoreCompleted()
                    },
                )
            }
            item(key = "export_file") {
                TvSettingsItem(
                    icon = Icons.Outlined.FolderOpen,
                    title = stringResource(R.string.backup_export_file),
                    subtitle = stringResource(R.string.backup_export_file_subtitle),
                    onClick = {
                        runCatching { createBackupLauncher.launch(BackupManager.DEFAULT_FILE_NAME) }.onFailure {
                            Toast.makeText(context, context.getString(R.string.toast_could_not_open), Toast.LENGTH_SHORT).show()
                        }
                    },
                    showChevron = true,
                )
            }
            item(key = "import_file") {
                TvSettingsItem(
                    icon = Icons.Outlined.Restore,
                    title = stringResource(R.string.backup_import_file),
                    subtitle = stringResource(R.string.backup_import_file_subtitle),
                    onClick = {
                        runCatching { openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }.onFailure {
                            Toast.makeText(context, context.getString(R.string.toast_could_not_open), Toast.LENGTH_SHORT).show()
                        }
                    },
                    showChevron = true,
                )
            }
        }
    }
}

@Composable
private fun LanguagePanel(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val firstItem = remember { FocusRequester() }
    val languages = AppLanguage.entries

    LaunchedEffect(Unit) { firstItem.requestFocusWhenReady() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_title),
            title = stringResource(R.string.customize_language),
            subtitle = stringResource(R.string.customize_language_panel_subtitle),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(languages, key = { _, lang -> lang.code }) { index, lang ->
                val selected = lang == currentLanguage
                ActionChoiceItem(
                    icon = Icons.Outlined.Language,
                    title = lang.nativeName,
                    subtitle = if (selected) stringResource(R.string.common_selected) else lang.code.uppercase(),
                    selected = selected,
                    onClick = { onSelectLanguage(lang) },
                    modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun RemoteButtonsPanel(
    focusMemory: PanelFocusMemory,
    configs: List<RemoteButtonConfig>,
    customMappings: List<CustomKeyMapping>,
    apps: List<LauncherApp>,
    overrideHomeKey: Boolean,
    onToggleOverrideHomeKey: (Boolean) -> Unit,
    onSelectButton: (RemoteButton) -> Unit,
    onSelectCustomMapping: (CustomKeyMapping) -> Unit,
    onAddCustomKey: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val focusController = rememberPanelFocus(
        pageKey = CustomizePage.RemoteButtonsList,
        memory = focusMemory,
        fallbackFirstKey = "accessibility_status",
    )
    val appsByPkg = remember(apps) { apps.associateBy { it.packageName } }
    val accessibilityEnabled = ToolkitAccessibilityService.instance != null

    LaunchedEffect(Unit) { focusController.restoreFocus() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.group_controls),
            title = stringResource(R.string.customize_remote_buttons),
            subtitle = stringResource(R.string.customize_remote_buttons_panel_subtitle),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "accessibility_status") {
                TvSettingsItem(
                    icon = Icons.Outlined.AccessibilityNew,
                    title = if (accessibilityEnabled) stringResource(R.string.remote_accessibility_on)
                    else stringResource(R.string.remote_accessibility_off),
                    subtitle = if (accessibilityEnabled) stringResource(R.string.remote_accessibility_on_subtitle)
                    else stringResource(R.string.remote_accessibility_off_subtitle),
                    onClick = { RemoteButtons.openAccessibilitySettings(context) },
                    modifier = focusController.modifierFor("accessibility_status"),
                    trailingContent = {
                        val badgeShape = RoundedCornerShape(6.dp)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (accessibilityEnabled) ToolkitColors.Accent.copy(alpha = 0.2f)
                                    else ToolkitColors.Danger.copy(alpha = 0.2f),
                                    badgeShape,
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                if (accessibilityEnabled) stringResource(R.string.remote_accessibility_badge_on)
                                else stringResource(R.string.remote_accessibility_badge_off),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (accessibilityEnabled) ToolkitColors.Accent else ToolkitColors.Danger,
                            )
                        }
                    },
                )
            }

            item(key = "override_home") {
                TvSettingsItem(
                    icon = Icons.Outlined.Home,
                    title = stringResource(R.string.remote_override_home),
                    subtitle = if (overrideHomeKey) stringResource(R.string.remote_override_home_on)
                    else stringResource(R.string.remote_override_home_off),
                    onClick = { onToggleOverrideHomeKey(!overrideHomeKey) },
                    modifier = focusController.modifierFor("override_home"),
                    trailingContent = {
                        TvSwitch(checked = overrideHomeKey)
                    },
                )
            }

            itemsIndexed(configs, key = { _, cfg -> cfg.button.id }) { _, cfg ->
                val assignedApp = appsByPkg[cfg.target]
                val targetLabel = when (cfg.target) {
                    RemoteButtons.TARGET_DEFAULT -> stringResource(cfg.button.defaultActionLabelRes)
                    RemoteButtons.TARGET_DISABLED -> stringResource(R.string.remote_target_disabled)
                    else -> stringResource(R.string.remote_target_open_app, assignedApp?.label ?: cfg.target)
                }
                val customKeyText = if (cfg.customKeyCode > 0 || cfg.customScanCode > 0) {
                    stringResource(
                        R.string.remote_custom_key_code_suffix,
                        RemoteButtons.formatKeyLabel(cfg.customKeyCode, cfg.customScanCode),
                    )
                } else {
                    ""
                }

                TvSettingsItem(
                    icon = remoteButtonIcon(cfg.button),
                    appIcon = assignedApp?.icon,
                    title = stringResource(cfg.button.titleRes),
                    subtitle = targetLabel + customKeyText,
                    onClick = {
                        focusController.saveNow(cfg.button.id)
                        onSelectButton(cfg.button)
                    },
                    modifier = focusController.modifierFor(cfg.button.id),
                    showChevron = true,
                )
            }

            itemsIndexed(customMappings, key = { _, mapping -> "custom_${mapping.id}" }) { _, mapping ->
                val assignedApp = appsByPkg[mapping.target]
                val keyName = RemoteButtons.formatKeyLabel(mapping.keyCode, mapping.scanCode)
                val mappingKey = "custom_${mapping.id}"
                TvSettingsItem(
                    icon = Icons.Outlined.Keyboard,
                    appIcon = assignedApp?.icon,
                    title = stringResource(R.string.remote_custom_key_item_title, keyName),
                    subtitle = RemoteButtons.formatCustomTargetLabel(context, mapping.target, appsByPkg),
                    onClick = {
                        focusController.saveNow(mappingKey)
                        onSelectCustomMapping(mapping)
                    },
                    modifier = focusController.modifierFor(mappingKey),
                    showChevron = true,
                )
            }

            item(key = "add_custom_key") {
                TvSettingsItem(
                    icon = Icons.Outlined.AddCircleOutline,
                    title = stringResource(R.string.remote_add_custom_key),
                    subtitle = stringResource(R.string.remote_add_custom_key_subtitle),
                    onClick = {
                        focusController.saveNow("add_custom_key")
                        onAddCustomKey()
                    },
                    modifier = focusController.modifierFor("add_custom_key"),
                    showChevron = true,
                )
            }
        }
    }
}

@Composable
private fun CustomKeyDetailPanel(
    keyCode: Int,
    scanCode: Int,
    currentTarget: String?,
    canRemove: Boolean,
    apps: List<LauncherApp>,
    onSelectTarget: (String) -> Unit,
    onRemove: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val firstItem = remember { FocusRequester() }
    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    val keyLabel = remember(keyCode, scanCode) { RemoteButtons.formatKeyLabel(keyCode, scanCode) }

    LaunchedEffect(Unit) { firstItem.requestFocusWhenReady() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_remote_buttons),
            title = stringResource(R.string.remote_custom_detail_title, keyLabel),
            subtitle = stringResource(R.string.remote_custom_detail_subtitle),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (canRemove) {
                item(key = "remove_custom_key") {
                    TvSettingsItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = stringResource(R.string.remote_custom_remove),
                        subtitle = stringResource(R.string.remote_custom_remove_subtitle, keyLabel),
                        onClick = {
                            onRemove()
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_removed_custom_key, keyLabel),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = Modifier.focusRequester(firstItem),
                        danger = true,
                    )
                }
            }
            item(key = RemoteButtons.ACTION_LIVE_TV) {
                val selected = currentTarget == RemoteButtons.ACTION_LIVE_TV
                ActionChoiceItem(
                    icon = Icons.Outlined.LiveTv,
                    title = stringResource(R.string.remote_action_live_tv),
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.remote_action_live_tv_subtitle),
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.ACTION_LIVE_TV) },
                    modifier = if (!canRemove) Modifier.focusRequester(firstItem) else Modifier,
                )
            }
            item(key = RemoteButtons.ACTION_SYSTEM_SETTINGS) {
                val selected = currentTarget == RemoteButtons.ACTION_SYSTEM_SETTINGS
                ActionChoiceItem(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.remote_action_system_settings),
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.remote_action_system_settings_subtitle),
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.ACTION_SYSTEM_SETTINGS) },
                )
            }
            item(key = RemoteButtons.ACTION_SYSTEM_ACCOUNTS) {
                val selected = currentTarget == RemoteButtons.ACTION_SYSTEM_ACCOUNTS
                ActionChoiceItem(
                    icon = Icons.Outlined.AccountCircle,
                    title = stringResource(R.string.remote_action_system_accounts),
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.remote_action_system_accounts_subtitle),
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.ACTION_SYSTEM_ACCOUNTS) },
                )
            }
            itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { _, app ->
                val selected = currentTarget == app.packageName
                ActionChoiceItem(
                    appIcon = app.icon,
                    title = app.label,
                    subtitle = if (selected) stringResource(R.string.remote_assigned_to_key, keyLabel) else app.packageName,
                    selected = selected,
                    onClick = { onSelectTarget(app.packageName) },
                )
            }
        }
    }
}

@Composable
private fun RemoteButtonDetailPanel(
    pageKey: CustomizePage.RemoteButtonDetail,
    focusMemory: PanelFocusMemory,
    config: RemoteButtonConfig,
    apps: List<LauncherApp>,
    onSelectTarget: (String) -> Unit,
    onStartCaptureKey: () -> Unit,
    onClearCustomKey: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val focusController = rememberPanelFocus(
        pageKey = pageKey,
        memory = focusMemory,
        fallbackFirstKey = "action_default",
    )
    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    val hasCustomKey = config.customKeyCode > 0 || config.customScanCode > 0
    val buttonTitle = stringResource(config.button.titleRes)

    LaunchedEffect(Unit) { focusController.restoreFocus() }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.customize_remote_buttons),
            title = buttonTitle,
            subtitle = stringResource(R.string.remote_detail_subtitle),
        )
        LazyColumn(
            state = focusController.listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "action_default") {
                val selected = config.target == RemoteButtons.TARGET_DEFAULT
                ActionChoiceItem(
                    icon = remoteButtonIcon(config.button),
                    title = stringResource(config.button.defaultActionLabelRes),
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.remote_detail_default_subtitle),
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.TARGET_DEFAULT) },
                    modifier = focusController.modifierFor("action_default"),
                )
            }
            item(key = "action_learn_key") {
                TvSettingsItem(
                    icon = Icons.Outlined.Sensors,
                    title = stringResource(R.string.remote_learn_key),
                    subtitle = if (hasCustomKey) {
                        stringResource(
                            R.string.remote_learn_key_active,
                            RemoteButtons.formatKeyLabel(config.customKeyCode, config.customScanCode),
                        )
                    } else {
                        stringResource(R.string.remote_learn_key_subtitle)
                    },
                    onClick = {
                        focusController.saveNow("action_learn_key")
                        onStartCaptureKey()
                    },
                    modifier = focusController.modifierFor("action_learn_key"),
                    showChevron = true,
                )
            }
            if (hasCustomKey) {
                item(key = "action_clear_key") {
                    TvSettingsItem(
                        icon = Icons.Outlined.Restore,
                        title = stringResource(R.string.remote_clear_key),
                        subtitle = stringResource(R.string.remote_clear_key_subtitle),
                        onClick = {
                            onClearCustomKey()
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_restored_default_key),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = focusController.modifierFor("action_clear_key"),
                    )
                }
            }
            item(key = "action_disabled") {
                val selected = config.target == RemoteButtons.TARGET_DISABLED
                ActionChoiceItem(
                    icon = Icons.Outlined.Block,
                    title = stringResource(R.string.remote_disable_button),
                    subtitle = if (selected) stringResource(R.string.common_selected)
                    else stringResource(R.string.remote_disable_button_subtitle),
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.TARGET_DISABLED) },
                    modifier = focusController.modifierFor("action_disabled"),
                )
            }
            itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { _, app ->
                val selected = config.target == app.packageName
                ActionChoiceItem(
                    appIcon = app.icon,
                    title = app.label,
                    subtitle = if (selected) stringResource(R.string.remote_assigned_to_button, buttonTitle)
                    else app.packageName,
                    selected = selected,
                    onClick = { onSelectTarget(app.packageName) },
                    modifier = focusController.modifierFor(app.packageName),
                )
            }
        }
    }
}

@Composable
private fun CaptureKeyPanel(
    breadcrumb: String,
    title: String,
    promptText: String,
    onKeyCaptured: (Int, Int) -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    val cancelFocus = remember { FocusRequester() }

    DisposableEffect(title) {
        val listener: (Int, Int) -> Unit = { keyCode, scanCode ->
            onKeyCaptured(keyCode, scanCode)
        }
        RemoteButtons.keyCaptureListener = listener
        onDispose {
            if (RemoteButtons.keyCaptureListener === listener) {
                RemoteButtons.keyCaptureListener = null
            }
        }
    }

    LaunchedEffect(Unit) { cancelFocus.requestFocusWhenReady() }

    Column(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (RemoteButtons.isCapturableKey(native)) {
                    if (event.type == KeyEventType.KeyDown && native.repeatCount == 0) {
                        RemoteButtons.keyCaptureListener?.invoke(native.keyCode, native.scanCode)
                    }
                    true
                } else {
                    false
                }
            },
    ) {
        SheetHeader(
            breadcrumb = breadcrumb,
            title = title,
            subtitle = stringResource(R.string.remote_capture_subtitle),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(ToolkitColors.Accent.copy(alpha = 0.14f), CircleShape)
                        .border(1.dp, ToolkitColors.Accent.copy(alpha = 0.4f), CircleShape),
                ) {
                    Icon(
                        Icons.Outlined.SettingsRemote,
                        contentDescription = null,
                        tint = ToolkitColors.Accent,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    promptText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.OnSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
            ActionItem(
                title = stringResource(R.string.common_cancel),
                subtitle = stringResource(R.string.remote_capture_cancel_subtitle),
                onClick = onCancel,
                modifier = Modifier.focusRequester(cancelFocus),
            )
        }
    }
}

@Composable
private fun WatchSourcesPanel(
    apps: List<LauncherApp>,
    disabledWatchPackages: Set<String>,
    activeWatchPackages: Set<String>,
    onToggleWatchPackage: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val firstItem = remember { FocusRequester() }
    val sortedApps = remember(apps, activeWatchPackages) {
        val (active, rest) = apps.partition { it.packageName in activeWatchPackages }
        active.sortedBy { it.label.lowercase() } + rest.sortedBy { it.label.lowercase() }
    }

    LaunchedEffect(sortedApps.isNotEmpty()) {
        if (sortedApps.isNotEmpty()) firstItem.requestFocusWhenReady()
    }

    Column(Modifier.fillMaxSize()) {
        SheetHeader(
            breadcrumb = stringResource(R.string.group_appearance),
            title = stringResource(R.string.customize_watch_sources),
            subtitle = stringResource(R.string.customize_watch_sources_panel_subtitle),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { index, app ->
                val enabled = app.packageName !in disabledWatchPackages
                val hasActiveContent = app.packageName in activeWatchPackages
                TvSettingsItem(
                    appIcon = app.icon,
                    title = app.label,
                    subtitle = when {
                        !enabled -> stringResource(R.string.customize_watch_source_disabled)
                        hasActiveContent -> stringResource(R.string.customize_watch_source_active)
                        else -> stringResource(R.string.customize_watch_source_enabled)
                    },
                    onClick = { onToggleWatchPackage(app.packageName, !enabled) },
                    modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                    trailingContent = {
                        TvSwitch(checked = enabled)
                    },
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    breadcrumb: String? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ToolkitColors.Accent.copy(alpha = 0.16f),
                        ToolkitColors.Accent.copy(alpha = 0.02f),
                    )
                )
            )
            .drawBehind {
                drawLine(
                    Color.White.copy(alpha = 0.08f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        if (!breadcrumb.isNullOrBlank()) {
            Text(
                breadcrumb.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = ToolkitColors.Accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = ToolkitColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = ToolkitColors.OnSurfaceMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TvSettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    appIcon: androidx.compose.ui.graphics.ImageBitmap? = null,
    showChevron: Boolean = false,
    danger: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val tint = if (danger) ToolkitColors.Danger else ToolkitColors.OnSurface
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        leadingContent = if (appIcon != null || icon != null) {
            {
                if (appIcon != null) {
                    Image(
                        appIcon,
                        contentDescription = title,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(IconBoxShape),
                    )
                } else if (icon != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(tint.copy(alpha = 0.08f), IconBoxShape),
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (danger) ToolkitColors.Danger else ToolkitColors.Accent,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        } else null,
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = when {
            trailingContent != null -> trailingContent
            showChevron -> {
                {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = ToolkitColors.OnSurfaceMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            else -> null
        },
        shape = ListItemDefaults.shape(ItemShape),
        scale = ListItemDefaults.scale(focusedScale = 1.02f),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = tint.copy(alpha = 0.78f),
            focusedContainerColor = tint.copy(alpha = 0.14f),
            focusedContentColor = tint,
        ),
    )
}

@Composable
private fun ActionChoiceItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    appIcon: androidx.compose.ui.graphics.ImageBitmap? = null,
) {
    TvSettingsItem(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        appIcon = appIcon,
        trailingContent = if (selected) {
            {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .background(ToolkitColors.Accent, CircleShape),
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.common_selected),
                        tint = ToolkitColors.Background,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        } else null,
    )
}

@Composable
private fun TvSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = ToolkitColors.Background,
            checkedTrackColor = ToolkitColors.Accent,
        ),
    )
}

private fun remoteButtonIcon(button: RemoteButton): ImageVector = when (button) {
    RemoteButton.GUIDE -> Icons.Outlined.LiveTv
    RemoteButton.YOUTUBE -> Icons.Outlined.SmartDisplay
    RemoteButton.SETTINGS -> Icons.Outlined.Settings
    RemoteButton.PROFILE -> Icons.Outlined.AccountCircle
}
