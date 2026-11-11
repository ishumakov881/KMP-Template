package online.lds.nos.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * Platform shell chrome: mobile keeps Cupertino bottom-sheet scaffold + system bars;
 * desktop/web use a plain Cupertino scaffold (cupertino `systemBars` crashes on JVM).
 */
@Composable
expect fun nosTopBarWindowInsets(): WindowInsets

@Composable
expect fun nosNavBarWindowInsets(): WindowInsets

@Composable
expect fun NosAppScaffold(
    actionsSheetOpen: Boolean,
    onActionsSheetChange: (Boolean) -> Unit,
    sheetContent: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
)
