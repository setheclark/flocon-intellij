package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * A reusable section component with a title and content area.
 *
 * Used for grouping related information under a bold section header.
 *
 * @param title The section title (displayed in bold)
 * @param modifier Optional modifier for the section
 * @param content The content to display in the section
 */
@Composable
fun InfoSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = IntelliJTheme.typography.body.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        content()
    }
}
