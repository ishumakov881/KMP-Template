package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.BreadcrumbBar
import top.yukonga.miuix.kmp.basic.BreadcrumbItem
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val WideBreakpoint = 840.dp

@Composable
fun CorporateShell(
    themeMode: ColorSchemeMode,
    keyColor: Color,
    onThemeModeChange: (ColorSchemeMode) -> Unit,
    onKeyColorChange: (Color) -> Unit,
) {
    val backStack = rememberNavBackStack<AppRoute>(AppRoute.Main)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf(MainSection.Desktop) }
    var tabIndex by remember { mutableIntStateOf(0) }

    val current = (backStack.lastOrNull() as? AppRoute) ?: AppRoute.Main
    val isMain = current is AppRoute.Main

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= WideBreakpoint

        Scaffold(
            topBar = {
                TopAppBar(
                    title = when (current) {
                        is AppRoute.Settings -> "Настройки"
                        is AppRoute.RequestDetail -> "Заявка ${current.id}"
                        AppRoute.Main -> "CorpDesk"
                    },
                    navigationIcon = {
                        if (!isMain) {
                            IconButton(onClick = { backStack.removeLastOrNull() }) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = "Назад",
                                    tint = MiuixTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    },
                    actions = {
                        if (isMain) {
                            TooltipBox(text = "Настройки") {
                                IconButton(onClick = { backStack.add(AppRoute.Settings) }) {
                                    Icon(
                                        imageVector = MiuixIcons.Settings,
                                        contentDescription = "Настройки",
                                        tint = MiuixTheme.colorScheme.onBackground,
                                    )
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (isMain && !useRail) {
                    MainNavigationBar(
                        section = section,
                        onSectionChange = {
                            section = it
                            tabIndex = 0
                        },
                    )
                }
            },
            floatingActionButton = {
                if (isMain && section == MainSection.Desktop) {
                    FloatingActionButton(
                        onClick = {
                            section = MainSection.Requests
                            tabIndex = 0
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Создайте заявку во вкладке «Новая»",
                                    actionLabel = "Ок",
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "Новая заявка",
                            tint = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            snackbarHost = { SnackbarHost(state = snackbarHostState) },
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                if (isMain && useRail) {
                    MainNavigationRail(
                        section = section,
                        onSectionChange = {
                            section = it
                            tabIndex = 0
                        },
                    )
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    entry<AppRoute.Main> {
                        MainWorkspace(
                            section = section,
                            tabIndex = tabIndex,
                            onTabIndexChange = { tabIndex = it },
                            onOpenRequest = { id -> backStack.add(AppRoute.RequestDetail(id)) },
                            snackbarHostState = snackbarHostState,
                        )
                    }
                    entry<AppRoute.RequestDetail> { route ->
                        RequestDetailScreen(
                            requestId = route.id,
                            snackbarHostState = snackbarHostState,
                            onSaved = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Заявка ${route.id} сохранена",
                                        actionLabel = "Отменить",
                                    )
                                }
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                    entry<AppRoute.Settings> {
                        SettingsScreen(
                            themeMode = themeMode,
                            keyColor = keyColor,
                            onThemeModeChange = onThemeModeChange,
                            onKeyColorChange = onKeyColorChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavigationBar(
    section: MainSection,
    onSectionChange: (MainSection) -> Unit,
) {
    val sections = MainSection.entries
    val icons = listOf(MiuixIcons.Home, MiuixIcons.Notes, MiuixIcons.Folder)
    NavigationBar {
        sections.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = section == item,
                onClick = { onSectionChange(item) },
                icon = icons[index],
                label = item.title,
                badge = if (item == MainSection.Requests) {
                    {
                        Badge { Text("3") }
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun MainNavigationRail(
    section: MainSection,
    onSectionChange: (MainSection) -> Unit,
) {
    val sections = MainSection.entries
    val icons = listOf(MiuixIcons.Home, MiuixIcons.Notes, MiuixIcons.Folder)
    NavigationRail {
        sections.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = section == item,
                onClick = { onSectionChange(item) },
                icon = icons[index],
                label = item.title,
                badge = if (item == MainSection.Requests) {
                    {
                        Badge { Text("3") }
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun MainWorkspace(
    section: MainSection,
    tabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    onOpenRequest: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val tabs = when (section) {
        MainSection.Desktop -> listOf("Все", "Мои", "Команда")
        MainSection.Requests -> listOf("Новая", "Черновики")
        MainSection.Archive -> listOf("Закрытые", "Отклонённые")
    }
    val safeTab = tabIndex.coerceIn(0, tabs.lastIndex)

    val crumbs = listOf(
        BreadcrumbItem(path = "CorpDesk", text = "CorpDesk"),
        BreadcrumbItem(path = section.name, text = section.title),
        BreadcrumbItem(path = tabs[safeTab], text = tabs[safeTab]),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        BreadcrumbBar(
            items = crumbs,
            onItemClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        TabRow(
            tabs = tabs,
            selectedTabIndex = safeTab,
            onTabSelected = onTabIndexChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when (section) {
            MainSection.Desktop -> DesktopScreen(
                tabLabel = tabs[safeTab],
                onOpenRequest = onOpenRequest,
                snackbarHostState = snackbarHostState,
            )
            MainSection.Requests -> RequestFormScreen(
                tabLabel = tabs[safeTab],
                snackbarHostState = snackbarHostState,
            )
            MainSection.Archive -> ArchiveScreen(
                tabLabel = tabs[safeTab],
                onOpenRequest = onOpenRequest,
            )
        }
    }
}
