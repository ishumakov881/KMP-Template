package online.lds.nos.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler

@Composable
actual fun rememberDeviceWebUi(): DeviceWebUi {
    val uriHandler = LocalUriHandler.current
    return remember(uriHandler) {
        object : DeviceWebUi {
            override fun openHttp(ip: String) {
                val url = httpUrlForDeviceIp(ip) ?: return
                runCatching { uriHandler.openUri(url) }
            }
        }
    }
}
