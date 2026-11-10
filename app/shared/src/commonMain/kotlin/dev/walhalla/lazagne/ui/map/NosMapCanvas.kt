package dev.walhalla.lazagne.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.lds.online.network.nos.LeanMapMarker

/**
 * Platform map surface. Android: osmdroid MapView. Other targets: stub.
 */
@Composable
expect fun NosMapCanvas(
    markers: List<LeanMapMarker>,
    modifier: Modifier = Modifier,
)
