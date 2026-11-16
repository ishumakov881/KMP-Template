package online.lds.nos.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * In-app Safari sheet (parity with Android Custom Tabs); fallback → system Safari.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDeviceWebUi(): DeviceWebUi {
    return remember {
        object : DeviceWebUi {
            override fun openHttp(ip: String) {
                val url = httpUrlForDeviceIp(ip) ?: return
                val nsUrl = NSURL.URLWithString(url) ?: return
                val presenter = topViewController()
                if (presenter != null) {
                    presenter.presentViewController(
                        SFSafariViewController(uRL = nsUrl),
                        animated = true,
                        completion = null,
                    )
                    return
                }
                UIApplication.sharedApplication.openURL(
                    nsUrl,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null,
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun topViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    val window = scenes
        .mapNotNull { it as? UIWindowScene }
        .flatMap { scene -> scene.windows.mapNotNull { it as? UIWindow } }
        .firstOrNull { it.isKeyWindow }
        ?: scenes
            .mapNotNull { it as? UIWindowScene }
            .flatMap { scene -> scene.windows.mapNotNull { it as? UIWindow } }
            .firstOrNull()
    var top = window?.rootViewController ?: return null
    while (true) {
        val presented = top.presentedViewController ?: break
        top = presented
    }
    return top
}
