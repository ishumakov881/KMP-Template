package net.lds.online.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.lds.online.network.ApiException
import net.lds.online.notifications.NotificationType

/**
 * Бизнес-состояние экрана.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T, val isRefreshing: Boolean = false) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

/**
 * Локальное UI-состояние (диалоги, пароли и ожидающее событие).
 */
data class ScreenUiLocalState(
    val confirmPassword: String = "",
    val isPasswordDialogVisible: Boolean = false,
    val pendingEvent: BaseEvent? = null
)

/**
 * Общее состояние экрана.
 */
data class ScreenState<T>(
    val contentState: UiState<T>,
    val ui: ScreenUiLocalState = ScreenUiLocalState()
)

/**
 * Базовый маркер для всех событий приложения.
 */
interface BaseEvent

/**
 * Системные события для управления диалогами (пароли и т.д.)
 */
sealed interface BaseUiEvent : BaseEvent {
    data class OnConfirmPasswordChange(val password: String) : BaseUiEvent
    data object DismissPasswordDialog : BaseUiEvent
    data object ConfirmActionWithPassword : BaseUiEvent
}

abstract class BaseViewModel<T, EVENT : BaseEvent> : ViewModel() {

    protected val _state = MutableStateFlow<ScreenState<T>>(
        ScreenState(contentState = UiState.Loading)
    )
    val state = _state.asStateFlow()

    protected val _effect = Channel<ServicesEffect>()
    val effect = _effect.receiveAsFlow()

    /**
     * Реализация загрузки данных.
     */
    abstract suspend fun onFetchData(): T

    /**
     * Обработка бизнес-логики в наследниках.
     */
    abstract fun onBusinessEvent(event: EVENT)

    /**
     * Точка входа для всех событий.
     */
    fun onEvent(event: BaseEvent) {
        when (event) {
            is BaseUiEvent -> handleBaseUiEvent(event)
            else -> {
                println("[EVENT] $event")
                @Suppress("UNCHECKED_CAST") onBusinessEvent(event as EVENT)
            }
        }
    }

    private fun handleBaseUiEvent(event: BaseUiEvent) {
        when (event) {
            is BaseUiEvent.OnConfirmPasswordChange -> {
                updateUi { it.copy(confirmPassword = event.password) }
            }
            is BaseUiEvent.ConfirmActionWithPassword -> {
                val pending = _state.value.ui.pendingEvent
                updateUi { it.copy(isPasswordDialogVisible = false, pendingEvent = null) }
                onConfirmActionWithPassword(pending)
            }
            is BaseUiEvent.DismissPasswordDialog -> {
                updateUi { it.copy(isPasswordDialogVisible = false, pendingEvent = null) }
                onDialogDismissed()
            }
        }
    }

    /**
     * Запуск подтверждения паролем.
     * Автоматически подставляет пароль и запоминает событие.
     */
    protected fun triggerPasswordConfirmation(
        getPassword: suspend () -> String,
        event: BaseEvent,
    ) {
        viewModelScope.launch {
            val savedPassword = getPassword()
            updateUi { 
                it.copy(
                    isPasswordDialogVisible = true, 
                    confirmPassword = savedPassword,
                    pendingEvent = event
                ) 
            }
        }
    }

    /**
     * Хук для действий после подтверждения пароля.
     */
    abstract fun onConfirmActionWithPassword(pendingEvent: BaseEvent?)

    /**
     * Хук для очистки при закрытии диалога.
     */
    open fun onDialogDismissed() {}

    /**
     * Быстрый доступ к данным Success-состояния.
     */
    protected fun getData(): T {
        val content = _state.value.contentState
        if (content is UiState.Success) return content.data
        throw IllegalStateException("Accessing data when state is not Success")
    }

    // --- Инфраструктура загрузки и ошибок ---

    fun loadData(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshing) setRefreshing(true)
            else _state.update { it.copy(contentState = UiState.Loading) }

            try {
                val result = onFetchData()
                _state.update { it.copy(contentState = UiState.Success(result)) }
            } catch (e: Exception) {
                handleThrowable(e)
            }
        }
    }

    protected open suspend fun handleThrowable(e: Exception) {
        val apiEx = e as? ApiException ?: ApiException.from(e)
        val message = apiEx.message
        val isWarning = apiEx.isClientError() ||
            (apiEx.code == 500 && !message.contains("Server error", ignoreCase = true))

        if (isWarning) {
            _effect.send(ShowToast(message))
            setRefreshing(false)
        } else {
            _state.update { it.copy(contentState = UiState.Error(apiEx.getUserMessage())) }
        }
    }

    protected fun updateUi(transform: (ScreenUiLocalState) -> ScreenUiLocalState) {
        _state.update { it.copy(ui = transform(it.ui)) }
    }

    protected fun setRefreshing(value: Boolean) {
        val currentContent = _state.value.contentState
        if (currentContent is UiState.Success) {
            _state.update { it.copy(contentState = currentContent.copy(isRefreshing = value)) }
        }
    }

    /**
     * Универсальный обработчик для действий (не загрузки контента).
     */
    suspend fun <R> handleActionResult(
        result: Result<R>,
        errorTitle: String? = "Ошибка",
        onSuccess: suspend (R) -> Unit
    ) {
        result.onSuccess {
            onSuccess(it)
        }.onFailure { e ->
            val apiEx = e as? ApiException ?: ApiException.from(e)
            _effect.send(
                ShowToast(
                    message = apiEx.getUserMessage(),
                    title = errorTitle,
                    type = NotificationType.ERROR
                )
            )
        }
    }
}
