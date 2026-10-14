package dev.walhalla.lazagne.ui

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Airplane
import io.github.alexzhirkevich.cupertino.icons.outlined.CheckmarkCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.DocText
import io.github.alexzhirkevich.cupertino.icons.outlined.Laptopcomputer
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock
import io.github.alexzhirkevich.cupertino.icons.outlined.SunMax
import io.github.alexzhirkevich.cupertino.icons.outlined.XmarkCircle

enum class RequestType(val title: String, val subtitle: String, val icon: ImageVector) {
    Vacation("Отпуск", "Ежегодный и за свой счёт", CupertinoIcons.Default.SunMax),
    Trip("Командировка", "Поездки и авансы", CupertinoIcons.Default.Airplane),
    Pass("Пропуск", "Гости и парковка", CupertinoIcons.Default.Lock),
    Equipment("Оборудование", "Ноутбуки и периферия", CupertinoIcons.Default.Laptopcomputer),
    Document("Справка", "2-НДФЛ, с места работы", CupertinoIcons.Default.DocText),
}

enum class RequestStatus(val title: String, val icon: ImageVector) {
    Pending("На согласовании", CupertinoIcons.Default.Clock),
    Approved("Согласовано", CupertinoIcons.Default.CheckmarkCircle),
    Rejected("Отклонено", CupertinoIcons.Default.XmarkCircle),
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
