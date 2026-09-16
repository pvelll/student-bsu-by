package github.alexzhirkevich.studentbsuby.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun DefaultTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textInputModifier : Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    // The text and the cursor position are owned by the field itself. The screens keep
    // the text in a view model state that is updated asynchronously (through the event
    // handlers on a background dispatcher), so driving BasicTextField with that String
    // directly reset the selection on every round trip and the cursor jumped while typing.
    val state = remember { TextInputState(value) }
    state.reconcile(value)

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically
    ) {

        leadingIcon?.let {
            Box(
                modifier = Modifier.padding(
                    top = 10.dp,
                    start = 15.dp,
                    bottom = 10.dp,
                )
            ) {
                it.invoke()
            }
        }
        Box(modifier = Modifier
            .weight(1f)
            .padding(10.dp)
        ) {

            BasicTextField(
                value = state.textFieldValue,
                onValueChange = { newValue ->
                    if (state.update(newValue)) {
                        onValueChange(newValue.text)
                    }
                },
                enabled = enabled,
                readOnly = readOnly,
                singleLine = singleLine,
                textStyle = textStyle,
                visualTransformation = visualTransformation,
                keyboardActions = keyboardActions,
                keyboardOptions = keyboardOptions,
                maxLines = maxLines,
                interactionSource = interactionSource,
                modifier = textInputModifier.fillMaxWidth(),
                cursorBrush = SolidColor(MaterialTheme.colors.onBackground)
            )

            if (state.textFieldValue.text.isEmpty()) {
                placeholder?.invoke()
            }
        }
        trailingIcon?.let {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .padding(
                        top = 10.dp,
                        bottom = 10.dp,
                        end = 15.dp
                    )

            ) {

                it.invoke()
            }
        }
    }
}

/**
 * Local source of truth of a text field whose text is mirrored in an asynchronously
 * updated external state.
 *
 * Texts sent to the owner via `onValueChange` are remembered as pending. An external value
 * equal to a pending text is an echo of a local edit and is ignored (newer local edits may
 * still be in flight); any other external value is a real programmatic change (prefilled
 * credentials, recognized captcha, cleared search) and replaces the local text.
 */
private class TextInputState(initial: String) {

    var textFieldValue by mutableStateOf(
        TextFieldValue(initial, selection = TextRange(initial.length))
    )
        private set

    private val pending = ArrayDeque<String>()

    fun update(newValue: TextFieldValue): Boolean {
        val textChanged = newValue.text != textFieldValue.text
        textFieldValue = newValue
        if (textChanged) {
            pending.addLast(newValue.text)
        }
        return textChanged
    }

    fun reconcile(external: String) {
        if (external == textFieldValue.text) {
            pending.clear()
            return
        }
        val echoIndex = pending.indexOf(external)
        if (echoIndex >= 0) {
            repeat(echoIndex + 1) { pending.removeFirst() }
            return
        }
        pending.clear()
        textFieldValue = TextFieldValue(external, selection = TextRange(external.length))
    }
}
