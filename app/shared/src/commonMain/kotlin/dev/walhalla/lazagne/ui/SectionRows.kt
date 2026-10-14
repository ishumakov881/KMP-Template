package dev.walhalla.lazagne.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoText
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

@Composable
fun TitleWithSubtitle(title: String, subtitle: String) {
    Column {
        CupertinoText(title, maxLines = 1)
        CupertinoText(
            text = subtitle,
            style = CupertinoTheme.typography.footnote,
            color = CupertinoTheme.colorScheme.secondaryLabel,
            maxLines = 1,
        )
    }
}

/** Section row: title on the start, a kit control (checkbox, value text) on the end. */
@Composable
fun TrailingControlRow(
    padding: PaddingValues,
    title: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoText(title, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Section row with a caption line and a full-width kit control (slider) below it. */
@Composable
fun LabeledControlRow(
    padding: PaddingValues,
    title: String,
    value: String,
    control: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CupertinoText(title, modifier = Modifier.weight(1f))
            CupertinoText(value, color = CupertinoTheme.colorScheme.secondaryLabel)
        }
        control()
    }
}
