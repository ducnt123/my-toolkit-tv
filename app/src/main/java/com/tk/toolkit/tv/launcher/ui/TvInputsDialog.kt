package com.tk.toolkit.tv.launcher.ui

import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Input
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.SettingsInputComponent
import androidx.compose.material.icons.outlined.SettingsInputComposite
import androidx.compose.material.icons.outlined.SettingsInputHdmi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.TvInputEntry
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val PanelShape = RoundedCornerShape(24.dp)

@Composable
fun TvInputsDialog(
    inputs: List<TvInputEntry>,
    onSelectInput: (TvInputEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(inputs.isNotEmpty()) {
        if (inputs.isNotEmpty()) firstItem.requestFocusWhenReady()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .width(460.dp)
                .clip(PanelShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.08f), PanelShape)
                .ignoreHeldSelectKey(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ToolkitColors.Accent.copy(alpha = 0.24f), ToolkitColors.Accent.copy(alpha = 0.05f)),
                        ),
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
                    "Ngõ vào",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ToolkitColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Chọn nguồn truyền hình hoặc cổng kết nối trên tivi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 380.dp),
            ) {
                itemsIndexed(inputs, key = { _, item -> item.id }) { index, input ->
                    val connected = input.isLiveTv || input.state == TvInputManager.INPUT_STATE_CONNECTED
                    ListItem(
                        selected = false,
                        onClick = { onSelectInput(input) },
                        modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                        leadingContent = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ToolkitColors.OnSurface.copy(alpha = 0.08f)),
                            ) {
                                if (input.icon != null) {
                                    Image(
                                        input.icon,
                                        contentDescription = input.displayTitle,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    Icon(
                                        inputIcon(input),
                                        contentDescription = input.displayTitle,
                                        tint = if (connected) ToolkitColors.Accent else ToolkitColors.OnSurfaceMuted,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        },
                        headlineContent = {
                            Text(
                                input.displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text(
                                input.stateLabel,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        trailingContent = {
                            if (connected) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(ToolkitColors.Accent, CircleShape),
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
            }
        }
    }
}

private fun inputIcon(input: TvInputEntry): ImageVector = when {
    input.isLiveTv -> Icons.Outlined.LiveTv
    input.type == TvInputInfo.TYPE_HDMI -> Icons.Outlined.SettingsInputHdmi
    input.type == TvInputInfo.TYPE_COMPONENT -> Icons.Outlined.SettingsInputComponent
    input.type == TvInputInfo.TYPE_COMPOSITE -> Icons.Outlined.SettingsInputComposite
    else -> Icons.Outlined.Input
}

