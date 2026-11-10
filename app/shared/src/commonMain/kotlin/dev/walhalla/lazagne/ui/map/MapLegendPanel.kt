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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import net.lds.online.network.nos.legend.MapLegendGroupState
import net.lds.online.network.nos.legend.MapLegendItemState
import net.lds.online.network.nos.legend.MapLegendSnapshot

/**
 * Floating map legend (web `#legend` / `#more_legend` parity).
 */
@Composable
fun MapLegendPanel(
    snapshot: MapLegendSnapshot,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
) {
    if (snapshot.isEmpty) return

    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val shape = RoundedCornerShape(12.dp)
    val surface = CupertinoTheme.colorScheme.secondarySystemBackground.copy(alpha = 0.94f)

    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(shape)
            .background(surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val legendToggleSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // CupertinoIndication is not IndicationNodeFactory; no-arg clickable crashes on newer Compose.
                .clickable(
                    interactionSource = legendToggleSource,
                    indication = LocalIndication.current,
                ) { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CupertinoText(
                text = "Легенда",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(Modifier.height(2.dp))
                snapshot.groups.forEach { group ->
                    LegendGroupBlock(group)
                }
            }
        }
    }
}

@Composable
private fun LegendGroupBlock(group: MapLegendGroupState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CupertinoText(
            text = group.group.title,
            style = CupertinoTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
            color = CupertinoTheme.colorScheme.secondaryLabel,
        )
        group.items.forEach { row ->
            LegendItemRow(row)
        }
    }
}

@Composable
private fun LegendItemRow(row: MapLegendItemState) {
    val item = row.item
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val iconUrl = item.iconUrl()
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = item.title,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Spacer(Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        CupertinoText(
            text = buildString {
                append(item.title)
                if (row.count > 0) append(" (${row.count})")
            },
            style = CupertinoTheme.typography.footnote,
            modifier = Modifier.weight(1f),
        )
    }
}
