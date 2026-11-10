package com.tk.toolkit.tv.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.launcher.LauncherViewModel
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.launcher.data.TvInputs
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.launcher.data.rememberTvInputs
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private const val MORE_APPS_KEY = "toolkit:more-apps"
private const val CUSTOMIZE_KEY = "toolkit:customize"
private const val WATCH_TITLE_KEY = "toolkit:continue-watching-title"
private const val WATCH_ROW_KEY = "toolkit:continue-watching-row"
private val GRID_PADDING_X = 56.dp
private val TILE_SPACING = 20.dp
private const val APPS_TITLE_KEY = "toolkit:apps-title"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val baseContext = LocalContext.current
    val context = remember(baseContext, state.language) {
        LocaleHelper.wrapContext(baseContext, state.language.code)
    }
    val localizedConfiguration = remember(context) {
        context.resources.configuration
    }

    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides localizedConfiguration,
    ) {
        HomeScreenContent(viewModel = viewModel, state = state)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreenContent(
    viewModel: LauncherViewModel,
    state: com.tk.toolkit.tv.launcher.LauncherState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val watchRowState = rememberLazyListState()
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(key: String) = focusRequesters.getOrPut(key) { FocusRequester() }
    var focusJob by remember { mutableStateOf<Job?>(null) }

    var menuApp by remember { mutableStateOf<LauncherApp?>(null) }
    var menuWatchItem by remember { mutableStateOf<WatchItem?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var showInputs by remember { mutableStateOf(false) }
    val tvInputs = rememberTvInputs(context)
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFocusedIndex by rememberSaveable { mutableStateOf(0) }
    // Not saved: every fresh start begins on the first app.
    var initialFocusPlaced by remember { mutableStateOf(false) }
    var headerFocused by remember { mutableStateOf(false) }
    var watchRowFocused by remember { mutableStateOf(false) }
    var firstAppRowFocused by remember { mutableStateOf(false) }
    // Collapse the reserved 2-line caption space under Continue Watching when the row isn't focused,
    // so the Apps section pulls up close to the Continue Watching thumbnails.
    val watchRowCollapse by animateDpAsState(
        targetValue = if (watchRowFocused) 0.dp else 46.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "watchRowCollapse",
    )

    val scrollSpec = with(LocalDensity.current) {
        remember { minimalScrollSpec(topMarginPx = 28.dp.toPx(), bottomMarginPx = 64.dp.toPx()) }
    }
    val columns = state.appColumns.coerceIn(4, 6)
    val watchItems = if (state.showWatchRow) state.watchItems else emptyList()
    val watchKeys = watchItems.map { it.key }
    val hasMoreApps = state.hiddenApps.isNotEmpty()
    val gridKeys = watchKeys + state.apps.map { it.packageName } +
        (if (hasMoreApps) listOf(MORE_APPS_KEY) else emptyList()) +
        listOf(CUSTOMIZE_KEY)

    suspend fun focusFirstApp() {
        val target = state.apps.firstOrNull()?.packageName ?: gridKeys.firstOrNull() ?: return
        lastFocusedKey = target
        lastFocusedIndex = gridKeys.indexOf(target).coerceAtLeast(0)
        if (gridState.layoutInfo.totalItemsCount > 0) {
            runCatching { gridState.scrollToItem(0) }
        }
        withFrameNanos { }
        requesterFor(target).requestFocusWhenReady(maxFrames = 30)
        if (gridState.layoutInfo.totalItemsCount > 0) {
            runCatching { gridState.scrollToItem(0) }
        }
    }

    suspend fun focusFirstWatchItem() {
        val target = watchItems.firstOrNull()?.key ?: return
        lastFocusedKey = target
        lastFocusedIndex = 0
        if (gridState.layoutInfo.totalItemsCount > 0) {
            runCatching { gridState.scrollToItem(0) }
        }
        withFrameNanos { }
        if (watchRowState.layoutInfo.totalItemsCount > 0) {
            runCatching { watchRowState.scrollToItem(0) }
        }
        withFrameNanos { }
        requesterFor(target).requestFocusWhenReady(maxFrames = 30)
        if (gridState.layoutInfo.totalItemsCount > 0) {
            runCatching { gridState.scrollToItem(0) }
        }
        if (watchRowState.layoutInfo.totalItemsCount > 0) {
            runCatching { watchRowState.scrollToItem(0) }
        }
    }

    // Back cancels an active move; otherwise jumps to the first item of the current section
    // (Continue Watching -> first watch item; Apps/Header -> first app). If already at the
    // first item with nowhere left to go back to, starts the screensaver / Ambient Mode if enabled.
    BackHandler {
        if (state.movingKey != null) {
            viewModel.endMove()
        } else {
            val inWatchSection = watchRowFocused || (!headerFocused && lastFocusedKey in watchKeys)
            val firstWatchKey = watchItems.firstOrNull()?.key
            val firstAppKey = state.apps.firstOrNull()?.packageName ?: gridKeys.firstOrNull()
            val alreadyAtSectionStart = if (inWatchSection && firstWatchKey != null) {
                !headerFocused && lastFocusedKey == firstWatchKey
            } else {
                !headerFocused && lastFocusedKey == firstAppKey
            }
            if (alreadyAtSectionStart) {
                if (state.backToAmbient) {
                    AppActions.startScreensaver(context)
                }
            } else {
                focusJob?.cancel()
                focusJob = scope.launch {
                    if (inWatchSection && watchItems.isNotEmpty()) {
                        focusFirstWatchItem()
                    } else {
                        focusFirstApp()
                    }
                }
            }
        }
    }

    // Home button always returns highlight to the first app in the list.
    LaunchedEffect(state.homeRequestCount) {
        if (state.homeRequestCount == 0) return@LaunchedEffect
        menuApp = null
        menuWatchItem = null
        showHidden = false
        showCustomize = false
        showInputs = false
        if (state.loaded) {
            focusJob?.cancel()
            focusFirstApp()
        }
    }

    // Initial focus on first launch (always first app), and keeping focus in the grid when
    // the focused tile goes away (hidden, uninstalled, or "More Apps" emptied).
    LaunchedEffect(state.loaded, gridKeys) {
        if (!state.loaded || gridKeys.isEmpty()) return@LaunchedEffect
        if (!initialFocusPlaced) {
            focusFirstApp()
            initialFocusPlaced = true
            return@LaunchedEffect
        }
        if (lastFocusedKey in gridKeys) return@LaunchedEffect
        val target = gridKeys[lastFocusedIndex.coerceIn(0, gridKeys.lastIndex)]
        requesterFor(target).requestFocusWhenReady()
    }

    // With focus on the header or the top row, show the grid from its very top so the first
    // section title isn't left half-hidden under the clock. The focus scroll that brings the
    // focused tile into view only scrolls the minimum, and would cancel a scroll started at the
    // same time, so this waits a frame and retries until the grid really is at the top.
    val atTopRow = headerFocused || watchRowFocused || (watchItems.isEmpty() && firstAppRowFocused)
    LaunchedEffect(atTopRow) {
        if (!atTopRow) return@LaunchedEffect
        repeat(3) {
            withFrameNanos { }
            if (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0) return@LaunchedEffect
            runCatching { gridState.animateScrollToItem(0) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 28.dp)
                // Entering Continue Watching from above (header shortcuts) or below (first app row)
                // always lands on its first item, rather than the geometrically nearest tile.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (state.movingKey != null || watchItems.isEmpty() || watchRowFocused) {
                        return@onPreviewKeyEvent false
                    }
                    val enterRow = when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> headerFocused
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            val key = lastFocusedKey
                            val appIndex = if (key == null || key in watchKeys) -1
                            else gridKeys.indexOf(key) - watchKeys.size
                            !headerFocused && appIndex in 0 until columns
                        }
                        else -> false
                    }
                    if (!enterRow) return@onPreviewKeyEvent false
                    focusJob?.cancel()
                    focusJob = scope.launch { focusFirstWatchItem() }
                    true
                },
        ) {
            ClockHeader(
                language = state.language,
                onOpenNetwork = { AppActions.openNetworkSettings(context) },
                onOpenAudioOutput = { AppActions.openAudioOutput(context) },
                onOpenSettings = { AppActions.openSystemSettings(context) },
                showInputsButton = tvInputs.isNotEmpty(),
                onOpenInputs = { showInputs = true },
                modifier = Modifier
                    .zIndex(1f)
                    .padding(horizontal = 48.dp)
                    .focusProperties { canFocus = initialFocusPlaced }
                    .onFocusChanged { headerFocused = it.hasFocus },
            )

            CompositionLocalProvider(LocalBringIntoViewSpec provides scrollSpec) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    // Generous padding so the enlarged focused/moving tile and bottom app label
                    // sit comfortably inside the TV safe area without being clipped or faded out.
                    contentPadding = PaddingValues(
                        start = GRID_PADDING_X,
                        end = GRID_PADDING_X,
                        top = 28.dp,
                        bottom = 56.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(TILE_SPACING),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalFadeEdges(top = 20.dp, bottom = 28.dp),
                ) {
                    if (state.loaded) {
                        if (watchItems.isNotEmpty()) {
                            item(key = WATCH_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                                SectionTitle(stringResource(R.string.section_continue_watching), accentBar = true)
                            }
                            item(key = WATCH_ROW_KEY, span = { GridItemSpan(maxLineSpan) }) {
                                WatchRow(
                                    items = watchItems,
                                    movingKey = state.movingKey,
                                    spacing = TILE_SPACING,
                                    bleedX = GRID_PADDING_X,
                                    requesterFor = ::requesterFor,
                                    rowState = watchRowState,
                                    onFocused = { item ->
                                        lastFocusedKey = item.key
                                        lastFocusedIndex = watchKeys.indexOf(item.key)
                                        ToolkitColors.focusedAmbientColor = item.glow
                                    },
                                    onFocusInRowChanged = { watchRowFocused = it },
                                    onClick = { item ->
                                        if (state.movingKey == null) {
                                            viewModel.watchItemIntent(item)?.let { context.safeStart(it) }
                                        }
                                    },
                                    onLongClick = { item -> if (state.movingKey == null) menuWatchItem = item },
                                    onMoveBy = viewModel::moveBy,
                                    onDrop = viewModel::endMove,
                                    modifier = Modifier
                                        .focusProperties { canFocus = initialFocusPlaced }
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            val trimPx = watchRowCollapse.roundToPx()
                                            val height = (placeable.height - trimPx).coerceAtLeast(0)
                                            layout(placeable.width, height) {
                                                placeable.place(0, 0)
                                            }
                                        },
                                )
                            }
                        }
                        // Full width, so it also starts the apps on their own row.
                        item(key = APPS_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                            SectionTitle(stringResource(R.string.section_apps), accentBar = false)
                        }
                        itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
                            val isMoving = app.packageName == state.movingKey
                            AppTile(
                                app = app,
                                index = index,
                                columns = columns,
                                isMoving = isMoving,
                                dimmed = state.movingKey != null && !isMoving,
                                focusRequester = requesterFor(app.packageName),
                                onFocused = {
                                    lastFocusedKey = app.packageName
                                    lastFocusedIndex = watchKeys.size + index
                                    firstAppRowFocused = index < columns
                                    ToolkitColors.focusedAmbientColor = app.accent
                                },
                                onClick = {
                                    if (state.movingKey == null) context.safeStart(viewModel.launchIntent(app))
                                },
                                onLongClick = { if (state.movingKey == null) menuApp = app },
                                onMoveBy = viewModel::moveBy,
                                onDrop = viewModel::endMove,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    // Slide only while moving; hiding/unhiding applies instantly.
                                    placementSpec = if (state.movingKey != null) spring(stiffness = Spring.StiffnessMediumLow) else null,
                                ),
                            )
                        }
                        if (hasMoreApps) {
                            item(key = MORE_APPS_KEY) {
                                MoreAppsTile(
                                    dimmed = state.movingKey != null,
                                    focusRequester = requesterFor(MORE_APPS_KEY),
                                    onFocused = {
                                        lastFocusedKey = MORE_APPS_KEY
                                        lastFocusedIndex = watchKeys.size + state.apps.size
                                        firstAppRowFocused = state.apps.size < columns
                                        ToolkitColors.focusedAmbientColor = ToolkitColors.GlowA
                                    },
                                    onClick = { if (state.movingKey == null) showHidden = true },
                                )
                            }
                        }
                        item(key = CUSTOMIZE_KEY) {
                            val customizeOffset = state.apps.size + if (hasMoreApps) 1 else 0
                            CustomizeTile(
                                dimmed = state.movingKey != null,
                                focusRequester = requesterFor(CUSTOMIZE_KEY),
                                onFocused = {
                                    lastFocusedKey = CUSTOMIZE_KEY
                                    lastFocusedIndex = watchKeys.size + customizeOffset
                                    firstAppRowFocused = customizeOffset < columns
                                    ToolkitColors.focusedAmbientColor = ToolkitColors.GlowA
                                },
                                onClick = { if (state.movingKey == null) showCustomize = true },
                                badgeText = state.availableUpdateVersion.takeIf { it.isNotBlank() }?.let {
                                    stringResource(R.string.update_badge_new)
                                },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !state.loaded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            LoadingIndicator()
        }

        AnimatedVisibility(
            visible = state.movingKey != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            MoveHint()
        }
    }

    if (showHidden && state.hiddenApps.isNotEmpty()) {
        HiddenAppsDialog(
            apps = state.hiddenApps,
            onDismiss = { showHidden = false },
            onLaunch = { app ->
                showHidden = false
                context.safeStart(viewModel.launchIntent(app))
            },
            onLongPress = { menuApp = it },
        )
    }

    if (showInputs && tvInputs.isNotEmpty()) {
        TvInputsDialog(
            inputs = tvInputs,
            onSelectInput = { input ->
                showInputs = false
                TvInputs.openInput(context, input)
            },
            onDismiss = { showInputs = false },
        )
    }

    if (showCustomize) {
        CustomizeDialog(
            showWatchRow = state.showWatchRow,
            appColumns = columns,
            backToAmbient = state.backToAmbient,
            language = state.language,
            themePalette = state.themePalette,
            dynamicAmbientColor = state.dynamicAmbientColor,
            hasCustomWallpaper = state.customWallpaper != null,
            autoLaunchTarget = state.autoLaunchTarget,
            autoLaunchOnBoot = state.autoLaunchOnBoot,
            autoLaunchOnWake = state.autoLaunchOnWake,
            updateCheckInterval = state.updateCheckInterval,
            availableUpdateVersion = state.availableUpdateVersion,
            tvInputs = tvInputs,
            apps = state.apps + state.hiddenApps,
            disabledWatchPackages = state.disabledWatchPackages,
            activeWatchPackages = state.activeWatchPackages,
            remoteButtons = state.remoteButtons,
            customKeyMappings = state.customKeyMappings,
            overrideHomeKey = state.overrideHomeKey,
            onToggleWatchRow = viewModel::setWatchRowVisible,
            onSetAppColumns = viewModel::setAppColumns,
            onToggleBackToAmbient = viewModel::setBackToAmbientEnabled,
            onSetLanguage = viewModel::setLanguage,
            onSetThemePalette = viewModel::setThemePalette,
            onSetDynamicAmbient = viewModel::setDynamicAmbientEnabled,
            onSetWallpaperFromUri = viewModel::setWallpaperFromUri,
            onSetWallpaperFromFile = viewModel::setWallpaperFromFile,
            onSetWallpaperFromUrl = viewModel::setWallpaperFromUrl,
            onClearWallpaper = viewModel::clearCustomWallpaper,
            onSetAutoLaunchTarget = viewModel::setAutoLaunchTarget,
            onSetAutoLaunchOnBoot = viewModel::setAutoLaunchOnBootEnabled,
            onSetAutoLaunchOnWake = viewModel::setAutoLaunchOnWakeEnabled,
            onSetUpdateCheckInterval = viewModel::setUpdateCheckInterval,
            onUpdateCheckResult = viewModel::onUpdateCheckResult,
            onRestoreCompleted = viewModel::reloadAfterRestore,
            onToggleWatchPackage = viewModel::setWatchPackageEnabled,
            onSetRemoteButtonTarget = viewModel::setRemoteButtonTarget,
            onSetRemoteButtonCustomKey = viewModel::setRemoteButtonCustomKey,
            onUpsertCustomKeyMapping = viewModel::upsertCustomKeyMapping,
            onRemoveCustomKeyMapping = viewModel::removeCustomKeyMapping,
            onToggleOverrideHomeKey = viewModel::setOverrideHomeKeyEnabled,
            onRefresh = viewModel::refresh,
            onDismiss = { showCustomize = false },
        )
    }

    menuWatchItem?.let { item ->
        WatchActionsDialog(
            item = item,
            onDismiss = { menuWatchItem = null },
            onOpen = {
                menuWatchItem = null
                viewModel.watchItemIntent(item)?.let { context.safeStart(it) }
            },
            onRemove = {
                menuWatchItem = null
                viewModel.removeWatchItem(item)
            },
            onClearAll = {
                menuWatchItem = null
                viewModel.clearAllWatchItems()
            },
        )
    }

    menuApp?.let { app ->
        val isHidden = state.hiddenApps.any { it.packageName == app.packageName }
        val dismiss = { menuApp = null }
        AppActionsDialog(
            app = app,
            isHidden = isHidden,
            onDismiss = dismiss,
            onMove = { dismiss(); viewModel.startMove(app.packageName) },
            onToggleHidden = { dismiss(); viewModel.setHidden(app.packageName, !isHidden) },
            onAppInfo = { dismiss(); AppActions.openAppInfo(context, app.packageName) },
            onForceStop = { dismiss(); AppActions.forceStop(context, app.packageName) },
            onUninstall = { dismiss(); AppActions.uninstall(context, app.packageName) },
        )
    }
}

