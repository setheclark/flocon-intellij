package io.github.setheclark.intellij.ui.network.list

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.component.styling.LocalSelectableLazyColumnStyle

@Composable
fun NetworkCallList(
    listState: NetworkCallListState,
    selectedCallId: String?,
    visibleColumns: List<NetworkCallListColumn>,
    columnWidths: Map<String, Float>,
    onColumnWidthChange: (Map<String, Float>) -> Unit,
    onIntent: (NetworkCallListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onColumnChange by rememberUpdatedState(onColumnWidthChange)
    val columnWidthsList: SnapshotStateList<Float> = remember(visibleColumns) {
        visibleColumns.map { col ->
            columnWidths[col.name] ?: col.preferredWidth.toFloat()
        }.toMutableStateList()
    }

    LaunchedEffect(columnWidthsList) {
        snapshotFlow { columnWidthsList.toList() }
            .drop(1)
            .debounce(500)
            .collect { widths ->
                val map = visibleColumns.zip(widths).associate { (col, w) -> col.name to w }
                onColumnChange(map)
            }
    }

    var sortAscending by remember { mutableStateOf(true) }

    val sortedCalls = remember(listState.calls, sortAscending) {
        if (sortAscending) {
            listState.calls.sortedBy { it.startTime }
        } else {
            listState.calls.sortedByDescending { it.startTime }
        }
    }

    val lazyListState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    val currentOnIntent by rememberUpdatedState(onIntent)

    // Scroll to newest entry when auto-scroll is active
    LaunchedEffect(sortedCalls.size, listState.autoScrollEnabled, sortAscending) {
        if (listState.autoScrollEnabled && sortedCalls.isNotEmpty()) {
            isAutoScrolling = true
            try {
                val target = if (sortAscending) sortedCalls.size - 1 else 0
                lazyListState.scrollToItem(target)
            } finally {
                isAutoScrolling = false
            }
        }
    }

    // Disable auto-scroll when user scrolls
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .filter { it }
            .collect {
                if (!isAutoScrolling) {
                    currentOnIntent(NetworkCallListIntent.DisableAutoScroll)
                }
            }
    }

    val density = LocalDensity.current
    var containerWidthDp by remember { mutableFloatStateOf(0f) }

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .onSizeChanged { size ->
                containerWidthDp = with(density) { size.width.toDp().value }
            }
            .onFocusChanged { isFocused = it.hasFocus }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onIntent(NetworkCallListIntent.ClearCallSelection)
                    true
                } else {
                    false
                }
            },
    ) {
        CallListHeader(
            visibleColumns = visibleColumns,
            columnWidths = columnWidthsList,
            containerWidthDp = containerWidthDp,
            sortAscending = sortAscending,
            onToggleSort = { sortAscending = !sortAscending },
        )
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        @Suppress("DEPRECATION")
        VerticallyScrollableContainer(
            scrollState = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(sortedCalls, key = { it.callId }) { call ->
                    CallListRow(
                        call = call,
                        isSelected = call.callId == selectedCallId,
                        isActive = isFocused,
                        visibleColumns = visibleColumns,
                        columnWidths = columnWidthsList,
                        onSelect = {
                            onIntent(NetworkCallListIntent.SelectCall(call.callId))
                            focusRequester.requestFocus()
                        },
                        onOpen = {
                            onIntent(NetworkCallListIntent.OpenCallInTab(call.callId, call.name))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CallListRow(
    call: NetworkCallListItem,
    isSelected: Boolean,
    isActive: Boolean,
    visibleColumns: List<NetworkCallListColumn>,
    columnWidths: SnapshotStateList<Float>,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val style = LocalSelectableLazyColumnStyle.current
    val itemState = ListItemState(isSelected = isSelected, isActive = isActive)
    val bgColor by style.simpleListItemStyle.colors.backgroundFor(itemState)
    val contentColor by style.simpleListItemStyle.colors.contentFor(itemState)
    val isDark = JewelTheme.isDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(style.itemHeight)
            .background(bgColor)
            .pointerInput(call.callId) {
                detectTapGestures(
                    onPress = { onSelect() },
                    onDoubleTap = { onOpen() },
                )
            }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visibleColumns.forEachIndexed { index, col ->
            val isLast = index == visibleColumns.lastIndex
            val text = col.formatForDisplay(call)
            val color = if (!isSelected) col.colorForDisplay(call, isDark) ?: contentColor else contentColor
            val widthModifier = if (isLast) Modifier.weight(1f) else Modifier.width(columnWidths[index].dp)
            Text(
                text = text,
                color = color,
                modifier = widthModifier.padding(start = 2.dp, end = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isLast) {
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

private fun NetworkCallListColumn.colorForDisplay(call: NetworkCallListItem, isDark: Boolean): Color? = when (this) {
    NetworkCallListColumn.STATUS -> call.status?.let { statusColor(it, isDark) }
    NetworkCallListColumn.METHOD -> methodColor(call.method, isDark)
    else -> null
}
