package io.github.setheclark.intellij.ui.list

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import io.github.setheclark.intellij.services.FloconProjectService
import io.github.setheclark.intellij.services.NetworkCallEntry
import io.github.setheclark.intellij.services.NetworkFilter
import io.github.setheclark.intellij.services.StatusFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SortOrder
import javax.swing.SwingUtilities
import javax.swing.RowSorter
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

/**
 * Panel displaying the list of captured network calls in a table format.
 */
class NetworkCallListPanel(
    private val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val floconService = project.service<FloconProjectService>()

    private val tableModel = NetworkCallTableModel()
    private val table = JBTable(tableModel)
    private var scrollPane: JBScrollPane? = null

    // Track if user has interacted - stops auto-scroll
    private var userHasInteracted = false
    private var previousCallCount = 0

    init {
        setupTable()
        observeNetworkCalls()
    }

    private fun setupTable() {
        // Set up row sorter for column sorting
        val sorter = TableRowSorter(tableModel).apply {
            // Set comparators that use the raw sortable values from the model
            setComparator(COL_TIME) { a: Any?, b: Any? ->
                ((a as? Long) ?: 0L).compareTo((b as? Long) ?: 0L)
            }
            setComparator(COL_STATUS) { a: Any?, b: Any? ->
                val aVal = (a as? Int) ?: Int.MAX_VALUE
                val bVal = (b as? Int) ?: Int.MAX_VALUE
                aVal.compareTo(bVal)
            }
            setComparator(COL_DURATION) { a: Any?, b: Any? ->
                val aVal = (a as? Long) ?: Long.MAX_VALUE
                val bVal = (b as? Long) ?: Long.MAX_VALUE
                aVal.compareTo(bVal)
            }
            setComparator(COL_SIZE) { a: Any?, b: Any? ->
                val aVal = (a as? Long) ?: Long.MAX_VALUE
                val bVal = (b as? Long) ?: Long.MAX_VALUE
                aVal.compareTo(bVal)
            }
            // Default sort by time ascending
            sortKeys = listOf(RowSorter.SortKey(COL_TIME, SortOrder.ASCENDING))
        }

        table.apply {
            setShowGrid(false)
            rowHeight = JBUI.scale(24)
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            rowSorter = sorter

            // Column widths
            columnModel.apply {
                getColumn(0).preferredWidth = 100  // Time
                getColumn(1).preferredWidth = 50   // Status
                getColumn(2).preferredWidth = 60   // Method
                getColumn(3).preferredWidth = 400  // URL
                getColumn(4).preferredWidth = 80   // Duration
                getColumn(5).preferredWidth = 80   // Size
            }

            // Custom renderers
            columnModel.getColumn(COL_TIME).cellRenderer = TimeRenderer()
            columnModel.getColumn(COL_STATUS).cellRenderer = StatusCodeRenderer()
            columnModel.getColumn(COL_METHOD).cellRenderer = MethodRenderer()
            columnModel.getColumn(COL_DURATION).cellRenderer = DurationRenderer()
            columnModel.getColumn(COL_SIZE).cellRenderer = SizeRenderer()

            // Selection listener - convert view row to model row for proper selection
            selectionModel.addListSelectionListener { e ->
                if (!e.valueIsAdjusting) {
                    val viewRow = selectedRow
                    if (viewRow >= 0) {
                        // User selected an item - stop auto-scrolling
                        userHasInteracted = true
                        val modelRow = convertRowIndexToModel(viewRow)
                        if (modelRow >= 0 && modelRow < tableModel.calls.size) {
                            floconService.selectCall(tableModel.calls[modelRow])
                        } else {
                            floconService.selectCall(null)
                        }
                    } else {
                        floconService.selectCall(null)
                    }
                }
            }
        }

        // Use JBScrollPane to properly handle the table header (keeps it pinned at top)
        scrollPane = JBScrollPane(table)

        // Track manual scrolling by user
        scrollPane!!.verticalScrollBar.addAdjustmentListener { e ->
            if (e.valueIsAdjusting) {
                // User is actively dragging the scrollbar
                userHasInteracted = true
            }
        }

        // Also track mouse wheel scrolling
        scrollPane!!.addMouseWheelListener {
            userHasInteracted = true
        }

        add(scrollPane, BorderLayout.CENTER)
    }

    /**
     * Reset auto-scroll behavior. Called when the list is cleared.
     */
    fun resetAutoScroll() {
        userHasInteracted = false
        previousCallCount = 0
    }

    /**
     * Check if auto-scroll is currently enabled.
     */
    fun isAutoScrollEnabled(): Boolean = !userHasInteracted

    /**
     * Re-enable auto-scroll and immediately scroll to show latest entries.
     */
    fun enableAutoScroll() {
        userHasInteracted = false
        if (isSortedByTime()) {
            scrollToShowNewEntries()
        }
    }

    companion object {
        // Column indices
        private const val COL_TIME = 0
        private const val COL_STATUS = 1
        private const val COL_METHOD = 2
        private const val COL_URL = 3
        private const val COL_DURATION = 4
        private const val COL_SIZE = 5
    }

    private fun observeNetworkCalls() {
        scope.launch {
            combine(
                floconService.networkCalls,
                floconService.filter
            ) { calls, filter ->
                applyFilter(calls, filter)
            }.collectLatest { filteredCalls ->
                SwingUtilities.invokeLater {
                    val newCallCount = filteredCalls.size
                    val hasNewItems = newCallCount > previousCallCount

                    // Reset interaction flag if list was cleared
                    if (newCallCount == 0) {
                        userHasInteracted = false
                    }

                    // Preserve selection by call ID
                    val selectedCallId = getSelectedCallId()
                    tableModel.updateCalls(filteredCalls)

                    // Restore selection if the call is still in the list
                    if (selectedCallId != null) {
                        restoreSelection(selectedCallId)
                    }

                    // Auto-scroll to show new entries if user hasn't interacted
                    if (hasNewItems && !userHasInteracted && isSortedByTime()) {
                        scrollToShowNewEntries()
                    }

                    previousCallCount = newCallCount
                }
            }
        }
    }

    /**
     * Check if the table is currently sorted by the Time column.
     */
    private fun isSortedByTime(): Boolean {
        val sortKeys = table.rowSorter?.sortKeys ?: return false
        if (sortKeys.isEmpty()) return false
        return sortKeys[0].column == COL_TIME
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
        return tableModel.calls.getOrNull(modelRow)?.id
    }

    private fun restoreSelection(callId: String) {
        val modelRow = tableModel.calls.indexOfFirst { it.id == callId }
        if (modelRow >= 0) {
            val viewRow = table.convertRowIndexToView(modelRow)
            if (viewRow >= 0) {
                table.setRowSelectionInterval(viewRow, viewRow)
            }
        }
    }

    /**
     * Check if a network call matches the search text.
     * Searches across URL, method, headers, body, status, and device info.
     */
    private fun matchesSearchText(call: NetworkCallEntry, searchText: String): Boolean {
        val search = searchText.lowercase()

        // Search in request fields
        if (call.request.url.lowercase().contains(search)) return true
        if (call.request.method.lowercase().contains(search)) return true
        if (call.request.body?.lowercase()?.contains(search) == true) return true
        if (call.request.contentType?.lowercase()?.contains(search) == true) return true
        if (call.request.headers.any { (key, value) ->
            key.lowercase().contains(search) || value.lowercase().contains(search)
        }) return true

        // Search in response fields
        call.response?.let { response ->
            if (response.statusCode.toString().contains(search)) return true
            if (response.statusMessage?.lowercase()?.contains(search) == true) return true
            if (response.body?.lowercase()?.contains(search) == true) return true
            if (response.contentType?.lowercase()?.contains(search) == true) return true
            if (response.error?.lowercase()?.contains(search) == true) return true
            if (response.headers.any { (key, value) ->
                key.lowercase().contains(search) || value.lowercase().contains(search)
            }) return true
        }

        // Search in device/app info
        if (call.deviceId.lowercase().contains(search)) return true
        if (call.packageName.lowercase().contains(search)) return true

        return false
    }

    private fun applyFilter(calls: List<NetworkCallEntry>, filter: NetworkFilter): List<NetworkCallEntry> {
        return calls.filter { call ->
            // Text search filter - searches across all fields
            if (filter.searchText.isNotEmpty()) {
                if (!matchesSearchText(call, filter.searchText)) {
                    return@filter false
                }
            }

            // Method filter
            if (filter.methodFilter != null) {
                if (!call.request.method.equals(filter.methodFilter, ignoreCase = true)) {
                    return@filter false
                }
            }

            // Status filter
            val statusCode = call.response?.statusCode
            val hasError = call.response?.error != null
            when (filter.statusFilter) {
                StatusFilter.ALL -> { /* include all */ }
                StatusFilter.SUCCESS -> {
                    if (statusCode == null || statusCode < 200 || statusCode >= 300) return@filter false
                }
                StatusFilter.REDIRECT -> {
                    if (statusCode == null || statusCode < 300 || statusCode >= 400) return@filter false
                }
                StatusFilter.CLIENT_ERROR -> {
                    if (statusCode == null || statusCode < 400 || statusCode >= 500) return@filter false
                }
                StatusFilter.SERVER_ERROR -> {
                    if (statusCode == null || statusCode < 500 || statusCode >= 600) return@filter false
                }
                StatusFilter.ERROR -> {
                    if (!hasError) return@filter false
                }
            }

            // Device filter
            if (filter.deviceFilter != null) {
                if (call.deviceId != filter.deviceFilter) {
                    return@filter false
                }
            }

            true
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}

/**
 * Table model for the network call list.
 * Returns raw values for proper sorting; cell renderers handle formatting.
 */
class NetworkCallTableModel : AbstractTableModel() {

    private val columns = arrayOf("Time", "Status", "Method", "URL", "Duration", "Size")

    var calls: List<NetworkCallEntry> = emptyList()
        private set

    fun updateCalls(newCalls: List<NetworkCallEntry>) {
        calls = newCalls
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = calls.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val call = calls.getOrNull(rowIndex) ?: return null
        return when (columnIndex) {
            0 -> call.startTime                                    // Raw Long for sorting
            1 -> call.response?.statusCode                         // Raw Int? for sorting
            2 -> call.request.method
            3 -> call.request.url
            4 -> call.duration                                     // Raw Long? for sorting
            5 -> call.response?.size ?: call.request.size          // Raw Long? for sorting
            else -> null
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
            0 -> Long::class.java
            1 -> Integer::class.java
            4 -> Long::class.java
            5 -> Long::class.java
            else -> String::class.java
        }
    }
}

/**
 * Renderer for the Time column - formats epoch millis to HH:mm:ss.SSS
 */
class TimeRenderer : DefaultTableCellRenderer() {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val formatted = (value as? Long)?.let {
            timeFormatter.format(Instant.ofEpochMilli(it))
        } ?: ""
        return super.getTableCellRendererComponent(table, formatted, isSelected, hasFocus, row, column)
    }
}

/**
 * Renderer for the Duration column - formats milliseconds
 */
class DurationRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val formatted = (value as? Long)?.let { "${it}ms" } ?: "..."
        return super.getTableCellRendererComponent(table, formatted, isSelected, hasFocus, row, column)
    }
}