@Composable
private fun SectionTitle(text: String, accentBar: Boolean) {
    // Sits closer to its row than the grid's row spacing would put it.
    Row(Modifier.offset(y = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (accentBar) {
            Spacer(
                Modifier
                    .size(width = 4.dp, height = 16.dp)
                    .background(
                        Brush.verticalGradient(listOf(ToolkitColors.Accent, ToolkitColors.GlowB)),
                        RoundedCornerShape(50),
                    ),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            color = ToolkitColors.OnSurface,
        )
    }
}

@Composable
private fun MoveHint() {
    val shape = RoundedCornerShape(50)
    Text(
        stringResource(R.string.move_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = ToolkitColors.OnSurface,
        modifier = Modifier
            .background(ToolkitColors.SurfaceRaised.copy(alpha = 0.95f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}

@Composable
private fun LoadingIndicator() {
    val transition = rememberInfiniteTransition(label = "loading")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
        ),
        label = "spinnerAngle",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(Modifier.size(44.dp)) {
            val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(
                color = ToolkitColors.OnSurface.copy(alpha = 0.1f),
                style = stroke,
            )
            rotate(angle) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            ToolkitColors.Accent.copy(alpha = 0.35f),
                            ToolkitColors.Accent,
                        )
                    ),
                    startAngle = 15f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = stroke,
                )
            }
        }
        Text(
            stringResource(R.string.loading_apps),
            style = MaterialTheme.typography.bodyMedium,
            color = ToolkitColors.OnSurfaceMuted,
        )
    }
}
