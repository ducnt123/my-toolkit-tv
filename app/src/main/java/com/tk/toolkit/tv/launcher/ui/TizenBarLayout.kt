package com.tk.toolkit.tv.launcher.ui

import android.media.tv.TvInputManager
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.launcher.data.AppChannelGroup
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.TvInputEntry
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.launch

private sealed interface TizenShelfTarget {
    data object None : TizenShelfTarget
    data object Menu : TizenShelfTarget
    data object Inputs : TizenShelfTarget
    data class App(val packageName: String) : TizenShelfTarget
}

private data class AppShelfEntry(
    val item: WatchItem,
    /** Non-null on the first thumbnail of each group ("Tiếp tục xem" or recommendation channel title). */
    val groupHeaderTitle: String?,
    /** True when a vertical divider should precede this group. */
    val showGroupDivider: Boolean,
)

private data class MenuQuickCardData(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val badge: String? = null,
    val onClick: () -> Unit,
)

@Composable
fun TizenBarLayout(
    apps: List<LauncherApp>,
    hasMoreApps: Boolean,
    watchItems: List<WatchItem>,
    previewChannelsByPackage: Map<String, List<AppChannelGroup>>,
    showWatchRow: Boolean,
    tvInputs: List<TvInputEntry>,
    lockedPackages: Set<String>,
    movingKey: String?,
    availableUpdateVersion: String,
    homeRequestCount: Int,
    onLaunchApp: (LauncherApp) -> Unit,
    onAppLongPress: (LauncherApp, Rect?) -> Unit,
    onLaunchWatchItem: (WatchItem) -> Unit,
    onWatchItemLongPress: (WatchItem) -> Unit,
    onMoveAppBy: (Int) -> Unit,
    onEndMove: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenSearch: () -> Unit,
    onStartScreensaver: () -> Unit,
    onSelectInput: (TvInputEntry) -> Unit,
    onSwitchToGrid: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appRowState = rememberLazyListState()
    val shelfRowState = rememberLazyListState()

    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(key: String): FocusRequester = requesters.getOrPut(key) { FocusRequester() }

    val tileBounds = remember { mutableMapOf<String, Rect>() }

    var shelfTarget by remember { mutableStateOf<TizenShelfTarget>(TizenShelfTarget.None) }
    var lastBottomFocusKey by remember { mutableStateOf<String?>(null) }
    var shelfFocused by remember { mutableStateOf(false) }

    val appsByPkg = remember(apps) { apps.associateBy { it.packageName } }

    // Build contextual entries when an App is highlighted
    val activeAppEntries: List<AppShelfEntry> = remember(
        shelfTarget,
        watchItems,
        previewChannelsByPackage,
        showWatchRow,
        appsByPkg,
        context,
    ) {
        val target = shelfTarget as? TizenShelfTarget.App ?: return@remember emptyList()
        if (!showWatchRow) return@remember emptyList()
        val pkg = target.packageName
        val appLabel = appsByPkg[pkg]?.label ?: pkg

        val continueWatching = watchItems.filter { it.packageName == pkg && !it.key.startsWith("pinned:") }
        val pinnedForApp = watchItems.filter { it.packageName == pkg && it.key.startsWith("pinned:") }
        val channelGroups = previewChannelsByPackage[pkg].orEmpty()

        val entries = ArrayList<AppShelfEntry>()

        if (continueWatching.isNotEmpty()) {
            val header = context.getString(R.string.section_continue_watching)
            continueWatching.forEachIndexed { idx, item ->
                entries += AppShelfEntry(
                    item = item,
                    groupHeaderTitle = if (idx == 0) header else null,
                    showGroupDivider = false,
                )
            }
        }

        if (channelGroups.isNotEmpty()) {
            for (group in channelGroups) {
                if (group.items.isEmpty()) continue
                val needDivider = entries.isNotEmpty()
                group.items.forEachIndexed { idx, item ->
                    entries += AppShelfEntry(
                        item = item,
                        groupHeaderTitle = if (idx == 0) group.channelTitle else null,
                        showGroupDivider = idx == 0 && needDivider,
                    )
                }
            }
        } else if (pinnedForApp.isNotEmpty()) {
            val needDivider = entries.isNotEmpty()
            val recTitle = context.getString(R.string.tizen_recommended_section, appLabel)
            pinnedForApp.forEachIndexed { idx, item ->
                entries += AppShelfEntry(
                    item = item,
                    groupHeaderTitle = if (idx == 0) recTitle else null,
                    showGroupDivider = idx == 0 && needDivider,
                )
            }
        }

        entries
    }

    // Build Menu quick cards when Menu (☰) is highlighted
    val menuCards: List<MenuQuickCardData> = remember(
        hasMoreApps,
        availableUpdateVersion,
        onSwitchToGrid,
        context,
    ) {
        buildList {
            if (onSwitchToGrid != null) {
                add(
                    MenuQuickCardData(
                        id = "menu_grid",
                        icon = Icons.Outlined.GridView,
                        title = context.getString(R.string.tizen_open_grid_label),
                        subtitle = context.getString(R.string.tizen_open_grid_subtitle),
                        onClick = onSwitchToGrid,
                    )
                )
            }
            if (hasMoreApps) {
                add(
                    MenuQuickCardData(
                        id = "menu_more_apps",
                        icon = Icons.Outlined.Apps,
                        title = context.getString(R.string.tile_more_apps),
                        subtitle = context.getString(R.string.hidden_apps_title),
                        onClick = onOpenHiddenApps,
                    )
                )
            }
            add(
                MenuQuickCardData(
                    id = "menu_customize",
                    icon = Icons.Outlined.Tune,
                    title = context.getString(R.string.tile_customize),
                    subtitle = context.getString(R.string.group_appearance),
                    badge = availableUpdateVersion.takeIf { it.isNotBlank() }?.let {
                        context.getString(R.string.update_badge_new)
                    },
                    onClick = onOpenCustomize,
                )
            )
            add(
                MenuQuickCardData(
                    id = "menu_settings",
                    icon = Icons.Outlined.Settings,
                    title = context.getString(R.string.header_settings),
                    subtitle = context.getString(R.string.remote_action_system_settings),
                    onClick = onOpenSettings,
                )
            )
            add(
                MenuQuickCardData(
                    id = "menu_audio",
                    icon = Icons.Outlined.Speaker,
                    title = context.getString(R.string.header_audio_output),
                    subtitle = context.getString(R.string.header_shortcut_audio_output),
                    onClick = onOpenAudioOutput,
                )
            )
            add(
                MenuQuickCardData(
                    id = "menu_network",
                    icon = Icons.Outlined.Wifi,
                    title = context.getString(R.string.header_shortcut_network),
                    subtitle = context.getString(R.string.header_network_wifi),
                    onClick = onOpenNetwork,
                )
            )
            add(
                MenuQuickCardData(
                    id = "menu_bluetooth",
                    icon = Icons.Outlined.Bluetooth,
                    title = context.getString(R.string.tizen_bluetooth_label),
                    subtitle = context.getString(R.string.tizen_bluetooth_subtitle),
                    onClick = onOpenBluetooth,
                )
            )
        }
    }

    val isShelfVisible = when (shelfTarget) {
        TizenShelfTarget.None -> false
        TizenShelfTarget.Menu -> menuCards.isNotEmpty()
        TizenShelfTarget.Inputs -> tvInputs.isNotEmpty()
        is TizenShelfTarget.App -> activeAppEntries.isNotEmpty()
    }

    val firstShelfItemKey: String? = when (shelfTarget) {
        TizenShelfTarget.None -> null
        TizenShelfTarget.Menu -> menuCards.firstOrNull()?.let { "shelf_menu_${it.id}" }
        TizenShelfTarget.Inputs -> tvInputs.firstOrNull()?.let { "shelf_input_${it.id}" }
        is TizenShelfTarget.App -> activeAppEntries.firstOrNull()?.let { "shelf_watch_${it.item.key}" }
    }

    fun focusFirstShelfItem(): Boolean {
        val targetKey = firstShelfItemKey ?: return false
        scope.launch {
            runCatching { shelfRowState.scrollToItem(0) }
            requesterFor(targetKey).requestFocusWhenReady()
        }
        return true
    }

    fun returnFocusToBottomBar(): Boolean {
        val key = lastBottomFocusKey ?: apps.firstOrNull()?.let { "tizen_app_${it.packageName}" } ?: "tizen_btn_menu"
        scope.launch {
            requesterFor(key).requestFocusWhenReady()
        }
        return true
    }

    // Place initial focus on the first app tile (or Menu button if no apps yet)
    LaunchedEffect(apps.isNotEmpty(), homeRequestCount) {
        val firstApp = apps.firstOrNull()
        val key = if (firstApp != null) "tizen_app_${firstApp.packageName}" else "tizen_btn_menu"
        if (firstApp != null) {
            runCatching { appRowState.scrollToItem(0) }
        }
        requesterFor(key).requestFocusWhenReady()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 20.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val code = event.nativeKeyEvent.keyCode

                // Handle moving app left/right on the Tizen Bar
                if (movingKey != null) {
                    return@onPreviewKeyEvent when (code) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onMoveAppBy(-1)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            onMoveAppBy(1)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BACK -> {
                            onEndMove()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> true
                        else -> false
                    }
                }

                when (code) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!shelfFocused && isShelfVisible) {
                            focusFirstShelfItem()
                        } else {
                            false
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (shelfFocused) {
                            returnFocusToBottomBar()
                        } else {
                            false
                        }
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (shelfFocused) {
                            returnFocusToBottomBar()
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.Bottom,
    ) {
        // --- 1. UPPER CONTEXTUAL PREVIEW SHELF ---
        AnimatedVisibility(
            visible = isShelfVisible && movingKey == null,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                expandVertically(expandFrom = Alignment.Bottom, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color(0xFF0C121A).copy(alpha = 0.86f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    )
                    .padding(top = 14.dp, bottom = 22.dp)
                    .onFocusChanged { shelfFocused = it.hasFocus },
            ) {
                LazyRow(
                    state = shelfRowState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    when (shelfTarget) {
                        TizenShelfTarget.Menu -> {
                            itemsIndexed(menuCards, key = { _, card -> "shelf_menu_${card.id}" }) { idx, card ->
                                val itemKey = "shelf_menu_${card.id}"
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (idx == 0) stringResource(R.string.tizen_menu_label) else "",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ToolkitColors.OnSurface,
                                        modifier = Modifier.height(22.dp),
                                    )
                                    TizenMenuQuickCard(
                                        card = card,
                                        focusRequester = requesterFor(itemKey),
                                    )
                                }
                            }
                        }
                        TizenShelfTarget.Inputs -> {
                            itemsIndexed(tvInputs, key = { _, input -> "shelf_input_${input.id}" }) { idx, input ->
                                val itemKey = "shelf_input_${input.id}"
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (idx == 0) stringResource(R.string.header_inputs) else "",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ToolkitColors.OnSurface,
                                        modifier = Modifier.height(22.dp),
                                    )
                                    TizenInputShelfCard(
                                        input = input,
                                        focusRequester = requesterFor(itemKey),
                                        onClick = { onSelectInput(input) },
                                    )
                                }
                            }
                        }
                        is TizenShelfTarget.App -> {
                            itemsIndexed(
                                activeAppEntries,
                                key = { _, entry -> "shelf_watch_${entry.item.key}" },
                            ) { _, entry ->
                                val itemKey = "shelf_watch_${entry.item.key}"
                                Row(verticalAlignment = Alignment.Bottom) {
                                    if (entry.showGroupDivider) {
                                        Box(
                                            Modifier
                                                .padding(start = 6.dp, end = 20.dp, bottom = 18.dp)
                                                .width(1.dp)
                                                .height(145.dp)
                                                .background(Color.White.copy(alpha = 0.16f)),
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Group title sits directly above the first thumbnail of that group
                                        Box(Modifier.height(22.dp), contentAlignment = Alignment.CenterStart) {
                                            if (!entry.groupHeaderTitle.isNullOrBlank()) {
                                                Text(
                                                    text = entry.groupHeaderTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = ToolkitColors.OnSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                        TizenWatchShelfCard(
                                            item = entry.item,
                                            focusRequester = requesterFor(itemKey),
                                            onClick = { onLaunchWatchItem(entry.item) },
                                            onLongClick = { onWatchItemLongPress(entry.item) },
                                        )
                                    }
                                }
                            }
                        }
                        TizenShelfTarget.None -> Unit
                    }
                }
            }
        }

        // --- 2. BOTTOM TIZEN RIBBON BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .drawBehind {
                    val barShapeRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0E222E).copy(alpha = 0.96f),
                                ToolkitColors.SurfaceRaised.copy(alpha = 0.96f),
                            )
                        ),
                        cornerRadius = barShapeRadius,
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f),
                        cornerRadius = barShapeRadius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    )
                }
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left 4 Option Buttons: Menu (☰), Search (🔍), Screensaver (🌙), Inputs (Inputs)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TizenLeftOptionButton(
                        icon = Icons.Outlined.Menu,
                        label = stringResource(R.string.tizen_menu_label),
                        isActiveShelf = shelfTarget == TizenShelfTarget.Menu,
                        focusRequester = requesterFor("tizen_btn_menu"),
                        onFocused = {
                            lastBottomFocusKey = "tizen_btn_menu"
                            shelfTarget = TizenShelfTarget.Menu
                        },
                        onClick = {
                            shelfTarget = TizenShelfTarget.Menu
                            focusFirstShelfItem()
                        },
                    )
                    TizenLeftOptionButton(
                        icon = Icons.Outlined.Search,
                        label = stringResource(R.string.header_search),
                        isActiveShelf = false,
                        focusRequester = requesterFor("tizen_btn_search"),
                        onFocused = {
                            lastBottomFocusKey = "tizen_btn_search"
                            shelfTarget = TizenShelfTarget.None
                        },
                        onClick = onOpenSearch,
                    )
                    TizenLeftOptionButton(
                        icon = Icons.Outlined.Bedtime,
                        label = stringResource(R.string.tizen_screensaver_label),
                        isActiveShelf = false,
                        focusRequester = requesterFor("tizen_btn_screensaver"),
                        onFocused = {
                            lastBottomFocusKey = "tizen_btn_screensaver"
                            shelfTarget = TizenShelfTarget.None
                        },
                        onClick = onStartScreensaver,
                    )
                    if (tvInputs.isNotEmpty()) {
                        TizenLeftOptionButton(
                            icon = Icons.AutoMirrored.Outlined.Input,
                            label = stringResource(R.string.header_inputs),
                            isActiveShelf = shelfTarget == TizenShelfTarget.Inputs,
                            tint = Color(0xFF6EE7B7),
                            focusRequester = requesterFor("tizen_btn_inputs"),
                            onFocused = {
                                lastBottomFocusKey = "tizen_btn_inputs"
                                shelfTarget = TizenShelfTarget.Inputs
                            },
                            onClick = {
                                shelfTarget = TizenShelfTarget.Inputs
                                focusFirstShelfItem()
                            },
                        )
                    }
                }

                // Vertical separator between Left Option Buttons and Square App Icons
                Box(
                    Modifier
                        .padding(horizontal = 18.dp)
                        .width(1.dp)
                        .height(68.dp)
                        .background(Color.White.copy(alpha = 0.2f)),
                )

                // Horizontal Ribbon of 1:1 Square App Icons
                LazyRow(
                    state = appRowState,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { _, app ->
                        val itemKey = "tizen_app_${app.packageName}"
                        val isMoving = movingKey == app.packageName
                        val isDimmed = movingKey != null && !isMoving
                        TizenSquareAppTile(
                            app = app,
                            locked = app.packageName in lockedPackages,
                            moving = isMoving,
                            dimmed = isDimmed,
                            focusRequester = requesterFor(itemKey),
                            onBoundsChanged = { rect -> tileBounds[app.packageName] = rect },
                            onFocused = {
                                lastBottomFocusKey = itemKey
                                shelfTarget = TizenShelfTarget.App(app.packageName)
                                ToolkitColors.focusedAmbientColor = app.accent
                            },
                            onClick = {
                                if (movingKey == null) {
                                    onLaunchApp(app)
                                } else {
                                    onEndMove()
                                }
                            },
                            onLongClick = {
                                if (movingKey == null) {
                                    onAppLongPress(app, tileBounds[app.packageName])
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TizenLeftOptionButton(
    icon: ImageVector,
    label: String,
    isActiveShelf: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    tint: Color = ToolkitColors.OnSurface,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.15f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "leftBtnScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        focused -> Color.White
                        isActiveShelf -> Color.White.copy(alpha = 0.16f)
                        else -> Color.Transparent
                    }
                )
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                            event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                            event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                    ) {
                        onClick()
                        true
                    } else {
                        false
                    }
                },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (focused) Color(0xFF0A121A) else tint,
                modifier = Modifier.size(24.dp),
            )
        }
        if (focused) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TizenSquareAppTile(
    app: LauncherApp,
    locked: Boolean,
    moving: Boolean,
    dimmed: Boolean,
    focusRequester: FocusRequester,
    onBoundsChanged: (Rect) -> Unit,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val active = focused || moving
    val tileSize by animateDpAsState(
        targetValue = if (active) 104.dp else 84.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "sqTileSize",
    )
    val offsetY by animateDpAsState(
        targetValue = if (active) (-16).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "sqOffsetY",
    )
    val shape = RoundedCornerShape(if (active) 14.dp else 11.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = offsetY)
            .size(tileSize)
            .onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) }
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .graphicsLayer { alpha = if (dimmed) 0.45f else 1f }
            .then(
                if (active) {
                    Modifier.border(
                        width = if (moving) 3.dp else 2.5.dp,
                        color = if (moving) ToolkitColors.Accent else Color.White,
                        shape = shape,
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = shape,
                    )
                }
            )
            .clip(shape)
            .background(app.accent.copy(alpha = 0.92f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom gradient label strip when focused so the user always sees the app's name clearly
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f),
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (locked) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.72f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TizenWatchShelfCard(
    item: WatchItem,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "watchCardScale",
    )
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier.width(236.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(133.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (focused) {
                        Modifier.border(2.5.dp, Color.White, shape)
                    } else {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                    }
                )
                .clip(shape)
                .background(Color(0xFF161D28))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            val bmp = item.artworkBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    item.glow.copy(alpha = 0.55f),
                                    Color(0xFF121620),
                                )
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item.progress?.let { prog ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.28f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(prog.coerceIn(0.04f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFFFBBF24)),
                    )
                }
            }
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (focused) Color.White else ToolkitColors.OnSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun TizenMenuQuickCard(
    card: MenuQuickCardData,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "menuCardScale",
    )
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .width(184.dp)
            .height(112.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (focused) {
                    Modifier.border(2.5.dp, Color.White, shape)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                }
            )
            .clip(shape)
            .background(
                if (focused) Color(0xFF1D3244) else Color(0xFF152230).copy(alpha = 0.92f)
            )
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                ) {
                    card.onClick()
                    true
                } else {
                    false
                }
            }
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .background(ToolkitColors.Accent.copy(alpha = 0.18f), RoundedCornerShape(9.dp)),
                ) {
                    Icon(
                        imageVector = card.icon,
                        contentDescription = card.title,
                        tint = ToolkitColors.Accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (!card.badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(ToolkitColors.Accent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = card.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = ToolkitColors.Background,
                        )
                    }
                }
            }
            Column {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ToolkitColors.OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TizenInputShelfCard(
    input: TvInputEntry,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "inputCardScale",
    )
    val shape = RoundedCornerShape(12.dp)
    val title = remember(input, context) { input.displayTitle(context) }
    val stateText = remember(input, context) { input.stateLabel(context) }
    val isConnected = input.isLiveTv || input.state == TvInputManager.INPUT_STATE_CONNECTED

    Box(
        modifier = Modifier
            .width(196.dp)
            .height(112.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (focused) {
                    Modifier.border(2.5.dp, Color.White, shape)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                }
            )
            .clip(shape)
            .background(
                if (focused) Color(0xFF1D3244) else Color(0xFF152230).copy(alpha = 0.92f)
            )
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(9.dp))
                        .padding(5.dp),
                ) {
                    val bmp = input.icon
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Input,
                            contentDescription = title,
                            tint = ToolkitColors.Accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(Color(0xFF6EE7B7), CircleShape),
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) Color(0xFF6EE7B7) else ToolkitColors.OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
