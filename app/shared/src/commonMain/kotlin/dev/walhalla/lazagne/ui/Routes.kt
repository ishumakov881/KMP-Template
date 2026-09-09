package dev.walhalla.lazagne.ui

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Main : AppRoute

    @Serializable
    data class RequestDetail(val id: String) : AppRoute

    @Serializable
    data object Settings : AppRoute
}

enum class MainSection(val title: String) {
    Desktop("Рабочий стол"),
    Requests("Заявки"),
    Archive("Архив"),
}
