package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * A reusable search text field component.
 *
 * @param value The current text value
 * @param onValueChange Callback when text changes
 * @param placeholder Placeholder text when empty
 * @param modifier Optional modifier for the text field
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = IntelliJTheme.typography.body.copy(
            color = IntelliJTheme.colors.foreground
        ),
        cursorBrush = SolidColor(IntelliJTheme.colors.foreground),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .background(IntelliJTheme.colors.background)
                    .border(1.dp, IntelliJTheme.colors.border)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = IntelliJTheme.typography.body.copy(
                            color = IntelliJTheme.colors.foreground.copy(alpha = 0.5f)
                        )
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier.width(200.dp)
    )
}
