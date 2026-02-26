package io.github.setheclark.intellij.ui.network.list

import com.intellij.ui.JBColor
import java.awt.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

enum class NetworkCallListColumn(
    val displayName: String,
    val preferredWidth: Int,
    val valueClass: Class<*>,
    val getValue: (NetworkCallListItem) -> Any?,
) {
    TIME(
        displayName = "Time",
        preferredWidth = 75,
        valueClass = Long::class.java,
        getValue = { it.startTime },
    ),
    NAME(
        displayName = "Name",
        preferredWidth = 150,
        valueClass = String::class.java,
        getValue = { it.name },
    ),
    STATUS(
        displayName = "Status",
        preferredWidth = 50,
        valueClass = Int::class.javaObjectType,
        getValue = { it.status },
    ),
    METHOD(
        displayName = "Method",
        preferredWidth = 50,
        valueClass = String::class.java,
        getValue = { it.method },
    ),
    URL(
        displayName = "URL",
        preferredWidth = 400,
        valueClass = String::class.java,
        getValue = { it.url },
    ),
    DURATION(
        displayName = "Duration",
        preferredWidth = 50,
        valueClass = Double::class.javaObjectType,
        getValue = { it.duration },
    ),
    SIZE(
        displayName = "Size",
        preferredWidth = 50,
        valueClass = Long::class.java,
        getValue = { it.size ?: 0L },
    ),
    ;

    val renderer: TableCellRenderer by lazy {
        when (this) {
            TIME -> TimeRenderer()
            NAME -> DefaultTableCellRenderer()
            STATUS -> StatusCodeRenderer()
            METHOD -> MethodRenderer()
            URL -> DefaultTableCellRenderer()
            DURATION -> DurationRenderer()
            SIZE -> SizeRenderer()
        }
    }

    val comparator: Comparator<Any?>?
        get() = when (this) {
            TIME -> nullsLastLongComparator
            STATUS -> nullsLastIntComparator
            DURATION -> nullsLastDoubleComparator
            SIZE -> nullsLastLongComparator
            else -> null
        }

    companion object {
        private val nullsLastLongComparator = Comparator<Any?> { a, b ->
            val aVal = (a as? Long) ?: Long.MAX_VALUE
            val bVal = (b as? Long) ?: Long.MAX_VALUE
            aVal.compareTo(bVal)
        }
        private val nullsLastIntComparator = Comparator<Any?> { a, b ->
            val aVal = (a as? Int) ?: Int.MAX_VALUE
            val bVal = (b as? Int) ?: Int.MAX_VALUE
            aVal.compareTo(bVal)
        }
        private val nullsLastDoubleComparator = Comparator<Any?> { a, b ->
            val aVal = (a as? Double) ?: Double.MAX_VALUE
            val bVal = (b as? Double) ?: Double.MAX_VALUE
            aVal.compareTo(bVal)
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
            column: Int,
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
            column: Int,
        ): Component {
            val formatted = (value as? Double)?.let { formatDuration(it) } ?: "..."
            return super.getTableCellRendererComponent(table, formatted, isSelected, hasFocus, row, column)
        }

        private fun formatDuration(durationMs: Double): String {
            return if (durationMs >= 1000) {
                val seconds = durationMs / 1000
                "%.3f s".format(seconds)
            } else {
                "%.3f ms".format(durationMs)
            }
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
            column: Int,
        ): Component {
            val formatted = formatSize(value as? Long)
            return super.getTableCellRendererComponent(table, formatted, isSelected, hasFocus, row, column)
        }

        private fun formatSize(size: Long?): String {
            if (size == null) return "-"
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> "${size / (1024 * 1024)} MB"
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
            column: Int,
        ): Component {
            val statusCode = value as? Int
            val displayText = statusCode?.toString() ?: "..."
            val component = super.getTableCellRendererComponent(table, displayText, isSelected, hasFocus, row, column)

            if (!isSelected) {
                foreground = when (statusCode) {
                    null -> JBColor.GRAY
                    in 200..299 -> JBColor.namedColor("Network.status.success", JBColor(0x4CAF50, 0x4CAF50))
                    in 300..399 -> JBColor.namedColor("Network.status.redirect", JBColor(0x2196F3, 0x2196F3))
                    in 400..499 -> JBColor.namedColor("Network.status.clientError", JBColor(0xFF9800, 0xFF9800))
                    in 500..599 -> JBColor.namedColor("Network.status.serverError", JBColor(0xF44336, 0xF44336))
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
            column: Int,
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            if (!isSelected) {
                val method = value?.toString() ?: ""
                foreground = when (method.uppercase()) {
                    "GET" -> JBColor.namedColor("Network.method.get", JBColor(0x4CAF50, 0x4CAF50))
                    "POST" -> JBColor.namedColor("Network.method.post", JBColor(0x2196F3, 0x2196F3))
                    "PUT" -> JBColor.namedColor("Network.method.put", JBColor(0xFF9800, 0xFF9800))
                    "DELETE" -> JBColor.namedColor("Network.method.delete", JBColor(0xF44336, 0xF44336))
                    "PATCH" -> JBColor.namedColor("Network.method.patch", JBColor(0x9C27B0, 0x9C27B0))
                    else -> JBColor.foreground()
                }
            }

            return component
        }
    }
}
