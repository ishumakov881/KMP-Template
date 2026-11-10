package net.lds.online.ui.base

import net.lds.online.notifications.NotificationType

/**
 * Базовый интерфейс для всех Side-эффектов (навигация, тосты и т.д.)
 */
interface ServicesEffect

/**
 * Общий эффект для отображения уведомлений, используемый во всех ViewModel.
 * Теперь поддерживает типы (Success/Error) и заголовки.
 */
data class ShowToast(
    val message: String,
    val title: String? = null,
    val type: NotificationType = NotificationType.ERROR
) : ServicesEffect

/**
 * Эффект для скачивания файлов.
 */
data class DownloadFile(
    val base64: String,
    val mime: String,
    val name: String
) : ServicesEffect

/** Закрыть диалог смены MAC после успешного ответа сервера. */
data object MacAddressChangeSucceeded : ServicesEffect

/** Закрыть диалог смены роутера после успешного ответа сервера. */
data object DisableControlSucceeded : ServicesEffect

/**
 * Эффект навигации
 */
interface NavigationEffect : ServicesEffect
