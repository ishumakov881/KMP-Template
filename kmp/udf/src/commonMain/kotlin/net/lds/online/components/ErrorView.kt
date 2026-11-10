package net.lds.online.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    title: String = "Что-то пошло не так",
    message: String = "Попробуйте еще раз позже.",
    onRetry: (() -> Unit)? = null,
    retryButtonModifier: Modifier? = null,
) {
    val text = remember(message) { htmlToAnnotatedString(message) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CupertinoText(
            text = title,
            style = CupertinoTheme.typography.title2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        CupertinoText(
            text = text,
            style = CupertinoTheme.typography.body,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        if (onRetry != null) {
            CupertinoButton(
                onClick = onRetry,
                modifier = retryButtonModifier ?: Modifier,
                colors = CupertinoButtonDefaults.tintedButtonColors(),
            ) {
                CupertinoText("Повторить")
            }
        }
    }
}
