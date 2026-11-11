package online.lds.nos.ui.map

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

@Composable
actual fun rememberTextShare(): TextShare {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    return remember(context, clipboard) {
        object : TextShare {
            override fun share(text: String) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Поделиться"))
            }

            override fun copy(text: String) {
                clipboard.setText(AnnotatedString(text))
            }
        }
    }
}
