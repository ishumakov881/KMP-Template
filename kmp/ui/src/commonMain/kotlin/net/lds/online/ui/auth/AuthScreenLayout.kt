package net.lds.online.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.lds.online.ui.dashboard.WebViewportBp
import net.lds.online.ui.dashboard.rememberViewportWidthDp

/**
 * Web auth-страница: брейкпоинты viewport (px), не [WindowWidth].
 *
 * На Web нет «medium»-шаблона — только куски CSS:
 * - [AuthorizationView]: column &lt;768, row ≥768
 * - [NewsContent]: 1 / 2 (≥576) / 4 (≥768) col
 * - [AuthLayout]: margins 15/20 по 768 и 1000
 */
data class AuthScreenLayout(
    val formAndAsideInRow: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val contactAsideWidth: Dp?,
    val importantNewsColumns: Int,
)

@Composable
fun rememberAuthScreenLayout(): AuthScreenLayout {
    val viewport = rememberViewportWidthDp()
    return remember(viewport) {
        AuthScreenLayout(
            formAndAsideInRow = viewport >= WebViewportBp.mlg,
            horizontalPadding = if (viewport >= WebViewportBp.tsm) 24.dp else 16.dp,
            verticalPadding = if (viewport >= WebViewportBp.mlg) 24.dp else 16.dp,
            contactAsideWidth = if (viewport >= WebViewportBp.mlg) 304.dp else null,
            importantNewsColumns = when {
                viewport >= WebViewportBp.mlg -> 4
                viewport >= WebViewportBp.mmd -> 2
                else -> 1
            },
        )
    }
}
