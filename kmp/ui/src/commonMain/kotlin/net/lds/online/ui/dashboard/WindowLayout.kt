package net.lds.online.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.lds.online.notifications.NotificationAnchor

/** Wide layout (permanent drawer) — Android via adaptive API; JVM/iOS via [material3-window-size-class]. */
enum class WindowWidth {
    Compact,
    Medium,
    Expanded
}
@Composable
expect fun rememberWindowWidth(): WindowWidth

/**
 * Web `$window-maxsize` (`_variable.scss`) — CSS px ≈ Compose dp при `width=device-width`.
 * Единый источник порогов; не путать с Material WindowWidthSizeClass (600 / 840).
 */
object WebViewportBp {
    val msm = 480.dp   // phone small
    val mmd = 576.dp   // phone medium
    val mlg = 768.dp   // phone large / auth row
    val tsm = 1000.dp  // tablet small — HomeView 2 col, auth form wide
    val tlg = 1280.dp  // tablet large — permanent MainNavigation / extended shell
}

/** Viewport width in dp — Web `@media` breakpoints. */
@Composable
fun rememberViewportWidthDp(): Dp {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return with(density) { containerSize.width.toDp() }
}

/**
 * Web [MainNavigation]: постоянный сайдбар при `min-width: 1280px`.
 * Не путать с Material [WindowWidth.Expanded] (≥840dp).
 */
@Composable
fun rememberIsWebExtendedShell(): Boolean =
    rememberViewportWidthDp() >= WebViewportBp.tlg

/**
 * Web [HomeView] `.row`: 2 колонки при `min-width: 1000px`.
 */
@Composable
fun rememberIsWebAccountTwoColumns(): Boolean =
    rememberViewportWidthDp() >= WebViewportBp.tsm

/**
 * Web `_auth-form.scss` + layout tokens, dp округлены до кратности 8.
 * Breakpoints viewport: [WebViewportBp.msm] / [WebViewportBp.tsm].
 */
object AuthFormTokens {
    val wrapperPaddingHorizontal = 24.dp   // Web 20
    val wrapperPaddingVertical = 32.dp     // Web 30
    val fieldsGap = 24.dp                  // Web `.auth-form__fields` 20
    val ctrlGap = 8.dp                     // Web `.auth-form__ctrl` 10
    val titleBlockGap = 8.dp               // Web `.auth-form__title` 10
    val innerFormWidth = 400.dp            // Web ≥1000
    val primaryButtonWidth = 200.dp        // Web ≥480
    val contentGap = 24.dp                 // Web `.auth-form` gap 20
    val contentGapWide = 32.dp             // Web `.auth-form` gap 30 (≥1000)
    /** Мин. ширина поля капчи в строке; иначе FlowRow переносит капчу на следующую. */
    val captchaFieldMinWidth = 200.dp
    val captchaCanvasWidth = 120.dp
    val captchaRefreshWidth = 32.dp
}

data class AuthFormLayoutMetrics(
    val innerWidth: Dp,
    val primaryButtonFullWidth: Boolean,
    val titleSp: Int,
    val contentGap: Dp,
)

@Composable
fun rememberAuthFormLayoutMetrics(maxContentWidth: Dp): AuthFormLayoutMetrics {
    val viewportWidth = rememberViewportWidthDp()
    return remember(viewportWidth, maxContentWidth) {
        AuthFormLayoutMetrics(
            innerWidth = if (viewportWidth >= WebViewportBp.tsm) {
                minOf(AuthFormTokens.innerFormWidth, maxContentWidth)
            } else {
                maxContentWidth
            },
            primaryButtonFullWidth = viewportWidth < WebViewportBp.msm,
            titleSp = if (viewportWidth >= WebViewportBp.tsm) 26 else 20,
            contentGap = if (viewportWidth >= WebViewportBp.tsm) {
                AuthFormTokens.contentGapWide
            } else {
                AuthFormTokens.contentGap
            },
        )
    }
}

/**
 * Web [AuthLayout] `.container_auth` horizontal margin.
 * 15px (&lt;1000) / 20px (≥1000); ≥1280 auto + max-width — via [authContentMaxWidth] + center.
 */
val WindowWidth.authHorizontalMargin: Dp
    get() = when (this) {
        WindowWidth.Compact, WindowWidth.Medium -> 16.dp   // Web 15
        WindowWidth.Expanded -> 24.dp                        // Web 20
    }

/**
 * Web [AuthLayout] `.container_auth` vertical margin.
 * 15px (&lt;768) / 20px (≥768). (1600→40px omitted — no 4th WindowWidth bucket.)
 */
val WindowWidth.authVerticalMargin: Dp
    get() = when (this) {
        WindowWidth.Compact -> 16.dp   // Web 15
        WindowWidth.Medium, WindowWidth.Expanded -> 24.dp  // Web 20
    }

/** Web `.container_auth { max-width: 1200px }`. */
val WindowWidth.authContentMaxWidth: Dp
    get() = 1200.dp

/**
 * Web [AuthorizationView] aside: 100% (&lt;768) / 300px (≤1279).
 * Expanded phones/tablets stay in 300px band until ~1280.
 */
val WindowWidth.authContactAsideWidth: Dp?
    get() = when (this) {
        WindowWidth.Compact -> null
        WindowWidth.Medium, WindowWidth.Expanded -> 304.dp  // Web 300 → 8dp grid
    }

/**
 * Web [App.vue] `.p-toast`: Compact — full width + 20dp insets;
 * Medium/Expanded — top-right wrap.
 */
val WindowWidth.notificationToastAnchor: NotificationAnchor
    get() = when (this) {
        WindowWidth.Compact -> NotificationAnchor.Start
        WindowWidth.Medium, WindowWidth.Expanded -> NotificationAnchor.End
    }
