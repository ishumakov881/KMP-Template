package online.lds.nos.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.lds.online.network.nos.LeanMapLine
import net.lds.online.network.nos.LeanMapMarker

/**
 * Platform map surface. Android: osmdroid MapView. Other targets: stub.
 */
@Composable
expect fun NosMapCanvas(
    markers: List<LeanMapMarker>,
    lines: List<LeanMapLine> = emptyList(),
    modifier: Modifier = Modifier,
    onMarkerClick: (LeanMapMarker) -> Unit = {},
)
