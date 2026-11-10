@file:OptIn(ExperimentalCupertinoApi::class)

package dev.walhalla.lazagne.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.walhalla.lazagne.ui.ScreenList
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.default
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.section.textField
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import net.lds.online.ui.auth.rememberAuthScreenLayout
import net.lds.online.ui.base.BaseScreenLayout
import net.lds.online.ui.dashboard.AuthFormTokens
import net.lds.online.ui.dashboard.authContentMaxWidth
import net.lds.online.ui.dashboard.rememberAuthFormLayoutMetrics
import net.lds.online.ui.dashboard.rememberWindowWidth

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val windowWidth = rememberWindowWidth()
    val authLayout = rememberAuthScreenLayout()
    val metrics = rememberAuthFormLayoutMetrics(windowWidth.authContentMaxWidth)

    BaseScreenLayout(
        state = state,
        onRefresh = { viewModel.loadData(isRefreshing = true) },
        onRetry = { viewModel.loadData() },
        modifier = modifier,
    ) { data ->
        val listPadding = PaddingValues(
            horizontal = authLayout.horizontalPadding,
            vertical = authLayout.verticalPadding,
        )

        ScreenList(listPadding) {
            item {
                CupertinoText(
                    text = "NOS",
                    style = CupertinoTheme.typography.largeTitle.copy(fontSize = metrics.titleSp.sp),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                CupertinoText(
                    text = "Вход в систему",
                    style = CupertinoTheme.typography.body,
                    modifier = Modifier.padding(bottom = metrics.contentGap),
                )
            }

            section(
                title = { CupertinoText("Учётная запись".sectionTitle()) },
                caption = {
                    CupertinoText("Сессия как в веб-NOS: cookie PHPSESSID")
                },
            ) {
                textField(
                    value = data.login,
                    onValueChange = { viewModel.onEvent(LoginEvent.LoginChanged(it)) },
                    placeholder = { CupertinoText("Логин") },
                )
                textField(
                    value = data.password,
                    onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                    placeholder = { CupertinoText("Пароль") },
                )
            }

            item {
                val buttonModifier = if (metrics.primaryButtonFullWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(min = AuthFormTokens.primaryButtonWidth)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CupertinoSectionDefaults.PaddingValues),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CupertinoButton(
                        onClick = { viewModel.onEvent(LoginEvent.Submit) },
                        enabled = !data.isSubmitting,
                        modifier = buttonModifier,
                    ) {
                        if (data.isSubmitting) {
                            CupertinoActivityIndicator()
                        } else {
                            CupertinoText("Вход")
                        }
                    }
                }
            }

            if (authLayout.formAndAsideInRow) {
                item {
                    CupertinoText(
                        text = "Широкий layout (≥768): phone/tablet landscape",
                        style = CupertinoTheme.typography.footnote,
                        modifier = Modifier
                            .widthIn(max = windowWidth.authContentMaxWidth)
                            .padding(top = 8.dp),
                    )
                }
            }
        }

        data.errorMessage?.let { message ->
            CupertinoAlertDialog(
                onDismissRequest = { viewModel.onEvent(LoginEvent.DismissError) },
                title = { CupertinoText("Ошибка входа") },
                message = { CupertinoText(message) },
            ) {
                default(onClick = { viewModel.onEvent(LoginEvent.DismissError) }) {
                    CupertinoText("Понятно")
                }
            }
        }
    }
}

@Composable
fun LoginRoute(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { LoginViewModel(onAuthenticated) }
    LoginScreen(viewModel = viewModel, modifier = modifier)
}
