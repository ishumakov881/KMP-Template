package dev.walhalla.lazagne

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.walhalla.lazagne.data.AppPreferences
import dev.walhalla.lazagne.data.InMemorySettings
import dev.walhalla.lazagne.ui.CorporateShell
import dev.walhalla.lazagne.ui.ThemeMode
import dev.walhalla.lazagne.ui.ThemePrefs
import dev.walhalla.lazagne.ui.ThemeSettings
import dev.walhalla.lazagne.ui.auth.LoginRoute
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.darkColorScheme
import io.github.alexzhirkevich.cupertino.theme.lightColorScheme
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import net.lds.online.network.nos.NosAuthRepository
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.createNosHttpClient

private sealed interface AuthGate {
    data object Checking : AuthGate
    data object NeedLogin : AuthGate
    data object Authenticated : AuthGate
}

@Composable
@Preview
fun App(
    themeSettings: ThemeSettings? = null,
    appPreferences: AppPreferences? = null,
    authRepository: NosAuthRepository? = null,
    mapRepository: NosMapRepository? = null,
) {
    var sessionPrefs by remember { mutableStateOf(ThemePrefs()) }
    val prefs = themeSettings?.prefs ?: sessionPrefs

    val resolvedPrefs = remember(appPreferences) { appPreferences }
    val (resolvedAuth, resolvedMap) = remember(authRepository, mapRepository) {
        if (authRepository != null && mapRepository != null) {
            authRepository to mapRepository
        } else {
            val cookies = AcceptAllCookiesStorage()
            val client = createNosHttpClient(cookies = cookies)
            NosAuthRepository(cookies, client) to NosMapRepository(client)
        }
    }

    var gate by remember {
        mutableStateOf<AuthGate>(
            if (resolvedPrefs == null) AuthGate.NeedLogin else AuthGate.Checking,
        )
    }

    LaunchedEffect(resolvedPrefs, resolvedAuth) {
        val appPrefs = resolvedPrefs
        if (appPrefs == null) {
            gate = AuthGate.NeedLogin
            return@LaunchedEffect
        }
        val saved = appPrefs.getStringOrNull(AppPreferences.KEY_PHPSESSID)
        if (saved == null) {
            gate = AuthGate.NeedLogin
            return@LaunchedEffect
        }
        gate = AuthGate.Checking
        val ok = runCatching { resolvedAuth.restoreSession(saved) }.getOrDefault(false)
        if (ok) {
            gate = AuthGate.Authenticated
        } else {
            appPrefs.remove(AppPreferences.KEY_PHPSESSID)
            gate = AuthGate.NeedLogin
        }
    }

    val dark = when (prefs.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val accent = prefs.accent.color(dark)
    val colorScheme = if (dark) darkColorScheme(accent = accent) else lightColorScheme(accent = accent)

    CupertinoTheme(colorScheme = colorScheme) {
        when (gate) {
            AuthGate.Checking -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoActivityIndicator()
            }

            AuthGate.NeedLogin -> {
                val prefsForLogin = resolvedPrefs
                    ?: remember { AppPreferences(InMemorySettings()) }
                LoginRoute(
                    onAuthenticated = { gate = AuthGate.Authenticated },
                    authRepository = resolvedAuth,
                    appPreferences = prefsForLogin,
                )
            }

            AuthGate.Authenticated -> CorporateShell(
                themePrefs = prefs,
                isDark = dark,
                onThemePrefsChange = { transform ->
                    if (themeSettings != null) themeSettings.update(transform)
                    else sessionPrefs = transform(sessionPrefs)
                },
                mapRepository = resolvedMap,
            )
        }
    }
}
