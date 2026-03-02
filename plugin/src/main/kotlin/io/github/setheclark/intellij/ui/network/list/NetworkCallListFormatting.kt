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

internal fun statusColor(code: Int): Color = when (code) {
    in 200..299 -> Color(0xFF4CAF50)
    in 300..399 -> Color(0xFF2196F3)
    in 400..499 -> Color(0xFFFF9800)
    in 500..599 -> Color(0xFFF44336)
    else -> Color.Unspecified
}

internal fun methodColor(method: String): Color = when (method.uppercase()) {
    "GET" -> Color(0xFF4CAF50)
    "POST" -> Color(0xFF2196F3)
    "PUT" -> Color(0xFFFF9800)
    "DELETE" -> Color(0xFFF44336)
    "PATCH" -> Color(0xFF9C27B0)
    else -> Color.Unspecified
}
