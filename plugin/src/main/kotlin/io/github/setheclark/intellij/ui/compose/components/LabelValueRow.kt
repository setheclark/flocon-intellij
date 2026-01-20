package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
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
 * A reusable row component displaying a label and value pair.
 *
 * Used for displaying key-value information in a consistent format.
 *
 * @param label The label text (e.g., "Method", "Status")
 * @param value The value text (e.g., "GET", "200 OK")
 * @param modifier Optional modifier for the row
 */
@Composable
fun LabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = IntelliJTheme.typography.body.copy(
                color = IntelliJTheme.colors.foreground.copy(alpha = 0.6f)
            ),
            modifier = Modifier.width(100.dp)
        )

        Text(
            text = value,
            style = IntelliJTheme.typography.body,
            modifier = Modifier.weight(1f)
        )
    }
}
