package dev.walhalla.lazagne

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.russhwolf.settings.SharedPreferencesSettings
import dev.walhalla.lazagne.ui.ThemeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val themeSettings = ThemeSettings(SharedPreferencesSettings(getSharedPreferences("theme", MODE_PRIVATE)))
        setContent {
            App(themeSettings)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}