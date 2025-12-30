package io.github.setheclark.intellij.ui.network.list

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.UiCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableRowSorter

@Inject
class NetworkCallListPanel(
    @param:UiCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkCallListViewModel,
) : JPanel(BorderLayout()) {

    private val tableModel = NetworkCallTableModel()
    private val table = JBTable(tableModel)
    private var scrollPane: JBScrollPane? = null

    private var previousCallCount = 0

    init {
        setupTable()
        observeState()
    }

    private fun setupTable() {
        // Set up row sorter for column sorting
        val sorter = TableRowSorter(tableModel).apply {
            // Set comparators from Column enum
            NetworkCallListColumn.entries.forEachIndexed { index, column ->
                column.comparator?.let { setComparator(index, it) }
            }
            // Default sort by time ascending
            sortKeys = listOf(RowSorter.SortKey(NetworkCallListColumn.TIME.ordinal, SortOrder.ASCENDING))
        }

        table.apply {
            setShowGrid(false)
            rowHeight = JBUI.scale(24)
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            rowSorter = sorter

            // Configure columns from Column enum
            NetworkCallListColumn.entries.forEach { column ->
                columnModel.getColumn(column.ordinal).apply {
                    preferredWidth = column.preferredWidth
                    cellRenderer = column.renderer
                }
            }

            // Selection listener - convert view row to model row for proper selection
            selectionModel.addListSelectionListener { e ->
                if (!e.valueIsAdjusting) {
                    val viewRow = selectedRow
                    if (viewRow >= 0) {
                        // User selected an item - disable auto-scrolling
                        disableAutoScroll()
                        val modelRow = convertRowIndexToModel(viewRow)
                        val selectedCall = tableModel.calls.getOrNull(modelRow)
                        if (selectedCall != null) {
                            viewModel.dispatch(NetworkCallListIntent.SelectCall(selectedCall.callId))
                        } else {
                            clearCallSelection()
                        }
                    } else {
                        clearCallSelection()
                    }
                }
            }

            // Mouse listener - double-click to deselect
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val clickedRow = rowAtPoint(e.point)
                        if (clickedRow >= 0 && clickedRow == selectedRow) {
                            // Double-clicked the selected row - deselect
                            clearSelection()
                            clearCallSelection()
                        }
                    }
                }
            })

            // Key listener - press Escape to deselect
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ESCAPE) {
                        clearSelection()
                        clearCallSelection()
                    }
                }
            })
        }

        // Use JBScrollPane to properly handle the table header (keeps it pinned at top)
        scrollPane = JBScrollPane(table).also {
            // Track manual scrolling by user - disable auto-scroll
            it.verticalScrollBar.addAdjustmentListener { e ->
                if (e.valueIsAdjusting) {
                    // User is actively dragging the scrollbar
                    disableAutoScroll()
                }
            }

            // Also track mouse wheel scrolling
            it.addMouseWheelListener { disableAutoScroll() }
        }

        add(scrollPane, BorderLayout.CENTER)
    }

    private fun clearCallSelection() {
        viewModel.dispatch(NetworkCallListIntent.ClearCallSelection)
    }

    private fun disableAutoScroll() {
        viewModel.dispatch(NetworkCallListIntent.DisableAutoScroll)
    }


    private fun observeState() {
        // Observe filtered calls and auto-scroll state from ViewModel
        scope.launch {
            viewModel.state.collectLatest { (calls, autoScrollEnabled) ->
                val newCallCount = calls.size
                val hasNewItems = newCallCount > previousCallCount

                // Preserve selection by call ID
                val selectedCallId = getSelectedCallId()
                tableModel.updateCalls(calls)

                // Restore selection if the call is still in the list
                if (selectedCallId != null) {
                    restoreSelection(selectedCallId)
                }

                // Auto-scroll to show new entries if enabled
                if (hasNewItems && autoScrollEnabled && isSortedByTime()) {
                    scrollToShowNewEntries()
                }

                previousCallCount = newCallCount
            }
        }
    }

    /**
     * Check if the table is currently sorted by the Time column.
     */
    private fun isSortedByTime(): Boolean {
        val sortKeys = table.rowSorter?.sortKeys ?: return false
        if (sortKeys.isEmpty()) return false
        return sortKeys[0].column == NetworkCallListColumn.TIME.ordinal
    }

    /**
     * Check if the table is sorted in ascending order.
     */
    private fun isSortedAscending(): Boolean {
        val sortKeys = table.rowSorter?.sortKeys ?: return true
        if (sortKeys.isEmpty()) return true
        return sortKeys[0].sortOrder == SortOrder.ASCENDING
    }

    /**
     * Scroll to show new entries based on sort order.
     * Ascending: scroll to bottom (newest at bottom)
     * Descending: scroll to top (newest at top)
     */
    private fun scrollToShowNewEntries() {
        if (table.rowCount == 0) return

        if (isSortedAscending()) {
            // Scroll to bottom for ascending (newest at bottom)
            table.scrollRectToVisible(table.getCellRect(table.rowCount - 1, 0, true))
        } else {
            // Scroll to top for descending (newest at top)
            // Use scrollBar directly for more reliable behavior
            scrollPane?.verticalScrollBar?.value = 0
        }
    }

    private fun getSelectedCallId(): String? {
        val viewRow = table.selectedRow
        if (viewRow < 0) return null
        val modelRow = table.convertRowIndexToModel(viewRow)
        return tableModel.calls.getOrNull(modelRow)?.callId
    }

    private fun restoreSelection(callId: String) {
        val modelRow = tableModel.calls.indexOfFirst { it.callId == callId }
        if (modelRow >= 0) {
            val viewRow = table.convertRowIndexToView(modelRow)
            if (viewRow >= 0) {
                table.setRowSelectionInterval(viewRow, viewRow)
            }
        }
    }
}

/**
 * Table model for the network call list.
 * Uses [Column] enum to define structure; returns raw values for proper sorting.
 */
class NetworkCallTableModel : AbstractTableModel() {

    var calls: List<NetworkCallListItem> = emptyList()
        private set

    fun updateCalls(newCalls: List<NetworkCallListItem>) {
        calls = newCalls
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = calls.size
    override fun getColumnCount(): Int = NetworkCallListColumn.entries.size
    override fun getColumnName(column: Int): String = NetworkCallListColumn.entries[column].displayName
    override fun getColumnClass(columnIndex: Int): Class<*> = NetworkCallListColumn.entries[columnIndex].valueClass

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val call = calls.getOrNull(rowIndex) ?: return null
        return NetworkCallListColumn.entries[columnIndex].getValue(call)
    }
}
