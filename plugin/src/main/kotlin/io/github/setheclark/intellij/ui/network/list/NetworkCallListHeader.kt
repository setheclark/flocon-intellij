package io.github.setheclark.intellij.ui.network.list

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.stringResource
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Cursor

@Composable
internal fun CallListHeader(
    visibleColumns: List<NetworkCallListColumn>,
    columnWidths: SnapshotStateList<Float>,
    containerWidthDp: Float,
    sortAscending: Boolean,
    onToggleSort: () -> Unit,
) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visibleColumns.forEachIndexed { index, col ->
            val isLast = index == visibleColumns.lastIndex
            val isTime = col == NetworkCallListColumn.TIME
            val widthModifier = if (isLast) Modifier.weight(1f) else Modifier.width(columnWidths[index].dp)
            val colModifier = widthModifier
                .then(
                    if (isTime) {
                        Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                if (waitForUpOrCancellation() != null) {
                                    onToggleSort()
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(start = 2.dp, end = 4.dp)
            Box(modifier = colModifier) {
                if (isTime) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = col.displayName,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            key = if (sortAscending) {
                                AllIconsKeys.General.ArrowUp
                            } else {
                                AllIconsKeys.General.ArrowDown
                            },
                            contentDescription = stringResource(if (sortAscending) "column.sort.ascending" else "column.sort.descending"),
                        )
                    }
                } else {
                    Text(
                        text = col.displayName,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!isLast) {
                ColumnResizeHandle(
                    onDelta = { dragPx ->
                        val dragDp = with(density) { dragPx.toDp().value }
                        redistributeColumnWidth(columnWidths, visibleColumns, index, dragDp, containerWidthDp)
                    },
                )
            }
        }
    }
}

@Composable
private fun ColumnResizeHandle(onDelta: (Float) -> Unit) {
    val resizeCursor = remember { PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)) }
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .pointerHoverIcon(resizeCursor)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onDelta(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Divider(orientation = Orientation.Vertical, modifier = Modifier.fillMaxHeight())
    }
}

internal fun redistributeColumnWidth(
    columnWidths: SnapshotStateList<Float>,
    visibleColumns: List<NetworkCallListColumn>,
    draggedIndex: Int,
    dragDp: Float,
    containerWidthDp: Float,
) {
    val minCurrent = visibleColumns[draggedIndex].minWidth.toFloat()
    val maxShrink = (columnWidths[draggedIndex] - minCurrent).coerceAtLeast(0f)

    // Compute the last column's current effective width.
    val numHandles = visibleColumns.lastIndex
    val nonLastSum = columnWidths.take(visibleColumns.lastIndex).sum()
    val lastColWidth = containerWidthDp - 8f - numHandles * 4f - nonLastSum

    // Total space that can be redistributed across ALL subsequent columns
    // (both fixed non-last and the weight(1f) last column).
    val reducibleFixed = (draggedIndex + 1 until visibleColumns.lastIndex)
        .sumOf { j ->
            (columnWidths[j] - visibleColumns[j].minWidth.toFloat())
                .coerceAtLeast(0f).toDouble()
        }.toFloat()
    val reducibleLast = (lastColWidth - visibleColumns.last().minWidth.toFloat())
        .coerceAtLeast(0f)
    val totalReducible = reducibleFixed + reducibleLast

    // Clamp: can't grow more than subsequent cols can absorb;
    // can't shrink past own minimum.
    val actualDelta = dragDp.coerceIn(-maxShrink, totalReducible)
    columnWidths[draggedIndex] += actualDelta

    if (actualDelta > 0f && totalReducible > 0f) {
        // Growing column i: shrink subsequent columns proportionally
        // to their available reducible space (current − min).
        // Columns already at min contribute nothing; the last weight(1f)
        // column absorbs whatever's left automatically.
        for (j in (draggedIndex + 1) until visibleColumns.lastIndex) {
            val reducible = (columnWidths[j] - visibleColumns[j].minWidth.toFloat())
                .coerceAtLeast(0f)
            columnWidths[j] = (columnWidths[j] - actualDelta * (reducible / totalReducible))
                .coerceAtLeast(visibleColumns[j].minWidth.toFloat())
        }
    } else if (actualDelta < 0f) {
        // Shrinking column i: grow subsequent columns proportionally
        // to their minWidth so that even columns compressed to their
        // minimum re-expand together rather than only the last column
        // growing. The last (weight(1f)) column gets its share implicitly.
        val totalMinSubsequent = ((draggedIndex + 1) until visibleColumns.size)
            .sumOf { j -> visibleColumns[j].minWidth.toDouble() }.toFloat()
        if (totalMinSubsequent > 0f) {
            for (j in (draggedIndex + 1) until visibleColumns.lastIndex) {
                val share = visibleColumns[j].minWidth.toFloat() / totalMinSubsequent
                columnWidths[j] -= actualDelta * share // actualDelta < 0 → grows
            }
        }
    }
}
