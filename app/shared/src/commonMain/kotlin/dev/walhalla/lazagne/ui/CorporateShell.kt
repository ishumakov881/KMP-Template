package dev.walhalla.lazagne.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.composefluent.background.Mica
import io.github.composefluent.component.Badge
import io.github.composefluent.component.BadgeStatus
import io.github.composefluent.component.BreadcrumbBar
import io.github.composefluent.component.BreadcrumbBarItem
import io.github.composefluent.component.Icon
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarDefaults
import io.github.composefluent.component.InfoBarSeverity
import io.github.composefluent.component.NavigationDisplayMode
import io.github.composefluent.component.NavigationView
import io.github.composefluent.component.Text
import io.github.composefluent.component.menuItem
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Document
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Person
import io.github.composefluent.icons.regular.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val AppName = "Walhalla Corp"

enum class MainTab(val title: String, val icon: ImageVector) {
    Desktop("Рабочий стол", Icons.Regular.Home),
    Requests("Заявки", Icons.Regular.Document),
    Profile("Профиль", Icons.Regular.Person),
}

sealed interface Overlay {
    data class RequestForm(val request: CorpRequest?, val type: RequestType) : Overlay
    data object Settings : Overlay
}

@Stable
class RequestsState {
    var folder by mutableStateOf(RequestFolder.Inbox)
    var query by mutableStateOf("")
    var refreshing by mutableStateOf(false)
        private set
    var items by mutableStateOf(DemoData.requests())
        private set
    private var seed = 0

    fun refresh(scope: CoroutineScope) {
        if (refreshing) return
        scope.launch {
            refreshing = true
            delay(1200)
            items = DemoData.requests(++seed)
            refreshing = false
        }
    }
}

@Composable
fun CorporateShell(
    themePrefs: ThemePrefs,
    isDark: Boolean,
    onThemePrefsChange: ((ThemePrefs) -> ThemePrefs) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Desktop) }
    var stack by remember { mutableStateOf(listOf<Overlay>()) }
    var desktopLoaded by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val requestsState = remember { RequestsState() }

    val top = stack.lastOrNull()
    val push: (Overlay) -> Unit = { if (stack.lastOrNull() != it) stack = stack + it }
    val pop: () -> Unit = { stack = stack.dropLast(1) }
    val pending = requestsState.items.count { it.status == RequestStatus.Pending }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(3000)
            notice = null
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = stack.isNotEmpty(),
        onBackCompleted = pop,
    )

    val crumbs = listOf(AppName, tab.title) + stack.map { overlayTitle(it) }

    Mica(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            NavigationView(
                displayMode = if (maxWidth >= 600.dp) NavigationDisplayMode.LeftCompact else NavigationDisplayMode.Top,
                title = { Text(AppName) },
                menuItems = {
                    MainTab.entries.forEach { item ->
                        menuItem(
                            key = item,
                            selected = item == tab && top != Overlay.Settings,
                            onClick = {
                                tab = item
                                stack = emptyList()
                            },
                            text = { Text(item.title) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            badge = if (item == MainTab.Requests && pending > 0) {
                                { Badge(status = BadgeStatus.Attention) { Text("$pending") } }
                            } else {
                                null
                            },
                        )
                    }
                },
                footerItems = {
                    menuItem(
                        selected = top == Overlay.Settings,
                        onClick = { push(Overlay.Settings) },
                        text = { Text("Настройки") },
                        icon = { Icon(Icons.Regular.Settings, contentDescription = "Настройки") },
                    )
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    BreadcrumbBar(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        items(crumbs.size) { index ->
                            BreadcrumbBarItem(
                                onClick = {
                                    when (index) {
                                        0 -> {
                                            tab = MainTab.Desktop
                                            stack = emptyList()
                                        }
                                        else -> stack = stack.take(index - 1)
                                    }
                                },
                                chevronVisible = index < crumbs.lastIndex,
                            ) { Text(crumbs[index], maxLines = 1) }
                        }
                    }
                    notice?.let { message ->
                        InfoBar(
                            title = { Text("Готово") },
                            message = { Text(message) },
                            severity = InfoBarSeverity.Success,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            closeAction = { InfoBarDefaults.CloseActionButton(onClick = { notice = null }) },
                        )
                    }
                    AnimatedContent(
                        targetState = top ?: tab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.weight(1f),
                    ) { target ->
                        when (target) {
                            MainTab.Desktop -> DesktopScreen(
                                loaded = desktopLoaded,
                                onLoaded = { desktopLoaded = true },
                                recent = requestsState.items.take(3),
                                onOpenType = { push(Overlay.RequestForm(null, it)) },
                                onOpenRequest = { push(Overlay.RequestForm(it, it.type)) },
                            )

                            MainTab.Requests -> RequestsScreen(
                                state = requestsState,
                                onOpenRequest = { push(Overlay.RequestForm(it, it.type)) },
                                onCreate = { push(Overlay.RequestForm(null, RequestType.Vacation)) },
                            )

                            MainTab.Profile -> ProfileScreen(onNotice = { notice = it })

                            is Overlay.RequestForm -> RequestFormScreen(
                                request = target.request,
                                initialType = target.type,
                                onDone = { message ->
                                    pop()
                                    notice = message
                                },
                            )

                            Overlay.Settings -> SettingsScreen(
                                themePrefs = themePrefs,
                                isDark = isDark,
                                onThemePrefsChange = onThemePrefsChange,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun overlayTitle(overlay: Overlay): String = when (overlay) {
    is Overlay.RequestForm -> overlay.request?.let { "Заявка №${it.id}" } ?: "Новая заявка"
    Overlay.Settings -> "Настройки"
}

/** Opaque themed background drawn before the list's content padding, so the previous screen never shows through. */
@Composable
fun ScreenList(content: LazyListScope.() -> Unit) {
    Mica(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            content = content,
        )
    }
}