/**
 * Renderer for the Size column - formats bytes to human readable
 */
class SizeRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val formatted = formatSize(value as? Long)
        return super.getTableCellRendererComponent(table, formatted, isSelected, hasFocus, row, column)
    }

    private fun formatSize(size: Long?): String {
        if (size == null) return "-"
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            else -> "${size / (1024 * 1024)}MB"
        }
    }
}

/**
 * Renderer for HTTP status codes with color coding.
 */
class StatusCodeRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val statusCode = value as? Int
        val displayText = statusCode?.toString() ?: "..."
        val component = super.getTableCellRendererComponent(table, displayText, isSelected, hasFocus, row, column)

        if (!isSelected) {
            foreground = when {
                statusCode == null -> JBColor.GRAY
                statusCode in 200..299 -> JBColor.namedColor("Flocon.status.success", JBColor(0x4CAF50, 0x4CAF50))
                statusCode in 300..399 -> JBColor.namedColor("Flocon.status.redirect", JBColor(0x2196F3, 0x2196F3))
                statusCode in 400..499 -> JBColor.namedColor("Flocon.status.clientError", JBColor(0xFF9800, 0xFF9800))
                statusCode in 500..599 -> JBColor.namedColor("Flocon.status.serverError", JBColor(0xF44336, 0xF44336))
                else -> JBColor.foreground()
            }
        }

        return component
    }
}

/**
 * Renderer for HTTP methods with styling.
 */
class MethodRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        if (!isSelected) {
            val method = value?.toString() ?: ""
            foreground = when (method.uppercase()) {
                "GET" -> JBColor.namedColor("Flocon.method.get", JBColor(0x4CAF50, 0x4CAF50))
                "POST" -> JBColor.namedColor("Flocon.method.post", JBColor(0x2196F3, 0x2196F3))
                "PUT" -> JBColor.namedColor("Flocon.method.put", JBColor(0xFF9800, 0xFF9800))
                "DELETE" -> JBColor.namedColor("Flocon.method.delete", JBColor(0xF44336, 0xF44336))
                "PATCH" -> JBColor.namedColor("Flocon.method.patch", JBColor(0x9C27B0, 0x9C27B0))
                else -> JBColor.foreground()
            }
        }

        return component
    }
}
