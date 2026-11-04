package com.tk.toolkit.tv.launcher.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Input
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class ClockText(val time: String, val meridiem: String?, val weekday: String, val date: String)

private enum class NetworkType(val label: String, val icon: ImageVector) {
    Ethernet("Mạng có dây", Icons.Outlined.SettingsEthernet),
    Wifi("Wi-Fi", Icons.Outlined.Wifi),
    Disconnected("Không có mạng", Icons.Outlined.WifiOff),
}

@Composable
fun ClockHeader(
    onOpenNetwork: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showInputsButton: Boolean = false,
    onOpenInputs: () -> Unit = {},
) {
    val context = LocalContext.current
    val clock by produceState(clockText(DateFormat.is24HourFormat(context))) {
        while (true) {
            value = clockText(DateFormat.is24HourFormat(context))
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val networkType = rememberNetworkType(context)

    // One compact line, so the rows below get the height.
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                clock.time,
                style = MaterialTheme.typography.displaySmall.copy(
                    brush = Brush.verticalGradient(listOf(ToolkitColors.OnSurface, ToolkitColors.Accent)),
                ),
            )
            clock.meridiem?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                )
            }
        }
        Spacer(
            Modifier
                .padding(horizontal = 20.dp)
                .size(width = 1.dp, height = 32.dp)
                .background(ToolkitColors.OnSurface.copy(alpha = 0.15f)),
        )
        Column {
            Text(clock.weekday.uppercase(), style = MaterialTheme.typography.labelLarge, color = ToolkitColors.Accent)
            Text(clock.date, style = MaterialTheme.typography.bodyLarge, color = ToolkitColors.OnSurfaceMuted)
        }
        Spacer(Modifier.weight(1f))
        if (showInputsButton) {
            HeaderButton(Icons.Outlined.Input, "Ngõ vào", onOpenInputs)
            Spacer(Modifier.width(12.dp))
        }
        HeaderButton(networkType.icon, networkType.label, onOpenNetwork)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.Speaker, "Đầu ra âm thanh", onOpenAudioOutput)
        Spacer(Modifier.width(12.dp))
        HeaderButton(Icons.Outlined.Settings, "Cài đặt", onOpenSettings)
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val tooltipAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "tooltipAlpha")
    val tooltipShape = RoundedCornerShape(8.dp)

    Box(contentAlignment = Alignment.TopCenter) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .onFocusChanged { focused = it.isFocused },
            colors = IconButtonDefaults.colors(
                containerColor = ToolkitColors.OnSurface.copy(alpha = 0.06f),
                contentColor = ToolkitColors.OnSurfaceMuted,
                focusedContainerColor = ToolkitColors.OnSurface,
                focusedContentColor = ToolkitColors.Background,
            ),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }

        // Floating tooltip placed below the button without affecting ClockHeader's layout size.
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ToolkitColors.OnSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints())
                    val gapPx = 8.dp.roundToPx()
                    val buttonSizePx = 44.dp.roundToPx()
                    layout(0, 0) {
                        placeable.place(
                            x = -placeable.width / 2,
                            y = buttonSizePx + gapPx,
                        )
                    }
                }
                .alpha(tooltipAlpha)
                .background(ToolkitColors.SurfaceRaised.copy(alpha = 0.96f), tooltipShape)
                .border(1.dp, ToolkitColors.OnSurface.copy(alpha = 0.14f), tooltipShape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun rememberNetworkType(context: Context): NetworkType {
    var networkType by remember { mutableStateOf(detectNetworkType(context)) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@DisposableEffect onDispose { }
        val update = { networkType = detectNetworkType(context) }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = update()
            override fun onLost(network: Network) = update()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = update()
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        update()
        onDispose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }
    return networkType
}

@Suppress("DEPRECATION")
private fun detectNetworkType(context: Context): NetworkType {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return NetworkType.Disconnected
    val activeCaps = cm.activeNetwork?.let { runCatching { cm.getNetworkCapabilities(it) }.getOrNull() }
    if (activeCaps != null) {
        if (activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return NetworkType.Ethernet
        if (activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return NetworkType.Wifi
    }
    // Check underlying networks (e.g. when a VPN is active on top of Ethernet or Wi-Fi).
    val allCaps = runCatching { cm.allNetworks.mapNotNull { cm.getNetworkCapabilities(it) } }.getOrDefault(emptyList())
    if (allCaps.any { it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) }) return NetworkType.Ethernet
    if (allCaps.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }) return NetworkType.Wifi
    return NetworkType.Disconnected
}

private fun clockText(is24Hour: Boolean): ClockText {
    val now = LocalDateTime.now()
    return ClockText(
        time = now.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm")),
        meridiem = if (is24Hour) null else now.format(DateTimeFormatter.ofPattern("a")).uppercase(),
        weekday = now.format(DateTimeFormatter.ofPattern("EEEE")),
        date = now.format(DateTimeFormatter.ofPattern("d MMMM")),
    )
}
