package online.lds.nos

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.lds.online.db.createMarkerOfflineStore

fun main() = application {
    val markerOfflineStore = createMarkerOfflineStore()
    Window(
        onCloseRequest = ::exitApplication,
        title = "NOS",
    ) {
        App(markerOfflineStore = markerOfflineStore)
    }
}