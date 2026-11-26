@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

/**
 * Заглушка пустого списка в [androidx.compose.foundation.lazy.LazyColumn] / ScreenList:
 * на всю высоту viewport списка, контент по центру (не «прибито к верху»).
 */
@Composable
fun LazyItemScope.CupertinoEmptyListStub(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    above: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillParentMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            above?.invoke()
            CupertinoIcon(
                imageVector = icon,
                contentDescription = null,
                tint = CupertinoTheme.colorScheme.tertiaryLabel,
                modifier = Modifier.size(48.dp),
            )
            CupertinoText(
                text = text,
                style = CupertinoTheme.typography.headline,
                color = CupertinoTheme.colorScheme.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}
