package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import io.github.setheclark.intellij.ui.network.detail.common.HeadersTablePanel

/**
 * Compose wrapper for Swing HeadersTablePanel.
 *
 * Temporary wrapper during migration (Phase 2.2-3.1).
 * Will be replaced with pure Compose HeadersTable in Phase 3.1.
 *
 * @param headers Map of header key-value pairs to display
 * @param modifier Optional modifier
 */
@Composable
fun HeadersTableView(
    headers: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val panel = remember { HeadersTablePanel() }

    DisposableEffect(headers) {
        panel.showHeaders(headers)
        onDispose { }
    }

    SwingPanel(
        factory = { panel },
        modifier = modifier
    )
}
