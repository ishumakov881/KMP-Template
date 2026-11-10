package dev.walhalla.lazagne.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoText
import net.lds.online.network.nos.LeanMapMarker

@Composable
actual fun NosMapCanvas(
    markers: List<LeanMapMarker>,
    modifier: Modifier,
    onMarkerClick: (LeanMapMarker) -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = onMarkerClick
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CupertinoText("Карта доступна на Android (${markers.size})")
    }
}
