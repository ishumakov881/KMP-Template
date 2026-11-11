package online.lds.nos.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.alexzhirkevich.cupertino.CupertinoBottomSheetScaffold
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi
import io.github.alexzhirkevich.cupertino.rememberCupertinoBottomSheetScaffoldState
import io.github.alexzhirkevich.cupertino.rememberCupertinoSheetState

@Composable
actual fun nosTopBarWindowInsets(): WindowInsets = WindowInsets.systemBars

@Composable
actual fun nosNavBarWindowInsets(): WindowInsets = WindowInsets.navigationBars

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
    val sheetState = rememberCupertinoSheetState()
    val scaffoldState = rememberCupertinoBottomSheetScaffoldState(sheetState)

    LaunchedEffect(actionsSheetOpen) {
        if (actionsSheetOpen) sheetState.show()
        else if (sheetState.isVisible) sheetState.hide()
    }
    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible && actionsSheetOpen) onActionsSheetChange(false)
    }

    CupertinoBottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = sheetContent,
        topBar = topBar,
        bottomBar = bottomBar,
        content = content,
    )
}
