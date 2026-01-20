package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Headers table view - now using pure Compose implementation.
 *
 * This was a Swing wrapper in Phase 2.2, replaced with pure Compose in Phase 3.1.
 *
 * @param headers Map of header key-value pairs to display
 * @param modifier Optional modifier
 */
@Composable
fun HeadersTableView(
    headers: Map<String, String>,
    modifier: Modifier = Modifier
) {
    // Delegate to pure Compose implementation
    HeadersTable(
        headers = headers,
        modifier = modifier
    )
}
