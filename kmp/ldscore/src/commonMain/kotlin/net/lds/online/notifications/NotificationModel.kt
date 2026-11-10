package net.lds.online.notifications
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class NotificationType {
    SUCCESS, ERROR
}

/** Горизонтальный якорь тоста (Web App.vue `.p-toast`). */
enum class NotificationAnchor {
    /** ≤576: full width, insets 20. */
    Start,
    /** Шире: top-right, wrap content. */
    End,
}

data class NotificationItem @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.random().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val durationMillis: Long = 4000L
)
