package com.tk.toolkit.tv.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.LauncherViewModel
import com.tk.toolkit.tv.launcher.data.AppActions
import com.tk.toolkit.tv.launcher.data.AppActions.safeStart
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private const val COLUMNS = 6
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
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFocusedIndex by rememberSaveable { mutableStateOf(0) }
    // Not saved: every fresh start begins on the first app.
    var initialFocusPlaced by remember { mutableStateOf(false) }
    var headerFocused by remember { mutableStateOf(false) }
    var watchRowFocused by remember { mutableStateOf(false) }
    var firstAppRowFocused by remember { mutableStateOf(false) }

    val scrollSpec = with(LocalDensity.current) { remember { minimalScrollSpec(28.dp.toPx()) } }
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
    // (Continue Watching -> first watch item; Apps/Header -> first app).
    BackHandler {
        if (state.movingKey != null) {
            viewModel.endMove()
        } else {
            focusJob?.cancel()
            val inWatchSection = watchRowFocused || (!headerFocused && lastFocusedKey in watchKeys)
            focusJob = scope.launch {
                if (inWatchSection && watchItems.isNotEmpty()) {
                    focusFirstWatchItem()
                } else {
                    focusFirstApp()
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
                            !headerFocused && appIndex in 0 until COLUMNS
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
                onOpenNetwork = { AppActions.openNetworkSettings(context) },
                onOpenAudioOutput = { AppActions.openAudioOutput(context) },
                onOpenSettings = { AppActions.openSystemSettings(context) },
                modifier = Modifier
                    .zIndex(1f)
                    .padding(horizontal = 48.dp)
                    .focusProperties { canFocus = initialFocusPlaced }
                    .onFocusChanged { headerFocused = it.hasFocus },
            )

            CompositionLocalProvider(LocalBringIntoViewSpec provides scrollSpec) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(COLUMNS),
                    state = gridState,
                    // Generous padding so the enlarged focused/moving tile is never clipped at the edges.
                    contentPadding = PaddingValues(horizontal = GRID_PADDING_X, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(TILE_SPACING),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalFadeEdges(top = 20.dp, bottom = 28.dp),
                ) {
                    if (state.loaded) {
                        if (watchItems.isNotEmpty()) {
                            item(key = WATCH_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                                SectionTitle("Tiếp tục xem", accentBar = true)
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
                                    modifier = Modifier.focusProperties { canFocus = initialFocusPlaced },
                                )
                            }
                        }
                        // Full width, so it also starts the apps on their own row.
                        item(key = APPS_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                            SectionTitle("Ứng dụng", accentBar = false)
                        }
                        itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
                            val isMoving = app.packageName == state.movingKey
                            AppTile(
                                app = app,
                                index = index,
                                columns = COLUMNS,
                                isMoving = isMoving,
                                dimmed = state.movingKey != null && !isMoving,
                                focusRequester = requesterFor(app.packageName),
                                onFocused = {
                                    lastFocusedKey = app.packageName
                                    lastFocusedIndex = watchKeys.size + index
                                    firstAppRowFocused = index < COLUMNS
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
                                        firstAppRowFocused = state.apps.size < COLUMNS
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
                                    firstAppRowFocused = customizeOffset < COLUMNS
                                },
                                onClick = { if (state.movingKey == null) showCustomize = true },
                            )
                        }
                    }
                }
            }
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

    if (showCustomize) {
        CustomizeDialog(
            showWatchRow = state.showWatchRow,
            apps = state.apps + state.hiddenApps,
            disabledWatchPackages = state.disabledWatchPackages,
            activeWatchPackages = state.activeWatchPackages,
            onToggleWatchRow = viewModel::setWatchRowVisible,
            onToggleWatchPackage = viewModel::setWatchPackageEnabled,
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
        "Use the arrows to move  ·  Press OK to drop",
        style = MaterialTheme.typography.bodyMedium,
        color = ToolkitColors.OnSurface,
        modifier = Modifier
            .background(ToolkitColors.SurfaceRaised.copy(alpha = 0.95f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}
