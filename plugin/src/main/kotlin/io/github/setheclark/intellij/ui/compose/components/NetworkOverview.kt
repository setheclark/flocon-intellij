package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Compose version of OverviewPanel.
 * Displays overview information about a network call including general info and timing.
 *
 * @param call The network call entity to display
 * @param modifier Optional modifier for the container
 */
@Composable
fun NetworkOverview(
    call: NetworkCallEntity?,
    modifier: Modifier = Modifier
) {
    if (call == null) {
        EmptyState(
            message = "Select a network call to view details",
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Section
            InfoSection(title = "General") {
                LabelValueRow(
                    label = "Method",
                    value = call.request.method
                )
                LabelValueRow(
                    label = "Name",
                    value = call.name
                )
                LabelValueRow(
                    label = "Status",
                    value = formatStatus(call.response)
                )
                LabelValueRow(
                    label = "URL",
                    value = call.request.url
                )
            }

            // Timing Section
            InfoSection(title = "Timing") {
                LabelValueRow(
                    label = "Duration",
                    value = formatDuration(call.response)
                )
                LabelValueRow(
                    label = "Start Time",
                    value = formatStartTime(call.startTime)
                )
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}

/**
 * Formats the response status for display
 */
private fun formatStatus(response: NetworkResponse?): String {
    return when (response) {
        is NetworkResponse.Success -> buildString {
            response.statusCode?.let { append("$it") }
            response.contentType?.let {
                if (isNotEmpty()) append(" - ")
                append(it)
            }
            if (isEmpty()) append("Success")
        }
        is NetworkResponse.Failure -> "Error: ${response.issue}"
        null -> "Pending"
    }
}

/**
 * Formats the duration for display
 */
private fun formatDuration(response: NetworkResponse?): String {
    return response?.durationMs?.let { "${it}ms" } ?: "N/A"
}

/**
 * Formats the start time for display
 */
private fun formatStartTime(startTimeMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(startTimeMillis))
}
