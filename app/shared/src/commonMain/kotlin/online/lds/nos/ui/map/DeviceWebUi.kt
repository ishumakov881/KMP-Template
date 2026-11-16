package online.lds.nos.ui.map

import androidx.compose.runtime.Composable

/**
 * Opens the device's own HTTP UI (`http://{ip}/`), not NOS pages.
 * - Android: Custom Tabs (fallback ACTION_VIEW)
 * - iOS: SFSafariViewController (fallback system Safari)
 * - Desktop JVM: Desktop.browse / OS open command
 */
interface DeviceWebUi {
    fun openHttp(ip: String)
}

@Composable
expect fun rememberDeviceWebUi(): DeviceWebUi

/** Only dotted IPv4 from markgen — no schemes/paths. */
internal fun httpUrlForDeviceIp(ip: String): String? {
    val trimmed = ip.trim()
    if (!DEVICE_IPV4.matches(trimmed)) return null
    return "http://$trimmed/"
}

private val DEVICE_IPV4 = Regex("""^(?:\d{1,3}\.){3}\d{1,3}$""")
