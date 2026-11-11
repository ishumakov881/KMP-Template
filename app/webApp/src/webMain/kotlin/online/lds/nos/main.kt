package online.lds.nos

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(markerOfflineStore = net.lds.online.network.nos.NoOpMarkerOfflineStore)
    }
}