package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import io.github.setheclark.intellij.ui.network.list.NetworkCallListItem
import org.jetbrains.jewel.ui.component.Text

/**
 * Sortable network call table with selection and auto-scroll.
 *
 * Features:
 * - Column headers (click to sort)
 * - Single selection (click to select, double-click to deselect)
 * - Keyboard navigation (Escape to deselect)
 * - Auto-scroll to newest entries
 *
 * @param calls List of network calls to display
 * @param selectedCallId Currently selected call ID
 * @param autoScrollEnabled Whether auto-scroll is enabled
 * @param onSelectCall Callback when a call is selected
 * @param onClearSelection Callback when selection is cleared
 * @param onDisableAutoScroll Callback when user manually scrolls
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkCallTable(
    calls: List<NetworkCallListItem>,
    selectedCallId: String?,
    autoScrollEnabled: Boolean,
    onSelectCall: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDisableAutoScroll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sort column and direction
    var sortColumn by remember { mutableStateOf(SortColumn.TIME) }
    var sortAscending by remember { mutableStateOf(true) }

    // Sort calls based on current sort settings
    val sortedCalls = remember(calls, sortColumn, sortAscending) {
        val sorted = when (sortColumn) {
            SortColumn.TIME -> calls.sortedBy { it.startTime }
            SortColumn.NAME -> calls.sortedBy { it.name }
            SortColumn.STATUS -> calls.sortedBy { it.status ?: Int.MAX_VALUE }
            SortColumn.METHOD -> calls.sortedBy { it.method }
            SortColumn.URL -> calls.sortedBy { it.url }
            SortColumn.DURATION -> calls.sortedBy { it.duration ?: Double.MAX_VALUE }
            SortColumn.SIZE -> calls.sortedBy { it.size ?: Long.MAX_VALUE }
        }
        if (sortAscending) sorted else sorted.reversed()
    }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var userHasScrolled by remember { mutableStateOf(false) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Request focus initially so keyboard shortcuts work
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Reset userHasScrolled flag when autoscroll is enabled
    LaunchedEffect(autoScrollEnabled) {
        if (autoScrollEnabled) {
            userHasScrolled = false
        }
    }

    // Auto-scroll to newest entries when enabled
    LaunchedEffect(sortedCalls.size, autoScrollEnabled, sortColumn, sortAscending) {
        if (autoScrollEnabled && sortColumn == SortColumn.TIME && sortedCalls.isNotEmpty() && !userHasScrolled) {
            isProgrammaticScroll = true
            val targetIndex = if (sortAscending) sortedCalls.size - 1 else 0
            listState.animateScrollToItem(targetIndex)
            isProgrammaticScroll = false
        }
    }

    // Detect manual scrolling (but ignore programmatic scrolling from autoscroll)
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isProgrammaticScroll) {
            userHasScrolled = true
            onDisableAutoScroll()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header row
        NetworkCallTableHeader(
            sortColumn = sortColumn,
            sortAscending = sortAscending,
            onSort = { column ->
                if (sortColumn == column) {
                    sortAscending = !sortAscending
                } else {
                    sortColumn = column
                    sortAscending = true
                }
                userHasScrolled = false
            }
        )

        // Data rows
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            onClearSelection()
                            true
                        } else false
                    }
            ) {
                itemsIndexed(sortedCalls, key = { _, call -> call.callId }) { _, call ->
                    NetworkCallRow(
                        call = call,
                        isSelected = call.callId == selectedCallId,
                        onClick = {
                            userHasScrolled = true
                            onDisableAutoScroll()
                            onSelectCall(call.callId)
                            focusRequester.requestFocus()
                        },
                        onDoubleClick = {
                            if (call.callId == selectedCallId) {
                                onClearSelection()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
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
}

/**
 * Table header with sortable columns
 */
@Composable
private fun NetworkCallTableHeader(
    sortColumn: SortColumn,
    sortAscending: Boolean,
    onSort: (SortColumn) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IntelliJTheme.colors.background.copy(alpha = 0.95f))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        HeaderCell("Time", 75.dp, SortColumn.TIME, sortColumn, sortAscending, onSort)
        HeaderCell("Name", 150.dp, SortColumn.NAME, sortColumn, sortAscending, onSort)
        HeaderCell("Status", 50.dp, SortColumn.STATUS, sortColumn, sortAscending, onSort)
        HeaderCell("Method", 50.dp, SortColumn.METHOD, sortColumn, sortAscending, onSort)
        HeaderCell("URL", null, SortColumn.URL, sortColumn, sortAscending, onSort, modifier = Modifier.weight(1f))
        HeaderCell("Duration", 70.dp, SortColumn.DURATION, sortColumn, sortAscending, onSort)
        HeaderCell("Size", 70.dp, SortColumn.SIZE, sortColumn, sortAscending, onSort)
    }
}

@Composable
private fun HeaderCell(
    label: String,
    width: androidx.compose.ui.unit.Dp?,
    column: SortColumn,
    sortColumn: SortColumn,
    sortAscending: Boolean,
    onSort: (SortColumn) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSorted = sortColumn == column
    val sortIndicator = if (isSorted) {
        if (sortAscending) " ▲" else " ▼"
    } else ""

    Text(
        text = "$label$sortIndicator",
        style = IntelliJTheme.typography.body.copy(
            fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Normal
        ),
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .clickable { onSort(column) }
            .padding(start = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Clip
    )
}

/**
 * Sort columns for the network call table
 */
enum class SortColumn {
    TIME,
    NAME,
    STATUS,
    METHOD,
    URL,
    DURATION,
    SIZE
}
