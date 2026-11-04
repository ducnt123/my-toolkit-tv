package com.tk.toolkit.tv.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.launcher.data.WatchItem
import com.tk.toolkit.tv.ui.theme.ToolkitColors

private val TileShape = RoundedCornerShape(14.dp)

/** Continue Watching / Play Next entry, with optional playback progress bar and title caption. */
@Composable
fun WatchTile(
    item: WatchItem,
    index: Int,
    columns: Int,
    isMoving: Boolean,
    dimmed: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveBy: (Int) -> Unit,
    onDrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    val tileAlpha by animateFloatAsState(if (dimmed) 0.35f else 1f, label = "dim")
    val glowAlpha by animateFloatAsState(if (focused && !dimmed) 1f else 0f, label = "glow")
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "label")

    // Keep the tile being moved focused and on screen as it changes position.
    LaunchedEffect(isMoving, index) {
        if (isMoving) {
            runCatching { focusRequester.requestFocus() }
            bringIntoView.bringIntoView()
        }
    }

    Column(
        modifier
            .alpha(tileAlpha)
            .moveKeys(isMoving, columns, onMoveBy, onDrop),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.accentGlow(item.glow, glowAlpha)) {
            Card(
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .bringIntoViewRequester(bringIntoView)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onFocused()
                    },
                shape = CardDefaults.shape(TileShape),
                scale = CardDefaults.scale(focusedScale = if (isMoving) 1.14f else 1.08f),
                border = CardDefaults.border(focusedBorder = if (isMoving) movingBorder() else Border.None),
                colors = CardDefaults.colors(containerColor = ToolkitColors.Surface),
            ) {
                Box(Modifier.fillMaxSize()) {
                    when {
                        item.artworkBitmap != null -> {
                            Image(
                                item.artworkBitmap,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        item.artwork != null && item.caption == null -> {
                            Image(
                                painterResource(item.artwork),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        item.artwork != null -> {
                            LogoWithCaption(item, item.artwork)
                        }
                        else -> {
                            FallbackWatchContent(item)
                        }
                    }
                    if (item.progress != null && item.progress > 0f) {
                        PlaybackProgressBar(
                            progress = item.progress,
                            color = item.glow,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                    if (isMoving) MoveOverlay()
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .alpha(labelAlpha),
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = ToolkitColors.OnSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                item.appName,
                style = MaterialTheme.typography.bodySmall,
                color = ToolkitColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun PlaybackProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(5.dp)
            .background(Color.Black.copy(alpha = 0.55f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .background(color),
        )
    }
}

/**
 * Logo-only artwork (logo on black) scaled down on a black tile, with the caption set under
 * the wordmark like a subtitle.
 */
@Composable
private fun LogoWithCaption(item: WatchItem, artworkRes: Int) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painterResource(artworkRes),
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.Center)
                .offset(y = (-8).dp),
        )
        Text(
            item.caption.orEmpty().uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, letterSpacing = 3.sp),
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun FallbackWatchContent(item: WatchItem) {
    val base = lerp(item.glow, ToolkitColors.Surface, 0.55f)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.55f))))
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                color = ToolkitColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.appName.isNotBlank()) {
                Text(
                    item.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = ToolkitColors.OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
