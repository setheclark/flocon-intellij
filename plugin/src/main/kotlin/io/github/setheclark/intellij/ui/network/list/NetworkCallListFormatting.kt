package io.github.setheclark.intellij.ui.network.list

import androidx.compose.ui.graphics.Color
import io.github.setheclark.intellij.domain.models.RequestTypeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

internal fun formatTime(startTime: Long): String = timeFormatter.format(Instant.ofEpochMilli(startTime))

internal fun formatDuration(ms: Double): String = if (ms >= 1000) "%.3f s".format(ms / 1000) else "%.3f ms".format(ms)

internal fun formatSize(size: Long?): String = when {
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

/** Returns the label to show in the type badge, or null for plain HTTP (no badge). */
internal fun typeLabel(type: RequestTypeFilter): String? = when (type) {
    RequestTypeFilter.Http -> null
    RequestTypeFilter.GraphQl -> "GQL"
    RequestTypeFilter.Grpc -> "gRPC"
}

/** Returns the color for the type badge, or null for plain HTTP. */
internal fun typeBadgeColor(type: RequestTypeFilter, isDark: Boolean): Color? = when (type) {
    RequestTypeFilter.Http -> null
    RequestTypeFilter.GraphQl -> if (isDark) Color(0xFFBA68C8) else Color(0xFF7B1FA2)
    RequestTypeFilter.Grpc -> if (isDark) Color(0xFF4DB6AC) else Color(0xFF00695C)
}
