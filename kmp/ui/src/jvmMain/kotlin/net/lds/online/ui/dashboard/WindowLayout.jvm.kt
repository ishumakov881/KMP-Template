package net.lds.online.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberWindowWidth(): WindowWidth {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val widthDp = with(density) { containerSize.width.toDp() }
    return remember(widthDp) {
        when {
            widthDp < 600.dp -> WindowWidth.Compact
            widthDp < 840.dp -> WindowWidth.Medium
            else -> WindowWidth.Expanded
        }
    }
}
