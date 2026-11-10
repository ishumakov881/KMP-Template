package net.lds.online.ui.base

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import net.lds.online.components.ErrorView

private enum class BaseContentPhase { Loading, Empty, Error, Success }

private fun <T> UiState<T>.phase(): BaseContentPhase = when (this) {
    is UiState.Loading -> BaseContentPhase.Loading
    is UiState.Empty -> BaseContentPhase.Empty
    is UiState.Error -> BaseContentPhase.Error
    is UiState.Success -> BaseContentPhase.Success
}

@Composable
fun <T> BaseScreenLayout(
    state: ScreenState<T>,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerContent: @Composable () -> Unit = { ShimmerScreen() },
    emptyContent: @Composable () -> Unit = { EmptyView() },
    successContent: @Composable (T) -> Unit,
) {
    Crossfade(
        targetState = state.contentState.phase(),
        label = "BaseStateTransition",
        modifier = modifier.fillMaxSize(),
    ) { phase ->
        when (phase) {
            BaseContentPhase.Loading -> shimmerContent()
            BaseContentPhase.Empty -> emptyContent()
            BaseContentPhase.Error -> ErrorView(
                message = (state.contentState as? UiState.Error)?.message.orEmpty(),
                onRetry = onRetry,
            )
            BaseContentPhase.Success -> {
                val success = state.contentState as? UiState.Success<T>
                if (success != null) {
                    Box(Modifier.fillMaxSize()) {
                        successContent(success.data)
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CupertinoActivityIndicator()
    }
}

@Composable
fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Empty state placeholder — no data to show.
    }
}

@Composable
fun ScreenRefreshTemplate(
    modifier: Modifier,
    error: String?,
    content: @Composable (() -> Unit)? = null,
    onRefresh: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content?.invoke()
        error?.let {
            ErrorView(
                title = it,
                modifier = Modifier.align(Alignment.Center),
                onRetry = onRefresh,
            )
        }
    }
}
