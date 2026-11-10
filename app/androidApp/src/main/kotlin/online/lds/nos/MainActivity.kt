package online.lds.nos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.russhwolf.settings.SharedPreferencesSettings
import online.lds.nos.data.AppPreferences
import online.lds.nos.ui.ThemeSettings
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import net.lds.online.network.nos.NosAuthRepository
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.createNosHttpClient

class MainActivity : ComponentActivity() {
    private var lastBackPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = SharedPreferencesSettings(
            getSharedPreferences("nos", MODE_PRIVATE),
        )
        val themeSettings = ThemeSettings(prefs)
        val appPreferences = AppPreferences(prefs)
        val cookies = AcceptAllCookiesStorage()
        val client = createNosHttpClient(cookies = cookies)
        val authRepository = NosAuthRepository(cookies, client)
        val mapRepository = NosMapRepository(client)
        val swRepository = NosSwRepository(client)
        val exitToast = Toast.makeText(
            this,
            getString(R.string.press_again_to_exit),
            Toast.LENGTH_SHORT,
        )

        setContent {
            // ldsonline: when Compose tab/overlay handlers are disabled, double-back exits.
            BackHandler(true) {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedTime < 2000L) {
                    exitToast.cancel()
                    finish()
                } else {
                    lastBackPressedTime = now
                    exitToast.show()
                }
            }

            App(
                themeSettings = themeSettings,
                appPreferences = appPreferences,
                authRepository = authRepository,
                mapRepository = mapRepository,
                swRepository = swRepository,
                // Same CookieJar as API — Coil loads `/inc/other/rrd/{ip}/nolegend.png` (empty graph PNG ok).
                httpClient = client,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
