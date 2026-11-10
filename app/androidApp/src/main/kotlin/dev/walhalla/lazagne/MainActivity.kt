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
import net.lds.online.network.nos.NosAuthRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = SharedPreferencesSettings(
            getSharedPreferences("nos", MODE_PRIVATE),
        )
        val themeSettings = ThemeSettings(prefs)
        val appPreferences = AppPreferences(prefs)
        val authRepository = NosAuthRepository()

        setContent {
            App(
                themeSettings = themeSettings,
                appPreferences = appPreferences,
                authRepository = authRepository,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
