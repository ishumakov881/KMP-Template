package online.lds.nos.ui.map

import androidx.compose.runtime.Composable

/** Platform share sheet + clipboard for marker plain text. */
interface TextShare {
    fun share(text: String)
    fun copy(text: String)
}

@Composable
expect fun rememberTextShare(): TextShare
