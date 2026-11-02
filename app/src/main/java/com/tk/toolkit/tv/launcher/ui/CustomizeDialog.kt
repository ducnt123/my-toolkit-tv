package com.tk.toolkit.tv.launcher.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

@Composable
fun CustomizeDialog(
    showWatchRow: Boolean,
    apps: List<LauncherApp>,
    disabledWatchPackages: Set<String>,
    activeWatchPackages: Set<String>,
    onToggleWatchRow: (Boolean) -> Unit,
    onToggleWatchPackage: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var managingSources by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (managingSources) managingSources = false else onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        if (!managingSources) {
            MainCustomizePanel(
                showWatchRow = showWatchRow,
                enabledCount = apps.count { it.packageName !in disabledWatchPackages },
                totalCount = apps.size,
                onToggleWatchRow = onToggleWatchRow,
                onOpenSources = { managingSources = true },
                onRefresh = onRefresh,
            )
        } else {
            WatchSourcesPanel(
                apps = apps,
                disabledWatchPackages = disabledWatchPackages,
                activeWatchPackages = activeWatchPackages,
                onToggleWatchPackage = onToggleWatchPackage,
                onBack = { managingSources = false },
            )
        }
    }
}

@Composable
private fun MainCustomizePanel(
    showWatchRow: Boolean,
    enabledCount: Int,
    totalCount: Int,
    onToggleWatchRow: (Boolean) -> Unit,
    onOpenSources: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstAction.requestFocusWhenReady() }

    Column(
        Modifier
            .width(460.dp)
            .clip(PanelShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
            .ignoreHeldSelectKey(),
    ) {
        DialogHeader(
            title = "Tùy chỉnh",
            subtitle = "Cài đặt màn hình chính và mục Tiếp tục xem",
        )
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ListItem(
                selected = false,
                onClick = { onToggleWatchRow(!showWatchRow) },
                modifier = Modifier.focusRequester(firstAction),
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

