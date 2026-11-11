package online.lds.nos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi

private val ZeroInsets = WindowInsets(0, 0, 0, 0)

@Composable
actual fun nosTopBarWindowInsets(): WindowInsets = ZeroInsets

@Composable
actual fun nosNavBarWindowInsets(): WindowInsets = ZeroInsets

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun NosAppScaffold(
    actionsSheetOpen: Boolean,
    onActionsSheetChange: (Boolean) -> Unit,
    sheetContent: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    CupertinoScaffold(
        contentWindowInsets = ZeroInsets,
        topBar = topBar,
        bottomBar = bottomBar,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            content(padding)
            if (actionsSheetOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f),
                ) {
                    sheetContent()
                }
            }
        }
    }
}
