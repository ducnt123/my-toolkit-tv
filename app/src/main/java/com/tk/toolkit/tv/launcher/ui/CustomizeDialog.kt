package com.tk.toolkit.tv.launcher.ui

import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.tk.toolkit.tv.core.ToolkitAccessibilityService
import com.tk.toolkit.tv.launcher.data.CustomKeyMapping
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.RemoteButton
import com.tk.toolkit.tv.launcher.data.RemoteButtonConfig
import com.tk.toolkit.tv.launcher.data.RemoteButtons
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

private sealed interface CustomizePage {
    data object Main : CustomizePage
    data object WatchSources : CustomizePage
    data object RemoteButtonsList : CustomizePage
    data class RemoteButtonDetail(val button: RemoteButton) : CustomizePage
    data class CaptureKey(val button: RemoteButton) : CustomizePage
    data object CaptureNewCustomKey : CustomizePage
    data class CustomKeyDetail(val keyCode: Int, val scanCode: Int, val isNew: Boolean) : CustomizePage
}

@Composable
fun CustomizeDialog(
    showWatchRow: Boolean,
    appColumns: Int,
    backToAmbient: Boolean,
    apps: List<LauncherApp>,
    disabledWatchPackages: Set<String>,
    activeWatchPackages: Set<String>,
    remoteButtons: List<RemoteButtonConfig>,
    customKeyMappings: List<CustomKeyMapping>,
    overrideHomeKey: Boolean,
    onToggleWatchRow: (Boolean) -> Unit,
    onSetAppColumns: (Int) -> Unit,
    onToggleBackToAmbient: (Boolean) -> Unit,
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

    fun navigateBack() {
        page = when (val current = page) {
            CustomizePage.Main -> {
                onDismiss()
                return
            }
            CustomizePage.WatchSources -> CustomizePage.Main
            CustomizePage.RemoteButtonsList -> CustomizePage.Main
            is CustomizePage.RemoteButtonDetail -> CustomizePage.RemoteButtonsList
            is CustomizePage.CaptureKey -> CustomizePage.RemoteButtonDetail(current.button)
            CustomizePage.CaptureNewCustomKey -> CustomizePage.RemoteButtonsList
            is CustomizePage.CustomKeyDetail -> CustomizePage.RemoteButtonsList
        }
    }

    Dialog(
        onDismissRequest = ::navigateBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        when (val current = page) {
            CustomizePage.Main -> {
                MainCustomizePanel(
                    showWatchRow = showWatchRow,
                    appColumns = appColumns,
                    backToAmbient = backToAmbient,
                    enabledCount = apps.count { it.packageName !in disabledWatchPackages },
                    totalCount = apps.size,
                    onToggleWatchRow = onToggleWatchRow,
                    onSetAppColumns = onSetAppColumns,
                    onToggleBackToAmbient = onToggleBackToAmbient,
                    onOpenSources = { page = CustomizePage.WatchSources },
                    onOpenRemoteButtons = { page = CustomizePage.RemoteButtonsList },
                    onRefresh = onRefresh,
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
            CustomizePage.RemoteButtonsList -> {
                RemoteButtonsPanel(
                    configs = remoteButtons,
                    customMappings = customKeyMappings,
                    apps = apps,
                    overrideHomeKey = overrideHomeKey,
                    onToggleOverrideHomeKey = onToggleOverrideHomeKey,
                    onSelectButton = { button -> page = CustomizePage.RemoteButtonDetail(button) },
                    onSelectCustomMapping = { mapping ->
                        page = CustomizePage.CustomKeyDetail(
                            keyCode = mapping.keyCode,
                            scanCode = mapping.scanCode,
                            isNew = false,
                        )
                    },
                    onAddCustomKey = { page = CustomizePage.CaptureNewCustomKey },
                    onBack = ::navigateBack,
                )
            }
            is CustomizePage.RemoteButtonDetail -> {
                val config = remoteButtons.firstOrNull { it.button == current.button }
                    ?: RemoteButtonConfig(current.button)
                RemoteButtonDetailPanel(
                    config = config,
                    apps = apps,
                    onSelectTarget = { target ->
                        onSetRemoteButtonTarget(current.button, target)
                        page = CustomizePage.RemoteButtonsList
                    },
                    onStartCaptureKey = { page = CustomizePage.CaptureKey(current.button) },
                    onClearCustomKey = {
                        onSetRemoteButtonCustomKey(current.button, 0, 0)
                    },
                    onBack = ::navigateBack,
                )
            }
            is CustomizePage.CaptureKey -> {
                val context = LocalContext.current
                CaptureKeyPanel(
                    title = "Nhận diện ${current.button.title}",
                    promptText = "Hãy bấm phím bạn muốn gán làm \"${current.button.title}\" trên điều khiển.\n(Các phím điều hướng, OK, Back, Home và Âm lượng sẽ được bỏ qua)",
                    onKeyCaptured = { keyCode, scanCode ->
                        val label = RemoteButtons.formatKeyLabel(keyCode, scanCode)
                        Toast.makeText(context, "Đã gán phím $label cho ${current.button.title}", Toast.LENGTH_SHORT).show()
                        onSetRemoteButtonCustomKey(current.button, keyCode, scanCode)
                        page = CustomizePage.RemoteButtonDetail(current.button)
                    },
                    onCancel = ::navigateBack,
                )
            }
            CustomizePage.CaptureNewCustomKey -> {
                CaptureKeyPanel(
                    title = "Gán phím tùy chỉnh",
                    promptText = "Hãy bấm phím bất kỳ trên điều khiển từ xa mà bạn muốn nhận diện và gán chức năng.\n(Các phím điều hướng, OK, Back, Home và Âm lượng sẽ được bỏ qua)",
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

@Composable
private fun MainCustomizePanel(
    showWatchRow: Boolean,
    appColumns: Int,
    backToAmbient: Boolean,
    enabledCount: Int,
    totalCount: Int,
    onToggleWatchRow: (Boolean) -> Unit,
    onSetAppColumns: (Int) -> Unit,
    onToggleBackToAmbient: (Boolean) -> Unit,
    onOpenSources: () -> Unit,
    onOpenRemoteButtons: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstAction.requestFocusWhenReady() }

    Column(
        Modifier
            .width(480.dp)
            .clip(PanelShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
            .ignoreHeldSelectKey(),
    ) {
        DialogHeader(
            title = "Tùy chỉnh",
            subtitle = "Cài đặt màn hình chính và phím tắt điều khiển",
        )
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ListItem(
                selected = false,
                onClick = {
                    val next = if (appColumns >= 6) 4 else appColumns + 1
                    onSetAppColumns(next)
                },
                modifier = Modifier
                    .focusRequester(firstAction)
                    .onPreviewKeyEvent { event ->
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
                headlineContent = {
                    Text("Lưới ứng dụng", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(
                        "Hiển thị $appColumns mục trên mỗi hàng",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(4, 5, 6).forEach { count ->
                            val active = count == appColumns
                            val pillShape = RoundedCornerShape(8.dp)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(
                                        if (active) ToolkitColors.Accent else ToolkitColors.OnSurface.copy(alpha = 0.08f),
                                        pillShape,
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) ToolkitColors.Background else ToolkitColors.OnSurfaceMuted,
                                )
                            }
                        }
                    }
                },
                shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                scale = ListItemDefaults.scale(focusedScale = 1.02f),
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                    focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                    focusedContentColor = ToolkitColors.OnSurface,
                ),
            )
            ListItem(
                selected = false,
                onClick = { onToggleWatchRow(!showWatchRow) },
                headlineContent = {
                    Text("Hiển thị hàng Tiếp tục xem", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(
                        if (showWatchRow) "Đang hiện trên màn hình chính" else "Đang ẩn khỏi màn hình chính",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = showWatchRow,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ToolkitColors.Background,
                            checkedTrackColor = ToolkitColors.Accent,
                        ),
                    )
                },
                shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                scale = ListItemDefaults.scale(focusedScale = 1.02f),
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                    focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                    focusedContentColor = ToolkitColors.OnSurface,
                ),
            )
            ActionItem(
                title = "Nguồn ứng dụng Tiếp tục xem",
                subtitle = "Đang bật $enabledCount/$totalCount ứng dụng",
                onClick = onOpenSources,
            )
            ListItem(
                selected = false,
                onClick = { onToggleBackToAmbient(!backToAmbient) },
                headlineContent = {
                    Text("Bấm Back bật Bảo vệ màn hình", style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(
                        if (backToAmbient) "Mở Ambient Mode khi bấm Back ở mục đầu tiên"
                        else "Không mở bảo vệ màn hình khi bấm Back",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = backToAmbient,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ToolkitColors.Background,
                            checkedTrackColor = ToolkitColors.Accent,
                        ),
                    )
                },
                shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                scale = ListItemDefaults.scale(focusedScale = 1.02f),
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                    focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                    focusedContentColor = ToolkitColors.OnSurface,
                ),
            )
            ActionItem(
                title = "Gán phím điều khiển",
                subtitle = "Phím Guide, YouTube, Cài đặt, Chuyển người dùng và phím tùy chỉnh",
                onClick = onOpenRemoteButtons,
            )
            ActionItem(
                title = "Làm mới nội dung",
                subtitle = "Cập nhật lại danh sách Tiếp tục xem và ứng dụng",
                onClick = {
                    onRefresh()
                    Toast.makeText(context, "Đã làm mới nội dung", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

@Composable
private fun RemoteButtonsPanel(
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
    val firstItem = remember { FocusRequester() }
    val appsByPkg = remember(apps) { apps.associateBy { it.packageName } }
    val accessibilityEnabled = ToolkitAccessibilityService.instance != null

    LaunchedEffect(Unit) { firstItem.requestFocusWhenReady() }

    Column(
        Modifier
            .width(520.dp)
            .clip(PanelShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
            .ignoreHeldSelectKey(),
    ) {
        DialogHeader(
            title = "Gán phím điều khiển",
            subtitle = "Chọn ứng dụng hoặc hành động cho các phím trên remote",
        )
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 400.dp),
        ) {
            item(key = "accessibility_status") {
                ListItem(
                    selected = false,
                    onClick = { RemoteButtons.openAccessibilitySettings(context) },
                    modifier = Modifier.focusRequester(firstItem),
                    headlineContent = {
                        Text(
                            if (accessibilityEnabled) "Dịch vụ Trợ năng: Đang bật"
                            else "Dịch vụ Trợ năng: Đang tắt",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Text(
                            if (accessibilityEnabled) "Đang nhận tín hiệu phím tắt ở mọi màn hình"
                            else "Bấm vào đây để bật Toolkit trong Cài đặt › Trợ năng",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    trailingContent = {
                        val badgeShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (accessibilityEnabled) ToolkitColors.Accent.copy(alpha = 0.2f)
                                    else ToolkitColors.Danger.copy(alpha = 0.2f),
                                    badgeShape,
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                if (accessibilityEnabled) "Đang bật" else "Bật ngay",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (accessibilityEnabled) ToolkitColors.Accent else ToolkitColors.Danger,
                            )
                        }
                    },
                    shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }

            item(key = "override_home") {
                ListItem(
                    selected = false,
                    onClick = { onToggleOverrideHomeKey(!overrideHomeKey) },
                    headlineContent = {
                        Text(
                            "Ghi đè phím Home",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = {
                        Text(
                            if (overrideHomeKey) "Đang bật — mở Toolkit khi chưa tắt launcher gốc"
                            else "Đang tắt — dùng khi Toolkit đã là launcher mặc định",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = overrideHomeKey,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ToolkitColors.Background,
                                checkedTrackColor = ToolkitColors.Accent,
                            ),
                        )
                    },
                    shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }

            itemsIndexed(configs, key = { _, cfg -> cfg.button.id }) { _, cfg ->
                val assignedApp = appsByPkg[cfg.target]
                val targetLabel = when (cfg.target) {
                    RemoteButtons.TARGET_DEFAULT -> cfg.button.defaultActionLabel
                    RemoteButtons.TARGET_DISABLED -> "Đã tắt (Không can thiệp)"
                    else -> "Mở ứng dụng: ${assignedApp?.label ?: cfg.target}"
                }
                val customKeyText = if (cfg.customKeyCode > 0 || cfg.customScanCode > 0) {
                    " · Mã phím: ${RemoteButtons.formatKeyLabel(cfg.customKeyCode, cfg.customScanCode)}"
                } else {
                    ""
                }

                ListItem(
                    selected = false,
                    onClick = { onSelectButton(cfg.button) },
                    leadingContent = assignedApp?.let { app ->
                        {
                            Image(
                                app.icon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            cfg.button.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            targetLabel + customKeyText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }

            itemsIndexed(customMappings, key = { _, mapping -> "custom_${mapping.id}" }) { _, mapping ->
                val assignedApp = appsByPkg[mapping.target]
                val keyName = RemoteButtons.formatKeyLabel(mapping.keyCode, mapping.scanCode)
                ListItem(
                    selected = false,
                    onClick = { onSelectCustomMapping(mapping) },
                    leadingContent = assignedApp?.let { app ->
                        {
                            Image(
                                app.icon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            "Phím tùy chỉnh ($keyName)",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            RemoteButtons.formatCustomTargetLabel(mapping.target, appsByPkg),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }

            item(key = "add_custom_key") {
                ActionItem(
                    title = "Gán phím tùy chỉnh",
                    subtitle = "Nhận diện mã phím bất kỳ trên remote và gán mở ứng dụng",
                    onClick = onAddCustomKey,
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

    Column(
        Modifier
            .width(520.dp)
            .clip(PanelShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
            .ignoreHeldSelectKey(),
    ) {
        DialogHeader(
            title = "Phím $keyLabel",
            subtitle = "Chọn ứng dụng hoặc hành động cho mã phím này",
        )
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 390.dp),
        ) {
            if (canRemove) {
                item(key = "remove_custom_key") {
                    ActionItem(
                        title = "Xóa phím tùy chỉnh này",
                        subtitle = "Gỡ bỏ thiết lập gán phím cho $keyLabel",
                        onClick = {
                            onRemove()
                            Toast.makeText(context, "Đã xóa phím tùy chỉnh $keyLabel", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.focusRequester(firstItem),
                        danger = true,
                    )
                }
            }
            item(key = RemoteButtons.ACTION_LIVE_TV) {
                val selected = currentTarget == RemoteButtons.ACTION_LIVE_TV
                ActionChoiceItem(
                    title = "Mở Truyền hình / Live TV",
                    subtitle = if (selected) "Đang chọn" else "Mở ngõ vào truyền hình hoặc ứng dụng TV",
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.ACTION_LIVE_TV) },
                    modifier = if (!canRemove) Modifier.focusRequester(firstItem) else Modifier,
                )
            }
            item(key = RemoteButtons.ACTION_SYSTEM_SETTINGS) {
                val selected = currentTarget == RemoteButtons.ACTION_SYSTEM_SETTINGS
                ActionChoiceItem(
                    title = "Mở Cài đặt hệ thống",
                    subtitle = if (selected) "Đang chọn" else "Mở màn hình Cài đặt Android TV",
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.ACTION_SYSTEM_SETTINGS) },
                )
            }
            item(key = RemoteButtons.ACTION_SYSTEM_ACCOUNTS) {
                val selected = currentTarget == RemoteButtons.ACTION_SYSTEM_ACCOUNTS
                ActionChoiceItem(
                    title = "Mở Tài khoản & Đăng nhập",
                    subtitle = if (selected) "Đang chọn" else "Mở quản lý tài khoản Google TV",
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.ACTION_SYSTEM_ACCOUNTS) },
                )
            }
            itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { _, app ->
                val selected = currentTarget == app.packageName
                ListItem(
                    selected = false,
                    onClick = { onSelectTarget(app.packageName) },
                    leadingContent = {
                        Image(
                            app.icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    },
                    headlineContent = {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            if (selected) "Đang được gán cho phím $keyLabel" else app.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = if (selected) {
                        {
                            Box(
                                modifier = Modifier
                                    .background(ToolkitColors.Accent, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "Đang chọn",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ToolkitColors.Background,
                                )
                            }
                        }
                    } else null,
                    shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun RemoteButtonDetailPanel(
    config: RemoteButtonConfig,
    apps: List<LauncherApp>,
    onSelectTarget: (String) -> Unit,
    onStartCaptureKey: () -> Unit,
    onClearCustomKey: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val firstItem = remember { FocusRequester() }
    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    val hasCustomKey = config.customKeyCode > 0 || config.customScanCode > 0

    LaunchedEffect(Unit) { firstItem.requestFocusWhenReady() }

    Column(
        Modifier
            .width(520.dp)
            .clip(PanelShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
            .ignoreHeldSelectKey(),
    ) {
        DialogHeader(
            title = config.button.title,
            subtitle = "Chọn hành động hoặc ứng dụng mở khi bấm phím",
        )
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 390.dp),
        ) {
            item(key = "action_default") {
                val selected = config.target == RemoteButtons.TARGET_DEFAULT
                ActionChoiceItem(
                    title = config.button.defaultActionLabel,
                    subtitle = if (selected) "Đang chọn" else "Sử dụng hành động mặc định cho phím này",
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.TARGET_DEFAULT) },
                    modifier = Modifier.focusRequester(firstItem),
                )
            }
            item(key = "action_learn_key") {
                ActionChoiceItem(
                    title = "Nhận diện mã phím từ điều khiển",
                    subtitle = if (hasCustomKey) {
                        "Đang dùng mã phím: ${RemoteButtons.formatKeyLabel(config.customKeyCode, config.customScanCode)}"
                    } else {
                        "Dùng khi bấm phím trên remote chưa thấy phản hồi"
                    },
                    selected = false,
                    onClick = onStartCaptureKey,
                )
            }
            if (hasCustomKey) {
                item(key = "action_clear_key") {
                    ActionChoiceItem(
                        title = "Xóa mã phím đã nhận diện",
                        subtitle = "Khôi phục về mã phím chuẩn mặc định",
                        selected = false,
                        onClick = {
                            onClearCustomKey()
                            Toast.makeText(context, "Đã khôi phục mã phím mặc định", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
            item(key = "action_disabled") {
                val selected = config.target == RemoteButtons.TARGET_DISABLED
                ActionChoiceItem(
                    title = "Không can thiệp (Tắt)",
                    subtitle = if (selected) "Đang chọn" else "Để hệ thống tự xử lý phím này",
                    selected = selected,
                    onClick = { onSelectTarget(RemoteButtons.TARGET_DISABLED) },
                )
            }
            itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { _, app ->
                val selected = config.target == app.packageName
                ListItem(
                    selected = false,
                    onClick = { onSelectTarget(app.packageName) },
                    leadingContent = {
                        Image(
                            app.icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    },
                    headlineContent = {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            if (selected) "Đang được gán cho ${config.button.title}" else app.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = if (selected) {
                        {
                            Box(
                                modifier = Modifier
                                    .background(ToolkitColors.Accent, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "Đang chọn",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ToolkitColors.Background,
                                )
                            }
                        }
                    } else null,
                    shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ActionChoiceItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = if (selected) {
            {
                Box(
                    modifier = Modifier
                        .background(ToolkitColors.Accent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "Đang chọn",
                        style = MaterialTheme.typography.labelLarge,
                        color = ToolkitColors.Background,
                    )
                }
            }
        } else null,
        shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ListItemDefaults.scale(focusedScale = 1.02f),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
            focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
            focusedContentColor = ToolkitColors.OnSurface,
        ),
    )
}

@Composable
private fun CaptureKeyPanel(
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
            .width(460.dp)
            .clip(PanelShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
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
            }
            .ignoreHeldSelectKey(),
    ) {
        DialogHeader(
            title = title,
            subtitle = "Nhấn phím tương ứng trên điều khiển từ xa",
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                promptText,
                style = MaterialTheme.typography.bodyMedium,
                color = ToolkitColors.OnSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            ActionItem(
                title = "Hủy",
                subtitle = "Quay lại mà không thay đổi mã phím",
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

    Column(
        Modifier
            .width(500.dp)
            .clip(PanelShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
            .ignoreHeldSelectKey(),
    ) {
        DialogHeader(
            title = "Nguồn ứng dụng Tiếp tục xem",
            subtitle = "Bật hoặc tắt hiển thị nội dung từ từng ứng dụng",
        )
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 380.dp),
        ) {
            itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { index, app ->
                val enabled = app.packageName !in disabledWatchPackages
                val hasActiveContent = app.packageName in activeWatchPackages
                ListItem(
                    selected = false,
                    onClick = { onToggleWatchPackage(app.packageName, !enabled) },
                    modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                    leadingContent = {
                        Image(
                            app.icon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    },
                    headlineContent = {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            when {
                                !enabled -> "Đã tắt hiển thị"
                                hasActiveContent -> "Đang có nội dung trong Tiếp tục xem"
                                else -> "Cho phép hiển thị"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ToolkitColors.Background,
                                checkedTrackColor = ToolkitColors.Accent,
                            ),
                        )
                    },
                    shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
                    scale = ListItemDefaults.scale(focusedScale = 1.02f),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = ToolkitColors.OnSurface.copy(alpha = 0.7f),
                        focusedContainerColor = ToolkitColors.OnSurface.copy(alpha = 0.12f),
                        focusedContentColor = ToolkitColors.OnSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(ToolkitColors.Accent.copy(alpha = 0.24f), ToolkitColors.Accent.copy(alpha = 0.05f))
                )
            )
            .drawBehind {
                drawLine(
                    Color.White.copy(alpha = 0.1f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 28.dp, vertical = 22.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = ToolkitColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = ToolkitColors.OnSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
