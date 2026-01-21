package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Status bar showing server state.
 *
 * Pure Compose implementation migrated in Phase 5.
 * Replaces Swing JBLabel status bar.
 *
 * @param serverState Current server state
 * @param modifier Optional modifier
 */
@Composable
fun StatusBar(
    serverState: MessageServerState,
    modifier: Modifier = Modifier
) {
    val statusText = when (serverState) {
        MessageServerState.Stopped -> "Message server stopped"
        MessageServerState.Running -> "Message server running"
        is MessageServerState.Error -> "Error: ${serverState.message}"
        MessageServerState.Starting -> "Message server starting"
        MessageServerState.Stopping -> "Message server stopping"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(IntelliJTheme.colors.background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = statusText,
            style = IntelliJTheme.typography.body.copy(
                fontSize = IntelliJTheme.typography.body.fontSize * 0.9f
            )
        )
    }
}
