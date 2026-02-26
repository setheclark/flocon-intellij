package io.github.setheclark.intellij.ui.network.list

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.settings.NetworkStorageSettingsState
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
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
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
        table.apply {
            setShowGrid(false)
            rowHeight = JBUI.scale(24)
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

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

            // Mouse listener - double-click to open call in tab
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val clickedRow = rowAtPoint(e.point)
                        if (clickedRow >= 0) {
                            val modelRow = convertRowIndexToModel(clickedRow)
                            val call = tableModel.calls.getOrNull(modelRow)
                            if (call != null) {
                                viewModel.dispatch(NetworkCallListIntent.OpenCallInTab(call.callId, call.name))
                            }
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

        // Apply initial column visibility from settings
        refreshColumns()
    }

    fun refreshColumns() {
        val settings = NetworkStorageSettingsState.getInstance()
        val visible = NetworkCallListColumn.entries.filter { col ->
            col == NetworkCallListColumn.TIME || col.name !in settings.hiddenColumns
        }
        if (visible == tableModel.visibleColumns) return
        tableModel.updateVisibleColumns(visible)
        // Re-apply renderers and widths (fireTableStructureChanged rebuilds the column model)
        visible.forEachIndexed { index, col ->
            table.columnModel.getColumn(index).apply {
                preferredWidth = col.preferredWidth
                cellRenderer = col.renderer
            }
        }
        // Re-create row sorter with updated column indices
        val sorter = TableRowSorter(tableModel).apply {
            visible.forEachIndexed { index, col ->
                col.comparator?.let { setComparator(index, it) }
            }
            sortKeys = listOf(RowSorter.SortKey(0, SortOrder.ASCENDING)) // TIME always at index 0
        }
        table.rowSorter = sorter
    }

    private fun clearCallSelection() {
        viewModel.dispatch(NetworkCallListIntent.ClearCallSelection)
    }

    private fun disableAutoScroll() {
        viewModel.dispatch(NetworkCallListIntent.DisableAutoScroll)
    }

    private fun observeState() {
        scope.launch {
            var previousAutoScrollEnabled = false
            viewModel.state.collectLatest { (calls, autoScrollEnabled) ->
                val newCallCount = calls.size
                val hasNewItems = newCallCount > previousCallCount
                val autoScrollJustEnabled = autoScrollEnabled && !previousAutoScrollEnabled

                // Preserve selection by call ID
                val selectedCallId = getSelectedCallId()
                tableModel.updateCalls(calls)

                // Restore selection if the call is still in the list
                if (selectedCallId != null) {
                    restoreSelection(selectedCallId)
                }

                // Auto-scroll when new items arrive or when auto-scroll is just enabled
                if ((hasNewItems || autoScrollJustEnabled) && autoScrollEnabled && isSortedByTime()) {
                    scrollToShowNewEntries()
                }

                previousCallCount = newCallCount
                previousAutoScrollEnabled = autoScrollEnabled
            }
        }
    }

    /**
     * Check if the table is currently sorted by the Time column.
     */
    private fun isSortedByTime(): Boolean {
        val sortKeys = table.rowSorter?.sortKeys ?: return false
        if (sortKeys.isEmpty()) return false
        val timeIndex = tableModel.visibleColumns.indexOf(NetworkCallListColumn.TIME)
        return sortKeys[0].column == timeIndex
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
 * Uses [NetworkCallListColumn] enum to define structure; returns raw values for proper sorting.
 * Only [visibleColumns] are exposed to the table; call [updateVisibleColumns] to change visibility.
 */
class NetworkCallTableModel : AbstractTableModel() {

    var calls: List<NetworkCallListItem> = emptyList()
        private set

    var visibleColumns: List<NetworkCallListColumn> = emptyList()
        private set

    fun updateCalls(newCalls: List<NetworkCallListItem>) {
        calls = newCalls
        fireTableDataChanged()
    }

    fun updateVisibleColumns(columns: List<NetworkCallListColumn>) {
        visibleColumns = columns
        fireTableStructureChanged()
    }

    override fun getRowCount(): Int = calls.size
    override fun getColumnCount(): Int = visibleColumns.size
    override fun getColumnName(column: Int): String = visibleColumns[column].displayName
    override fun getColumnClass(columnIndex: Int): Class<*> = visibleColumns[columnIndex].valueClass

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val call = calls.getOrNull(rowIndex) ?: return null
        return visibleColumns[columnIndex].getValue(call)
    }
}
