@file:OptIn(ExperimentalCupertinoApi::class)

package dev.walhalla.lazagne.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.alexzhirkevich.cupertino.CupertinoBottomSheetContent
import io.github.alexzhirkevich.cupertino.CupertinoBottomSheetScaffold
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoIconButton
import io.github.alexzhirkevich.cupertino.CupertinoNavigateBackButton
import io.github.alexzhirkevich.cupertino.CupertinoNavigationBar
import io.github.alexzhirkevich.cupertino.CupertinoNavigationBarItem
import io.github.alexzhirkevich.cupertino.CupertinoSurface
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.ListBulletCircle
import io.github.alexzhirkevich.cupertino.icons.filled.ListBulletClipboard
import io.github.alexzhirkevich.cupertino.icons.filled.Map
// import io.github.alexzhirkevich.cupertino.icons.filled.DocText
// import io.github.alexzhirkevich.cupertino.icons.filled.House
// import io.github.alexzhirkevich.cupertino.icons.filled.PersonCropCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise
import io.github.alexzhirkevich.cupertino.icons.outlined.DocOnDoc
// import io.github.alexzhirkevich.cupertino.icons.outlined.DocText
import io.github.alexzhirkevich.cupertino.icons.outlined.EllipsisCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape
// import io.github.alexzhirkevich.cupertino.icons.outlined.House
import io.github.alexzhirkevich.cupertino.icons.outlined.ListBulletIndent
import io.github.alexzhirkevich.cupertino.icons.outlined.Map
// import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Plus
import io.github.alexzhirkevich.cupertino.icons.outlined.ServerRack
import io.github.alexzhirkevich.cupertino.icons.outlined.SquareAndArrowUp
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash
import io.github.alexzhirkevich.cupertino.rememberCupertinoBottomSheetScaffoldState
import io.github.alexzhirkevich.cupertino.rememberCupertinoSheetState
import io.github.alexzhirkevich.cupertino.section.CupertinoLinkIcon
import io.github.alexzhirkevich.cupertino.section.link
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.systemRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.lds.online.network.nos.NosMapRepository
import net.lds.online.network.nos.NosSwRepository
import dev.walhalla.lazagne.ui.map.MapRoute
import dev.walhalla.lazagne.ui.sites.SiteDetailRoute
import dev.walhalla.lazagne.ui.sites.SitesEvent
import dev.walhalla.lazagne.ui.sites.SitesRoute
import dev.walhalla.lazagne.ui.sites.SitesViewModel
import dev.walhalla.lazagne.ui.switches.SwitchesEvent
import dev.walhalla.lazagne.ui.switches.SwitchesRoute
import dev.walhalla.lazagne.ui.switches.SwitchesViewModel

const val AppName = "NOS"

enum class MainTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Map("Карта", CupertinoIcons.Default.Map, CupertinoIcons.Filled.Map),
    Switches("Свичи", CupertinoIcons.Default.ServerRack, CupertinoIcons.Filled.ListBulletCircle),
    /** Web `/sw` left menu `#s_sw` (площадки/узлы), не markgen. */
    Sites("Узлы", CupertinoIcons.Default.ListBulletIndent, CupertinoIcons.Filled.ListBulletClipboard),
    // Демо Desktop/Requests оставляем в файлах, tab убран — целевые: Карта / Свичи / Узлы.
    // Desktop("Рабочий стол", CupertinoIcons.Default.House, CupertinoIcons.Filled.House),
    // Requests("Заявки", CupertinoIcons.Default.DocText, CupertinoIcons.Filled.DocText),
    // Веб-«Профиль» = тема → Settings в тулбаре. Демо ProfileScreen.kt оставляем, tab убран.
    // Profile("Профиль", CupertinoIcons.Default.PersonCropCircle, CupertinoIcons.Filled.PersonCropCircle),
}

