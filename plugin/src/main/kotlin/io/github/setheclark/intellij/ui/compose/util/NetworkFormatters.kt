package io.github.setheclark.intellij.ui.compose.util

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formatting utilities for network call display.
 */
object NetworkFormatters {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    /**
     * Format timestamp (epoch millis) to HH:mm:ss.SSS
     */
    fun formatTime(epochMillis: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(epochMillis))
    }

    /**
     * Format duration in milliseconds
     */
    fun formatDuration(durationMs: Double?): String {
        if (durationMs == null) return "..."
        return if (durationMs >= 1000) {
            val seconds = durationMs / 1000
            "%.3f s".format(seconds)
        } else {
            "%.3f ms".format(durationMs)
        }
    }

    /**
     * Format size in bytes to human readable
     */
    fun formatSize(size: Long?): String {
        if (size == null) return "-"
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    /**
     * Get color for HTTP status code
     */
    fun getStatusColor(statusCode: Int?): Color {
        return when (statusCode) {
            null -> Color.Gray
            in 200..299 -> Color(0xFF4CAF50)  // Green
            in 300..399 -> Color(0xFF2196F3)  // Blue
            in 400..499 -> Color(0xFFFF9800)  // Orange
            in 500..599 -> Color(0xFFF44336)  // Red
            else -> Color.Unspecified
        }
    }

    /**
     * Get color for HTTP method
     */
    fun getMethodColor(method: String): Color {
        return when (method.uppercase()) {
            "GET" -> Color(0xFF4CAF50)     // Green
            "POST" -> Color(0xFF2196F3)    // Blue
            "PUT" -> Color(0xFFFF9800)     // Orange
            "DELETE" -> Color(0xFFF44336)  // Red
            "PATCH" -> Color(0xFF9C27B0)   // Purple
            else -> Color.Unspecified
        }
    }
}
