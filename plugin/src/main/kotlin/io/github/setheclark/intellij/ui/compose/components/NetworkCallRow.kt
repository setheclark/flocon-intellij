package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import io.github.setheclark.intellij.ui.compose.util.NetworkFormatters
import io.github.setheclark.intellij.ui.network.list.NetworkCallListItem
import org.jetbrains.jewel.ui.component.Text

/**
 * A single row in the network call list table.
 *
 * Displays: TIME | NAME | STATUS | METHOD | URL | DURATION | SIZE
 *
 * @param call Network call data
 * @param isSelected Whether this row is currently selected
 * @param onClick Callback when row is clicked
 * @param onDoubleClick Callback when row is double-clicked
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkCallRow(
    call: NetworkCallListItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) IntelliJTheme.colors.selection
                else IntelliJTheme.colors.background
            )
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // TIME column (75dp)
        Text(
            text = NetworkFormatters.formatTime(call.startTime),
            style = IntelliJTheme.typography.body,
            modifier = Modifier.width(75.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        // NAME column (150dp)
        Text(
            text = call.name,
            style = IntelliJTheme.typography.body,
            modifier = Modifier.width(150.dp).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // STATUS column (50dp)
        Text(
            text = call.status?.toString() ?: "...",
            style = IntelliJTheme.typography.body.copy(
                color = NetworkFormatters.getStatusColor(call.status)
            ),
            modifier = Modifier.width(50.dp).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        // METHOD column (50dp)
        Text(
            text = call.method,
            style = IntelliJTheme.typography.body.copy(
                color = NetworkFormatters.getMethodColor(call.method)
            ),
            modifier = Modifier.width(50.dp).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        // URL column (flexible, takes remaining space)
        Text(
            text = call.url,
            style = IntelliJTheme.typography.body,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // DURATION column (70dp)
        Text(
            text = NetworkFormatters.formatDuration(call.duration),
            style = IntelliJTheme.typography.body,
            modifier = Modifier.width(70.dp).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )

        // SIZE column (70dp)
        Text(
            text = NetworkFormatters.formatSize(call.size),
            style = IntelliJTheme.typography.body,
            modifier = Modifier.width(70.dp).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
