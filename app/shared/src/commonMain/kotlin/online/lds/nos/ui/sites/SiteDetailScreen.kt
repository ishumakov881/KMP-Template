@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.sites

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import online.lds.nos.ui.ScreenList
import online.lds.nos.ui.TitleWithSubtitle
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ServerRack
import io.github.alexzhirkevich.cupertino.section.CupertinoLinkIcon
import io.github.alexzhirkevich.cupertino.section.CupertinoSectionDefaults
import io.github.alexzhirkevich.cupertino.section.items
import io.github.alexzhirkevich.cupertino.section.section
import io.github.alexzhirkevich.cupertino.section.sectionTitle
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme
import io.github.alexzhirkevich.cupertino.theme.systemGreen
import io.github.alexzhirkevich.cupertino.theme.systemRed
import net.lds.online.network.nos.NosSwRepository
import net.lds.online.network.nos.SwDevice
import net.lds.online.ui.base.BaseScreenLayout
import net.lds.online.ui.base.UiState

@Composable
fun SiteDetailRoute(
    swRepository: NosSwRepository,
    siteId: Int,
    siteName: String,
    padding: PaddingValues,
    onOpenPic: (ip: String, titleHint: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember(swRepository, siteId) {
        SiteDetailViewModel(swRepository, siteId, siteName)
    }
    SiteDetailScreen(
        viewModel = viewModel,
        padding = padding,
        onOpenPic = onOpenPic,
        modifier = modifier,
    )
}

@Composable
fun SiteDetailScreen(
    viewModel: SiteDetailViewModel,
    padding: PaddingValues,
    onOpenPic: (ip: String, titleHint: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = modifier.fillMaxSize()) {
        BaseScreenLayout(
            state = state,
            onRefresh = { viewModel.onEvent(SiteDetailEvent.Refresh) },
            onRetry = { viewModel.loadData() },
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            val devices = data.devices
            val refreshing = (state.contentState as? UiState.Success)?.isRefreshing == true
            ScreenList(padding) {
                if (refreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(CupertinoSectionDefaults.PaddingValues),
                            contentAlignment = Alignment.Center,
                        ) { CupertinoActivityIndicator() }
                    }
                }

                if (devices.isEmpty()) {
                    item { EmptyDevicesState() }
                } else {
                    section(
                        title = {
                            CupertinoText(
                                "${data.title} · ${devices.size}".sectionTitle(),
                            )
                        },
                    ) {
                        items(
                            items = devices,
                            key = { "${it.ip}|${it.number}" },
                        ) { device, itemPadding ->
                            DeviceRow(
                                device = device,
                                modifier = Modifier.padding(itemPadding),
                                onOpenPic = onOpenPic,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: SwDevice,
    onOpenPic: (ip: String, titleHint: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val open = {
        if (device.ip.isNotBlank()) {
            onOpenPic(device.ip, device.address.ifBlank { device.ip })
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = open,
            )
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CupertinoLinkIcon(
                imageVector = CupertinoIcons.Default.ServerRack,
                containerColor = if (device.down) {
                    CupertinoColors.systemRed
                } else {
                    CupertinoColors.systemGreen
                },
            )
            Spacer(Modifier.width(12.dp))
            TitleWithSubtitle(
                title = device.ip.ifBlank { "—" },
                subtitle = buildString {
                    append("№${device.number}")
                    val body = device.subtitle
                    if (body.isNotBlank()) {
                        append(" · ")
                        append(body)
                    }
                    if (device.vlan.isNotBlank()) {
                        append(" · ")
                        append(device.vlan)
                    }
                },
            )
        }
        val mrtgUrl = device.mrtgUrl
        if (mrtgUrl != null) {
            // Web `nolegend.png` is ~647×182 — keep full frame, no top/bottom crop.
            // Loads via singleton Coil + NOS CookieJar (see App.setSingletonImageLoaderFactory).
            AsyncImage(
                model = mrtgUrl,
                contentDescription = "mrtg ${device.ip}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(MrtgThumbAspectRatio)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Intrinsic size of NOS `/inc/other/rrd/{ip}/nolegend.png`. */
private const val MrtgThumbAspectRatio = 647f / 182f

@Composable
private fun EmptyDevicesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CupertinoIcon(
            imageVector = CupertinoIcons.Default.ServerRack,
            contentDescription = null,
            tint = CupertinoTheme.colorScheme.tertiaryLabel,
            modifier = Modifier.size(48.dp),
        )
        CupertinoText(
            text = "Нет свичей на узле",
            style = CupertinoTheme.typography.headline,
            color = CupertinoTheme.colorScheme.secondaryLabel,
            textAlign = TextAlign.Center,
        )
    }
}
