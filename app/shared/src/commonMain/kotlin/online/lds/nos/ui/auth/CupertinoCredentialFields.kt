package online.lds.nos.ui.auth

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.theme.CupertinoTheme

/**
 * Login / password fields for Android Autofill (system credential store).
 *
 * Not [io.github.alexzhirkevich.cupertino.CupertinoTextField]: upstream puts
 * `modifier` on DecorationBox, so Autofill never sees [ContentType] on the editable.
 */

@Composable
fun CupertinoLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = "Логин",
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    CredentialBasicField(
        value = value,
        onValueChange = onValueChange,
        contentType = ContentType.Username + ContentType.EmailAddress,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder,
        visualTransformation = VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        trailingIcon = trailingIcon,
        isPassword = false,
    )
}

@Composable
fun CupertinoPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = "Пароль",
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    CredentialBasicField(
        value = value,
        onValueChange = onValueChange,
        contentType = ContentType.Password,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        trailingIcon = trailingIcon,
        isPassword = true,
    )
}

@Composable
private fun CredentialBasicField(
    value: String,
    onValueChange: (String) -> Unit,
    contentType: ContentType,
    modifier: Modifier,
    enabled: Boolean,
    placeholder: String?,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    interactionSource: MutableInteractionSource,
    trailingIcon: @Composable (() -> Unit)?,
    isPassword: Boolean,
) {
    val body = CupertinoTheme.typography.body
    val label = CupertinoTheme.colorScheme.label
    val secondary = CupertinoTheme.colorScheme.secondaryLabel
    val accent = CupertinoTheme.colorScheme.accent

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .semantics {
                this.contentType = contentType
                contentDataType = ContentDataType.Text
                if (isPassword) password()
            }
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp),
        enabled = enabled,
        textStyle = body.merge(TextStyle(color = label)),
        cursorBrush = SolidColor(accent),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = true,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    CupertinoText(text = placeholder, color = secondary, style = body)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.weight(1f)) { inner() }
                    trailingIcon?.invoke()
                }
            }
        },
    )
}
