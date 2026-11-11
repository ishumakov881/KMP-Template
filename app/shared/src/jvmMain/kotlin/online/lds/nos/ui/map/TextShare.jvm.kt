package online.lds.nos.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberTextShare(): TextShare {
    val clipboard = LocalClipboardManager.current
    return remember(clipboard) {
        object : TextShare {
            override fun share(text: String) {
                // Desktop: no system share sheet — copy is the useful path.
                copy(text)
            }

            override fun copy(text: String) {
                clipboard.setText(AnnotatedString(text))
                runCatching {
                    Toolkit.getDefaultToolkit().systemClipboard
                        .setContents(StringSelection(text), null)
                }
            }
        }
    }
}
