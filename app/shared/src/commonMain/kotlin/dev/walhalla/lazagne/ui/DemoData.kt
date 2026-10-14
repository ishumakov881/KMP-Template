package dev.walhalla.lazagne.ui

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.composefluent.component.BadgeStatus
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Airplane
import io.github.composefluent.icons.regular.Briefcase
import io.github.composefluent.icons.regular.DocumentText
import io.github.composefluent.icons.regular.Laptop
import io.github.composefluent.icons.regular.PersonCircle

enum class RequestType(val title: String, val subtitle: String, val icon: ImageVector) {
    Vacation("Отпуск", "Ежегодный и за свой счёт", Icons.Regular.Briefcase),
    Trip("Командировка", "Поездки и авансы", Icons.Regular.Airplane),
    Pass("Пропуск", "Гости и парковка", Icons.Regular.PersonCircle),
    Equipment("Оборудование", "Ноутбуки и периферия", Icons.Regular.Laptop),
    Document("Справка", "2-НДФЛ, с места работы", Icons.Regular.DocumentText),
}

enum class RequestStatus(val title: String, val badge: BadgeStatus) {
    Pending("На согласовании", BadgeStatus.Caution),
    Approved("Согласовано", BadgeStatus.Success),
    Rejected("Отклонено", BadgeStatus.Critical),
}

enum class RequestFolder(val title: String) {
    Inbox("Входящие"),
    Mine("Мои"),
    Archive("Архив"),
}

data class CorpRequest(
    val id: Int,
    val type: RequestType,
    val author: String,
    val status: RequestStatus,
    val folder: RequestFolder,
)

object DemoData {
    private val authors = listOf("Анна Петрова", "Иван Соколов", "Мария Ким", "Олег Лебедев", "Дина Ахмедова")

    fun requests(seed: Int = 0): List<CorpRequest> = List(40) { index ->
        val n = index + seed
        CorpRequest(
            id = 1000 + index,
            type = RequestType.entries[n % RequestType.entries.size],
            author = authors[n % authors.size],
            status = RequestStatus.entries[(n / 2) % RequestStatus.entries.size],
            folder = if (index % 4 == 0) RequestFolder.Mine else RequestFolder.Inbox,
        )
    }
}
