package com.tk.toolkit.tv.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.launcher.data.LauncherApp
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlin.math.roundToInt

private val MenuShape = RoundedCornerShape(12.dp)
private val MenuItemShape = RoundedCornerShape(8.dp)
private val MENU_WIDTH = 250.dp
private val ARROW_WIDTH = 14.dp
private val ARROW_HEIGHT = 7.dp
private val ANCHOR_GAP = 4.dp
private val SAFE_MARGIN_X = 56.dp
private val SAFE_MARGIN_Y = 24.dp

/**
 * App actions as a compact dropdown, like the stock Android TV launcher's long-press menu.
 *
 * When [anchorBounds] (window coordinates of the long-pressed tile) is known, the menu drops
 * down directly below or pops up directly above the app banner depending on available screen space,
 * with an arrow pointing towards it, never overlapping the banner itself.
 * Without an anchor (e.g. opened from the hidden apps popup) it is shown centered.
 */
@Composable
fun AppActionsDialog(
    app: LauncherApp,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onToggleHidden: () -> Unit,
    onAppInfo: () -> Unit,
    onForceStop: () -> Unit,
    onUninstall: () -> Unit,
    anchorBounds: Rect? = null,
    safeMarginX: Dp = SAFE_MARGIN_X,
    safeMarginY: Dp = SAFE_MARGIN_Y,
) {
    if (anchorBounds == null) {
        Dialog(onDismissRequest = onDismiss) {
            LocaleHelper.ProvideAppLocale {
                AppActionsMenu(
                    app = app,
                    isHidden = isHidden,
                    arrowTop = null,
                    arrowX = 0,
                    maxActionsHeight = 360.dp,
                    onMove = onMove,
                    onToggleHidden = onToggleHidden,
                    onAppInfo = onAppInfo,
                    onForceStop = onForceStop,
                    onUninstall = onUninstall,
                )
            }
        }
        return
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val gapPx = with(density) { ANCHOR_GAP.roundToPx() }
    val marginXPx = with(density) { safeMarginX.roundToPx() }
    val marginYPx = with(density) { safeMarginY.roundToPx() }

    val spaceAbovePx = (anchorBounds.top.roundToInt() - marginYPx - gapPx).coerceAtLeast(0)
    val spaceBelowPx = (screenHeightPx - anchorBounds.bottom.roundToInt() - marginYPx - gapPx).coerceAtLeast(0)

    // Full menu is ~240dp. If below doesn't have 240dp but above has more, prefer above.
    val estimatedHeightPx = with(density) { 240.dp.roundToPx() }
    val preferAbove = when {
        spaceBelowPx >= estimatedHeightPx -> false
        spaceAbovePx >= estimatedHeightPx -> true
        else -> spaceAbovePx > spaceBelowPx
    }

    var placedAbove by remember(anchorBounds) { mutableStateOf(preferAbove) }
    var arrowX by remember(anchorBounds) { mutableIntStateOf(with(density) { (MENU_WIDTH / 2).roundToPx() }) }

    val availableHeightDp = with(density) {
        val availablePx = if (placedAbove) spaceAbovePx else spaceBelowPx
        (availablePx.toDp() - 50.dp).coerceAtLeast(120.dp)
    }

    val positionProvider = remember(anchorBounds, density, preferAbove, marginXPx, marginYPx) {
        DropdownPositionProvider(
            anchor = anchorBounds,
            gapPx = gapPx,
            marginXPx = marginXPx,
            marginYPx = marginYPx,
            preferAbove = preferAbove,
            onPlacementChanged = { above, aX ->
                placedAbove = above
                arrowX = aX
            },
        )
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        LocaleHelper.ProvideAppLocale {
            AppActionsMenu(
                app = app,
                isHidden = isHidden,
                arrowTop = if (placedAbove) false else true,
                arrowX = arrowX,
                maxActionsHeight = availableHeightDp,
                onMove = onMove,
                onToggleHidden = onToggleHidden,
                onAppInfo = onAppInfo,
                onForceStop = onForceStop,
                onUninstall = onUninstall,
            )
        }
    }
}

/**
 * Positions the menu strictly outside the anchor (immediately above or below),
 * never overlapping the banner in the Y-axis.
 */
private class DropdownPositionProvider(
    private val anchor: Rect,
    private val gapPx: Int,
    private val marginXPx: Int,
    private val marginYPx: Int,
    private val preferAbove: Boolean,
    private val onPlacementChanged: (placedAbove: Boolean, arrowX: Int) -> Unit,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val centerX = anchor.center.x.roundToInt()
        val maxX = (windowSize.width - popupContentSize.width - marginXPx).coerceAtLeast(marginXPx)
        val x = (centerX - popupContentSize.width / 2).coerceIn(marginXPx, maxX)

        val spaceBelow = windowSize.height - anchor.bottom.roundToInt() - marginYPx
        val spaceAbove = anchor.top.roundToInt() - marginYPx

        val useAbove = when {
            preferAbove && (spaceAbove >= popupContentSize.height || spaceAbove >= spaceBelow) -> true
            !preferAbove && (spaceBelow >= popupContentSize.height || spaceBelow >= spaceAbove) -> false
            spaceAbove >= popupContentSize.height -> true
            spaceBelow >= popupContentSize.height -> false
            else -> spaceAbove > spaceBelow
        }

        val y = if (useAbove) {
            // Place strictly ABOVE the anchor. Bottom aligns to (anchor.top - gapPx).
            (anchor.top.roundToInt() - gapPx - popupContentSize.height).coerceAtLeast(marginYPx)
        } else {
            // Place strictly BELOW the anchor. Top aligns to (anchor.bottom + gapPx).
            anchor.bottom.roundToInt() + gapPx
        }

        val minArrow = 16
        val maxArrow = (popupContentSize.width - 16).coerceAtLeast(minArrow)
        val clampedArrowX = (centerX - x).coerceIn(minArrow, maxArrow)

        onPlacementChanged(useAbove, clampedArrowX)

        return IntOffset(x, y)
    }
}

