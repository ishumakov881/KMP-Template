package dev.walhalla.lazagne

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.russhwolf.settings.SharedPreferencesSettings
import dev.walhalla.lazagne.data.AppPreferences
import dev.walhalla.lazagne.ui.ThemeSettings
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import net.lds.online.network.nos.NosAuthRepository
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.createNosHttpClient

class MainActivity : ComponentActivity() {
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

        setContent {
            App(
                themeSettings = themeSettings,
                appPreferences = appPreferences,
                authRepository = authRepository,
                mapRepository = mapRepository,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