sealed interface Overlay {
    data class RequestForm(val request: CorpRequest?, val type: RequestType) : Overlay
    data object Settings : Overlay
    data class SwSiteDetail(val siteId: Int, val siteName: String) : Overlay
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
    mapRepository: NosMapRepository,
    swRepository: NosSwRepository,
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Map) }
    var stack by remember { mutableStateOf(listOf<Overlay>()) }
    var desktopLoaded by rememberSaveable { mutableStateOf(false) }
    val requestsState = remember { RequestsState() }
    val switchesViewModel = remember(mapRepository) { SwitchesViewModel(mapRepository) }
    val sitesViewModel = remember(swRepository) { SitesViewModel(swRepository) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberCupertinoSheetState()
    val scaffoldState = rememberCupertinoBottomSheetScaffoldState(sheetState)

    val top = stack.lastOrNull()
    val push: (Overlay) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { stack = stack.dropLast(1) }
    val hideSheet: () -> Unit = { scope.launch { sheetState.hide() } }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = stack.isNotEmpty() && !sheetState.isVisible,
        onBackCompleted = pop,
    )
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = sheetState.isVisible,
        onBackCompleted = hideSheet,
    )

    CupertinoBottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = { RequestActionsSheet(onAction = hideSheet) },
        topBar = {
            CupertinoTopAppBar(
                navigationIcon = {
                    if (top != null) {
                        val backTitle = (stack.getOrNull(stack.size - 2) as? Overlay.RequestForm)
                            ?.let { formTitle(it) } ?: tab.title
                        CupertinoNavigateBackButton(onClick = pop) {
                            CupertinoText(backTitle, maxLines = 1)
                        }
                    }
                },
                title = {
                    CupertinoText(
                        text = when (top) {
                            is Overlay.RequestForm -> formTitle(top)
                            Overlay.Settings -> "Настройки"
                            is Overlay.SwSiteDetail -> top.siteName.ifBlank { "Узел ${top.siteId}" }
                            null -> AppName
                        },
                        maxLines = 1,
                    )
                },
                actions = {
                    when (top) {
                        is Overlay.RequestForm -> CupertinoIconButton(
                            onClick = { scope.launch { sheetState.show() } },
                        ) {
                            CupertinoIcon(CupertinoIcons.Default.EllipsisCircle, contentDescription = "Ещё")
                        }

                        Overlay.Settings -> Unit

                        is Overlay.SwSiteDetail -> Unit

                        null -> {
                            if (tab == MainTab.Switches) {
                                CupertinoIconButton(
                                    onClick = { switchesViewModel.onEvent(SwitchesEvent.Refresh) },
                                ) {
                                    CupertinoIcon(
                                        CupertinoIcons.Default.ArrowClockwise,
                                        contentDescription = "Обновить",
                                    )
                                }
                            }
                            if (tab == MainTab.Sites) {
                                CupertinoIconButton(
                                    onClick = { sitesViewModel.onEvent(SitesEvent.Refresh) },
                                ) {
                                    CupertinoIcon(
                                        CupertinoIcons.Default.ArrowClockwise,
                                        contentDescription = "Обновить",
                                    )
                                }
                            }
                            // Демо Requests actions — tab закомментирован
                            // if (tab == MainTab.Requests) { ... }
                            CupertinoIconButton(onClick = { push(Overlay.Settings) }) {
                                CupertinoIcon(CupertinoIcons.Default.Gearshape, contentDescription = "Настройки")
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            CupertinoNavigationBar {
                MainTab.entries.forEach { item ->
                    val selected = item == tab
                    CupertinoNavigationBarItem(
                        selected = selected,
                        onClick = {
                            tab = item
                            stack = emptyList()
                        },
                        icon = {
                            CupertinoIcon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = null,
                            )
                        },
                        label = { CupertinoText(item.title) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = top ?: tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { target ->
            when (target) {
                MainTab.Map -> MapRoute(
                    mapRepository = mapRepository,
                    padding = padding,
                )

                MainTab.Switches -> SwitchesRoute(
                    mapRepository = mapRepository,
                    padding = padding,
                    viewModel = switchesViewModel,
                )

                MainTab.Sites -> SitesRoute(
                    swRepository = swRepository,
                    padding = padding,
                    viewModel = sitesViewModel,
                    onOpenSite = { site ->
                        push(Overlay.SwSiteDetail(siteId = site.id, siteName = site.name))
                    },
                )

                // MainTab.Desktop -> DesktopScreen(...)
                // MainTab.Requests -> RequestsScreen(...)
                // MainTab.Profile -> ProfileScreen(padding = padding)

                is Overlay.RequestForm -> RequestFormScreen(
                    padding = padding,
                    request = target.request,
                    initialType = target.type,
                    onSubmitted = pop,
                )

                Overlay.Settings -> SettingsScreen(
                    padding = padding,
                    themePrefs = themePrefs,
                    isDark = isDark,
                    onThemePrefsChange = onThemePrefsChange,
                )

                is Overlay.SwSiteDetail -> SiteDetailRoute(
                    swRepository = swRepository,
                    siteId = target.siteId,
                    siteName = target.siteName,
                    padding = padding,
                )
            }
        }
    }
}

private fun formTitle(form: Overlay.RequestForm): String =
    form.request?.let { "Заявка №${it.id}" } ?: "Новая заявка"

/** Opaque themed background applied before the scaffold padding, so the previous screen never shows through. */
@Composable
fun ScreenList(
    padding: PaddingValues,
    content: LazyListScope.() -> Unit,
) {
    CupertinoSurface(
        modifier = Modifier.fillMaxSize(),
        color = CupertinoTheme.colorScheme.systemGroupedBackground,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            content = content,
        )
    }
}

@Composable
private fun RequestActionsSheet(onAction: () -> Unit) {
    CupertinoBottomSheetContent(
        topBar = {
            CupertinoTopAppBar(
                title = { CupertinoText("Действия") },
                actions = {
                    CupertinoButton(
                        onClick = onAction,
                        colors = CupertinoButtonDefaults.plainButtonColors(),
                    ) {
                        CupertinoText("Готово")
                    }
                },
            )
        },
    ) { sheetPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = sheetPadding,
        ) {
            section {
                link(
                    onClick = onAction,
                    icon = { CupertinoLinkIcon(CupertinoIcons.Default.DocOnDoc) },
                ) { CupertinoText("Дублировать") }
                link(
                    onClick = onAction,
                    icon = { CupertinoLinkIcon(CupertinoIcons.Default.SquareAndArrowUp) },
                ) { CupertinoText("Поделиться") }
                link(
                    onClick = onAction,
                    icon = {
                        CupertinoLinkIcon(
                            imageVector = CupertinoIcons.Default.Trash,
                            containerColor = CupertinoColors.systemRed,
                        )
                    },
                ) { CupertinoText("Отозвать заявку", color = CupertinoColors.systemRed) }
            }
        }
    }
}
