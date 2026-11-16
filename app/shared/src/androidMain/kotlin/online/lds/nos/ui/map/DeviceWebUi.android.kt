package online.lds.nos.ui.map

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberDeviceWebUi(): DeviceWebUi {
    val context = LocalContext.current
    return remember(context) {
        object : DeviceWebUi {
            override fun openHttp(ip: String) {
                val url = httpUrlForDeviceIp(ip) ?: return
                val uri = Uri.parse(url)
                try {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(context, uri)
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }
        }
    }
}
