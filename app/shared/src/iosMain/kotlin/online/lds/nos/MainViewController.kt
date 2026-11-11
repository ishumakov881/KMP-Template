package online.lds.nos

import androidx.compose.ui.window.ComposeUIViewController
import net.lds.online.db.createMarkerOfflineStore

fun MainViewController() = ComposeUIViewController {
    App(markerOfflineStore = createMarkerOfflineStore())
}