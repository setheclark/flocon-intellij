package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Compose headers table with keyboard support.
 *
 * Replaces Swing HeadersTablePanel in Phase 3.1.
 * Features:
 * - Two-column layout (Name | Value)
 * - Striped rows
 * - Cmd/Ctrl+C to copy selected value
 * - Auto-calculated name column width
 *
 * Note: Context menu support omitted for now due to Compose API limitations.
 * Can be added later using Swing interop if needed.
 *
 * @param headers Map of header name-value pairs
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeadersTable(
    headers: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (headers.isEmpty()) {
        // Empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No headers",
                style = IntelliJTheme.typography.body.copy(
                    color = IntelliJTheme.colors.foreground.copy(alpha = 0.5f)
                )
            )
        }
        return
    }

    // Sort headers alphabetically and calculate max name width
    val sortedHeaders = remember(headers) {
        headers.entries
            .sortedBy { it.key.lowercase() }
            .map { it.key to it.value }
    }

    // Calculate name column width (simplified - using character count)
    val maxNameLength = remember(sortedHeaders) {
        sortedHeaders.maxOfOrNull { it.first.length } ?: 0
    }
    val nameColumnWidth = (maxNameLength * 8).coerceIn(100, 300)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }

    Box(modifier = modifier.fillMaxSize()) {
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.C -> {
                                // Cmd/Ctrl+C to copy value
                                if (event.isMetaPressed || event.isCtrlPressed) {
                                    selectedIndex?.let { index ->
                                        val (_, value) = sortedHeaders.getOrNull(index) ?: return@let
                                        clipboardManager.setText(AnnotatedString(value))
                                    }
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            itemsIndexed(sortedHeaders) { index, (name, value) ->
                HeaderRow(
                    name = name,
                    value = value,
                    isStriped = index % 2 == 0,
                    nameColumnWidth = nameColumnWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIndex = index
                            focusRequester.requestFocus()
                        }
                )
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}
