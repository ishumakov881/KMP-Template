@file:OptIn(ExperimentalCupertinoApi::class)

package online.lds.nos.ui.sites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

/** Intrinsic size of NOS `/inc/other/rrd/{ip}/nolegend.png` (used for loading/error box). */
internal const val MrtgGraphAspectRatio = 647f / 182f

/**
 * RRD mrtg graph image. On a real load error (HTTP 404 — file missing) shows
 * the web-parity stub "График временно недоступен!" instead of a broken image,
 * mirroring the browser rendering the `<img alt=...>`.
 *
 * Note: an empty-traffic graph is still a valid (empty) PNG, not a 404 — it loads
 * normally and must NOT fall back to the stub.
 */
@Composable
internal fun MrtgGraphImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(MrtgGraphAspectRatio),
                contentAlignment = Alignment.Center,
            ) { CupertinoActivityIndicator() }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(MrtgGraphAspectRatio)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoText(
                    text = "График временно недоступен!",
                    style = CupertinoTheme.typography.subhead,
                    color = CupertinoTheme.colorScheme.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}
