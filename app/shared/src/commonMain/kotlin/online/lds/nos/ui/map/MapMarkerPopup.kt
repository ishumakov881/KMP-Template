@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoSurface
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTextField
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.default
import io.github.alexzhirkevich.cupertino.destructive
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import net.lds.online.network.nos.LeanMapMarker

/**
 * Web Leaflet popup parity: details + ping / snmp / node / graphs / maintenance.
 */
@Composable
fun MapMarkerPopup(
    marker: LeanMapMarker,
    pingLoading: Boolean,
    onDismiss: () -> Unit,
    onPing: () -> Unit,
    onOpenPic: () -> Unit,
    onOpenSite: () -> Unit,
    onOpenSnmp: () -> Unit,
    onRequestEnableMaintenance: () -> Unit,
    onDisableMaintenance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CupertinoSurface(
        modifier = modifier.fillMaxWidth(),
        color = CupertinoTheme.colorScheme.secondarySystemGroupedBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CupertinoText(
                    text = marker.name.ifBlank { "Маркер" },
                    style = CupertinoTheme.typography.headline,
                    modifier = Modifier.weight(1f),
                )
                CupertinoButton(
                    onClick = onDismiss,
                    colors = CupertinoButtonDefaults.plainButtonColors(),
                ) { CupertinoText("Закрыть") }
            }

            PopupLine("Тип", marker.type)
            PopupLine("Слой", marker.layer)
            marker.ip?.let { PopupLine("IP", it) }
            marker.siteLabel?.let { PopupLine("Узел", it) }
            marker.place?.let { PopupLine("Место", it) }
            marker.deviceModel?.let { PopupLine("Модель", it) }
            marker.serial?.let { PopupLine("Серийник", it) }
            marker.mac?.let { PopupLine("MAC", it) }
            if (marker.maintenanceActive) {
                PopupLine("Техработы", marker.maintenanceText ?: "активны")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (marker.ip != null && marker.pingType != null) {
                    CupertinoButton(
                        onClick = onPing,
                        enabled = !pingLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (pingLoading) CupertinoActivityIndicator()
                        else CupertinoText("Ping")
                    }
                }
                if (marker.ip != null) {
                    CupertinoButton(
                        onClick = onOpenPic,
                        colors = CupertinoButtonDefaults.tintedButtonColors(),
                        modifier = Modifier.weight(1f),
                    ) { CupertinoText("Графики") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (marker.ip != null) {
                    CupertinoButton(
                        onClick = onOpenSnmp,
                        colors = CupertinoButtonDefaults.tintedButtonColors(),
                        modifier = Modifier.weight(1f),
                    ) { CupertinoText("SNMP") }
                }
                if (marker.siteId != null) {
                    CupertinoButton(
                        onClick = onOpenSite,
                        colors = CupertinoButtonDefaults.tintedButtonColors(),
                        modifier = Modifier.weight(1f),
                    ) { CupertinoText("Узел") }
                }
            }

            if (marker.maintenanceId != null) {
                CupertinoButton(
                    onClick = {
                        if (marker.maintenanceActive) onDisableMaintenance()
                        else onRequestEnableMaintenance()
                    },
                    colors = CupertinoButtonDefaults.tintedButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CupertinoText(
                        if (marker.maintenanceActive) "Снять техработы" else "Техработы",
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupLine(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        CupertinoText(
            text = label,
            style = CupertinoTheme.typography.footnote,
            color = CupertinoTheme.colorScheme.secondaryLabel,
        )
        CupertinoText(value)
    }
}

@Composable
fun MapMaintenanceCommentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    CupertinoAlertDialog(
        onDismissRequest = onDismiss,
        title = { CupertinoText("Техработы") },
        message = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CupertinoText("Комментарий")
                CupertinoTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        default(onClick = { onConfirm(text) }) { CupertinoText("Ok") }
        destructive(onClick = onDismiss) { CupertinoText("Отмена") }
    }
}