@Composable
private fun AppActionsMenu(
    app: LauncherApp,
    isHidden: Boolean,
    arrowTop: Boolean?,
    arrowX: Int,
    maxActionsHeight: Dp,
    onMove: () -> Unit,
    onToggleHidden: () -> Unit,
    onAppInfo: () -> Unit,
    onForceStop: () -> Unit,
    onUninstall: () -> Unit,
) {
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstAction.requestFocusWhenReady() }

    Column(Modifier.width(MENU_WIDTH)) {
        if (arrowTop == true) MenuArrow(pointsUp = true, arrowX = arrowX)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MenuShape)
                .background(ToolkitColors.SurfaceRaised)
                .border(1.dp, Color.White.copy(alpha = 0.1f), MenuShape)
                .ignoreHeldSelectKey(),
        ) {
            MenuHeader(app)
            Column(
                Modifier
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .heightIn(max = maxActionsHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (isHidden) {
                    DropdownActionItem(
                        icon = Icons.Outlined.Visibility,
                        title = stringResource(R.string.app_action_unhide),
                        onClick = onToggleHidden,
                        modifier = Modifier.focusRequester(firstAction),
                    )
                } else {
                    DropdownActionItem(
                        icon = Icons.Outlined.OpenWith,
                        title = stringResource(R.string.app_action_move),
                        onClick = onMove,
                        modifier = Modifier.focusRequester(firstAction),
                    )
                    DropdownActionItem(
                        icon = Icons.Outlined.VisibilityOff,
                        title = stringResource(R.string.app_action_hide),
                        onClick = onToggleHidden,
                    )
                }
                DropdownActionItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.app_action_info),
                    onClick = onAppInfo,
                )
                DropdownActionItem(
                    icon = Icons.Outlined.StopCircle,
                    title = stringResource(R.string.app_action_force_stop),
                    onClick = onForceStop,
                )
                if (app.canUninstall) {
                    DropdownActionItem(
                        icon = if (app.isUpdatedSystemApp) Icons.Outlined.Restore else Icons.Outlined.DeleteOutline,
                        title = if (app.isUpdatedSystemApp) stringResource(R.string.app_action_uninstall_updates)
                        else stringResource(R.string.app_action_uninstall),
                        onClick = onUninstall,
                        danger = true,
                    )
                }
            }
        }
        if (arrowTop == false) MenuArrow(pointsUp = false, arrowX = arrowX)
    }
}

/** Small triangle linking the menu to the tile it belongs to. */
@Composable
private fun MenuArrow(pointsUp: Boolean, arrowX: Int) {
    val density = LocalDensity.current
    val arrowWidthPx = with(density) { ARROW_WIDTH.toPx() }
    val inset = with(density) { 14.dp.toPx() }
    val color = ToolkitColors.SurfaceRaised
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(ARROW_HEIGHT),
    ) {
        val center = arrowX.toFloat().coerceIn(inset, size.width - inset)
        val half = arrowWidthPx / 2f
        val path = Path().apply {
            if (pointsUp) {
                moveTo(center - half, size.height)
                lineTo(center, 0f)
                lineTo(center + half, size.height)
            } else {
                moveTo(center - half, 0f)
                lineTo(center, size.height)
                lineTo(center + half, 0f)
            }
            close()
        }
        drawPath(path, color)
    }
}

/** App name with its package name small and muted underneath. */
@Composable
private fun MenuHeader(app: LauncherApp) {
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    Color.White.copy(alpha = 0.08f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            app.label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = ToolkitColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            app.packageName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = ToolkitColors.OnSurfaceMuted.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DropdownActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val tint = if (danger) ToolkitColors.Danger else ToolkitColors.OnSurface
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(MenuItemShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = tint.copy(alpha = 0.88f),
            focusedContainerColor = if (danger) tint.copy(alpha = 0.16f) else ToolkitColors.OnSurface,
            focusedContentColor = if (danger) tint else ToolkitColors.Background,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val tint = if (danger) ToolkitColors.Danger else ToolkitColors.OnSurface
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium) },
        shape = ListItemDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ListItemDefaults.scale(focusedScale = 1.02f),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = tint.copy(alpha = 0.7f),
            focusedContainerColor = tint.copy(alpha = 0.12f),
            focusedContentColor = tint,
        ),
    )
}
