package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * A single row in the headers table displaying name-value pair.
 *
 * @param name Header name (displayed in bold, gray)
 * @param value Header value
 * @param isStriped Whether this row should have alternate background color
 * @param nameColumnWidth Width of the name column
 * @param modifier Optional modifier
 */
@Composable
fun HeaderRow(
    name: String,
    value: String,
    isStriped: Boolean,
    nameColumnWidth: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isStriped) IntelliJTheme.colors.background.copy(alpha = 0.95f)
                else IntelliJTheme.colors.background
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        // Name column (bold, gray)
        Text(
            text = name,
            style = IntelliJTheme.typography.body.copy(
                fontWeight = FontWeight.Bold,
                color = IntelliJTheme.colors.foreground.copy(alpha = 0.6f)
            ),
            modifier = Modifier.width(nameColumnWidth.dp)
        )

        // Value column
        Text(
            text = value,
            style = IntelliJTheme.typography.body.copy(
                color = IntelliJTheme.colors.foreground
            ),
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
    }
}
