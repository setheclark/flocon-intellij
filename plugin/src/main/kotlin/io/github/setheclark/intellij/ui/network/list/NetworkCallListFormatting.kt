package io.github.setheclark.intellij.ui.network.list

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

internal fun NetworkCallListColumn.formatForDisplay(call: NetworkCallListItem): String = when (this) {
    NetworkCallListColumn.TIME -> timeFormatter.format(Instant.ofEpochMilli(call.startTime))
    NetworkCallListColumn.NAME -> call.name
    NetworkCallListColumn.STATUS -> call.status?.toString() ?: "..."
    NetworkCallListColumn.METHOD -> call.method
    NetworkCallListColumn.URL -> call.url
    NetworkCallListColumn.DURATION -> call.duration?.let { formatDuration(it) } ?: "..."
    NetworkCallListColumn.SIZE -> formatSize(call.size)
}

private fun formatDuration(ms: Double): String = if (ms >= 1000) "%.3f s".format(ms / 1000) else "%.3f ms".format(ms)

private fun formatSize(size: Long?): String = when {
    size == null -> "-"
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    else -> "${size / (1024 * 1024)} MB"
}

internal fun statusColor(code: Int, isDark: Boolean): Color = when (code) {
    in 200..299 -> if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    in 300..399 -> if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
    in 400..499 -> if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
    in 500..599 -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
    else -> Color.Unspecified
}

internal fun methodColor(method: String, isDark: Boolean): Color = when (method.uppercase()) {
    "GET" -> if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    "POST" -> if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
    "PUT" -> if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
    "DELETE" -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
    "PATCH" -> if (isDark) Color(0xFFCe93D8) else Color(0xFF6A1B9A)
    else -> Color.Unspecified
}
