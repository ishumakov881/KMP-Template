package online.lds.nos.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

/**
 * Opens the default browser. Desktop.browse first; OS-specific fallback if AWT browse is broken
 * (common on some Windows JDK setups).
 */
@Composable
actual fun rememberDeviceWebUi(): DeviceWebUi {
    return remember {
        object : DeviceWebUi {
            override fun openHttp(ip: String) {
                val url = httpUrlForDeviceIp(ip) ?: return
                if (browseWithDesktop(url)) return
                browseWithOsCommand(url)
            }
        }
    }
}

private fun browseWithDesktop(url: String): Boolean =
    runCatching {
        if (!Desktop.isDesktopSupported()) return false
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return false
        desktop.browse(URI(url))
        true
    }.getOrDefault(false)

private fun browseWithOsCommand(url: String) {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val command = when {
        os.contains("win") -> arrayOf("rundll32", "url.dll,FileProtocolHandler", url)
        os.contains("mac") -> arrayOf("open", url)
        else -> arrayOf("xdg-open", url)
    }
    runCatching {
        ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
    }
}
