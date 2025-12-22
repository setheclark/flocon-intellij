package io.github.openflocon.intellij.ui.list

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import io.github.openflocon.intellij.services.FloconProjectService
import io.github.openflocon.intellij.services.NetworkCallEntry
import io.github.openflocon.intellij.services.NetworkFilter
import io.github.openflocon.intellij.services.StatusFilter
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
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

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

    init {
        setupTable()
        observeNetworkCalls()
    }

    private fun setupTable() {
        table.apply {
            setShowGrid(false)
            rowHeight = JBUI.scale(24)
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

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
            columnModel.getColumn(1).cellRenderer = StatusCodeRenderer()
            columnModel.getColumn(2).cellRenderer = MethodRenderer()

            // Selection listener
            selectionModel.addListSelectionListener { e ->
                if (!e.valueIsAdjusting) {
                    val selectedRow = selectedRow
                    if (selectedRow >= 0 && selectedRow < tableModel.calls.size) {
                        floconService.selectCall(tableModel.calls[selectedRow])
                    } else {
                        floconService.selectCall(null)
                    }
                }
            }
        }

        // Use JBScrollPane to properly handle the table header (keeps it pinned at top)
        val scrollPane = JBScrollPane(table)
        add(scrollPane, BorderLayout.CENTER)
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
                    tableModel.updateCalls(filteredCalls)
                }
            }
        }
    }

    private fun applyFilter(calls: List<NetworkCallEntry>, filter: NetworkFilter): List<NetworkCallEntry> {
        return calls.filter { call ->
            // URL search filter
            if (filter.searchText.isNotEmpty()) {
                if (!call.request.url.contains(filter.searchText, ignoreCase = true)) {
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
 */
class NetworkCallTableModel : AbstractTableModel() {

    private val columns = arrayOf("Time", "Status", "Method", "URL", "Duration", "Size")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

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
            0 -> formatTime(call.startTime)
            1 -> call.response?.statusCode?.toString() ?: "..."
            2 -> call.request.method
            3 -> call.request.url
            4 -> call.duration?.let { "${it}ms" } ?: "..."
            5 -> formatSize(call.response?.size ?: call.request.size)
            else -> null
        }
    }

    private fun formatTime(epochMillis: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(epochMillis))
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
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        if (!isSelected) {
            val statusText = value?.toString() ?: ""
            foreground = when {
                statusText == "..." -> JBColor.GRAY
                statusText.startsWith("2") -> JBColor.namedColor("Flocon.status.success", JBColor(0x4CAF50, 0x4CAF50))
                statusText.startsWith("3") -> JBColor.namedColor("Flocon.status.redirect", JBColor(0x2196F3, 0x2196F3))
                statusText.startsWith("4") -> JBColor.namedColor("Flocon.status.clientError", JBColor(0xFF9800, 0xFF9800))
                statusText.startsWith("5") -> JBColor.namedColor("Flocon.status.serverError", JBColor(0xF44336, 0xF44336))
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
