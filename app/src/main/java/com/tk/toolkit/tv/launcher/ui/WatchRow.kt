package com.tk.toolkit.tv.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.toolkit.tv.launcher.data.WatchItem

/** Tiles fully visible at once in the Continue Watching row; the rest scroll in horizontally. */
private const val VISIBLE_TILES = 5

/**
 * Continue Watching as a single horizontally scrolling row, [VISIBLE_TILES] tiles wide.
 *
 * The row bleeds past its slot by [bleedX] on each side (so tiles scroll to the screen edge
 * instead of being cut at the grid's padding) and by [bleedY] vertically (so the focused tile's
 * scale and glow aren't clipped), while still taking up only the tiles' own height in the grid.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchRow(
    items: List<WatchItem>,
    movingKey: String?,
    spacing: Dp,
    bleedX: Dp,
    requesterFor: (String) -> FocusRequester,
    onFocused: (WatchItem) -> Unit,
    onFocusInRowChanged: (Boolean) -> Unit,
    onClick: (WatchItem) -> Unit,
    onLongClick: (WatchItem) -> Unit,
    onMoveBy: (Int) -> Unit,
    onDrop: () -> Unit,
    modifier: Modifier = Modifier,
    rowState: LazyListState = rememberLazyListState(),
    bleedY: Dp = 32.dp,
) {
    // 0 margin horizontally because contentPadding (bleedX) already leaves room for the focused
    // tile's scale and glow; a positive margin would pull item 0 left by marginPx on focus.
    val rowScrollSpec = remember { minimalScrollSpec(0f) }
    var focusedIndex by remember { mutableIntStateOf(-1) }
    var rowHasFocus by remember { mutableStateOf(false) }

    // Ensure that focusing the first tile always snaps the row cleanly to the left edge aligned
    // with the Apps grid below.
    LaunchedEffect(focusedIndex) {
        if (focusedIndex != 0) return@LaunchedEffect
        repeat(2) {
            withFrameNanos { }
            if (rowState.firstVisibleItemIndex == 0 && rowState.firstVisibleItemScrollOffset == 0) return@LaunchedEffect
            runCatching { rowState.animateScrollToItem(0) }
        }
    }

    // When focus leaves the row, scroll it back to the start so the first item is laid out and
    // ready when focus comes back (HomeScreen sends Up/Down entries to the first item).
    LaunchedEffect(rowHasFocus) {
        if (rowHasFocus) return@LaunchedEffect
        if (rowState.firstVisibleItemIndex != 0 || rowState.firstVisibleItemScrollOffset != 0) {
            runCatching { rowState.scrollToItem(0) }
        }
    }

    BoxWithConstraints(modifier) {
        val tileWidth = (maxWidth - spacing * (VISIBLE_TILES - 1)) / VISIBLE_TILES
        CompositionLocalProvider(LocalBringIntoViewSpec provides rowScrollSpec) {
            LazyRow(
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(horizontal = bleedX, vertical = bleedY),
                modifier = Modifier
                    .onFocusChanged {
                        rowHasFocus = it.hasFocus
                        onFocusInRowChanged(it.hasFocus)
                        if (!it.hasFocus) focusedIndex = -1
                    }
                    .layout { measurable, constraints ->
                        val extraX = bleedX.roundToPx()
                        val extraY = bleedY.roundToPx()
                        val width = constraints.maxWidth + extraX * 2
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = width, maxWidth = width),
                        )
                        val height = (placeable.height - extraY * 2).coerceAtLeast(0)
                        layout(constraints.maxWidth, height) { placeable.place(-extraX, -extraY) }
                    },
            ) {
                itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                    val isMoving = item.key == movingKey
                    WatchTile(
                        item = item,
                        index = index,
                        // A single row: only left/right move a tile.
                        columns = 0,
                        isMoving = isMoving,
                        dimmed = movingKey != null && !isMoving,
                        focusRequester = requesterFor(item.key),
                        onFocused = {
                            focusedIndex = index
                            onFocused(item)
                        },
                        onClick = { onClick(item) },
                        onLongClick = { onLongClick(item) },
                        onMoveBy = onMoveBy,
                        onDrop = onDrop,
                        modifier = Modifier
                            .width(tileWidth)
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = if (movingKey != null) spring(stiffness = Spring.StiffnessMediumLow) else null,
                            ),
                    )
                }
            }
        }
    }
}
