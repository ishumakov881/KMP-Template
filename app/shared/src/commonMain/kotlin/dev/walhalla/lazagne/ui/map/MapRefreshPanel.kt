package dev.walhalla.lazagne.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoCheckBox
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

/**
 * Web `#intval` / `#more_intval` — auto refresh interval for map markers.
 */
@Composable
fun MapRefreshPanel(
    selected: MapRefreshInterval,
    onSelect: (MapRefreshInterval) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val shape = RoundedCornerShape(12.dp)
    val surface = CupertinoTheme.colorScheme.secondarySystemBackground.copy(alpha = 0.94f)
    val headerSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(shape)
            .background(surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = headerSource,
                    indication = LocalIndication.current,
                ) { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CupertinoText(
                text = "Обновление",
                style = CupertinoTheme.typography.subhead.copy(fontWeight = FontWeight.SemiBold),
            )
            CupertinoText(
                text = if (expanded) "▲" else "▼",
                color = CupertinoTheme.colorScheme.secondaryLabel,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Spacer(Modifier.height(2.dp))
                CupertinoText(
                    text = selected.title,
                    style = CupertinoTheme.typography.footnote,
                    color = CupertinoTheme.colorScheme.secondaryLabel,
                )
                MapRefreshInterval.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CupertinoCheckBox(
                            checked = selected == option,
                            onCheckedChange = { checked ->
                                if (checked) onSelect(option)
                            },
                        )
                        CupertinoText(
                            text = option.title,
                            style = CupertinoTheme.typography.footnote,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
