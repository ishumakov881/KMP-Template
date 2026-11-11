package online.lds.nos.ui.auth

import androidx.lifecycle.viewModelScope
import online.lds.nos.data.AppPreferences
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import net.lds.online.network.ApiException
import net.lds.online.network.nos.NosAuthRepository
import net.lds.online.ui.base.BaseEvent
import net.lds.online.ui.base.BaseViewModel
import net.lds.online.ui.base.UiState

data class LoginUiData(
    val login: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val sessionIdPreview: String? = null,
)

sealed interface LoginEvent : BaseEvent {
    data class LoginChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object Submit : LoginEvent
    data object DismissError : LoginEvent
}

class LoginViewModel(
    private val onAuthenticated: () -> Unit = {},
    private val authRepository: NosAuthRepository,
    private val appPreferences: AppPreferences,
) : BaseViewModel<LoginUiData, LoginEvent>() {

    init {
        _state.value = _state.value.copy(contentState = UiState.Success(LoginUiData()))
    }

    override suspend fun onFetchData(): LoginUiData = getData()

    override fun onBusinessEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.LoginChanged -> updateData {
                it.copy(login = event.value, errorMessage = null)
            }
            is LoginEvent.PasswordChanged -> updateData {
                it.copy(password = event.value, errorMessage = null)
            }
            LoginEvent.DismissError -> updateData { it.copy(errorMessage = null) }
            LoginEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val data = getData()
        if (data.login.isBlank() || data.password.isBlank()) {
            updateData { it.copy(errorMessage = "Введите логин и пароль") }
            return
        }
        viewModelScope.launch {
            updateData { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val result = authRepository.login(data.login.trim(), data.password)
                val sessionId = result.phpSessionId
                    ?: throw ApiException.clientError(401, "Нет PHPSESSID")
                appPreferences.putString(AppPreferences.KEY_PHPSESSID, sessionId)
                updateData {
                    it.copy(
                        isSubmitting = false,
                        sessionIdPreview = sessionId,
                    )
                }
                onAuthenticated()
            }
            catch (e: IOException) {


                //Не удаляем сессию если нет сети

                val api = e as? ApiException ?: ApiException.from(e)
                updateData {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "@@@@"+api.getUserMessage(),
                    )
                }
            }

            catch (e: Exception) {
                println("@@@ ${e}")
                val api = e as? ApiException ?: ApiException.from(e)
                appPreferences.remove(AppPreferences.KEY_PHPSESSID)
                updateData {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = api.getUserMessage(),
                    )
                }
            }
        }
    }

    override fun onConfirmActionWithPassword(pendingEvent: BaseEvent?) = Unit

    private fun updateData(transform: (LoginUiData) -> LoginUiData) {
        val content = _state.value.contentState
        if (content is UiState.Success) {
            _state.value = _state.value.copy(contentState = content.copy(data = transform(content.data)))
        }
    }
}
