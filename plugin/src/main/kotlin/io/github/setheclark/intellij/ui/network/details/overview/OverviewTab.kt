package io.github.setheclark.intellij.ui.network.details.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.network.details.common.KeyValueRow
import io.github.setheclark.intellij.util.parseQueryParameters
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OverviewTab(
    call: NetworkCallEntity,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    }

    val statusText = when (val response = call.response) {
        is NetworkResponse.Success -> buildString {
            response.statusCode?.let { append("$it") }
            response.contentType?.let {
                if (isNotEmpty()) append(" - ")
                append(it)
            }
            if (isEmpty()) append(stringResource("label.statusSuccess"))
        }

        is NetworkResponse.Failure -> "Error: ${response.issue}"

        null -> stringResource("label.statusPending")
    }

    val durationText = call.response?.durationMs?.let { "${it}ms" } ?: "N/A"
    val startTimeText = remember(call.startTime) {
        timeFormatter.format(Instant.ofEpochMilli(call.startTime))
    }

    val queryParams = remember(call.request.url) { parseQueryParameters(call.request.url) }
    val sortedQueryParams = remember(queryParams) { queryParams.entries.sortedBy { it.key.lowercase() } }

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            SectionHeader(stringResource("section.general"))
            Spacer(Modifier.height(8.dp))
            OverviewRow(stringResource("label.overview.method"), call.request.method)
            OverviewRow(stringResource("label.overview.name"), call.name)
            OverviewRow(stringResource("label.overview.status"), statusText)
            OverviewRow(stringResource("label.overview.url"), call.request.url)

            if (sortedQueryParams.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource("section.queryParameters"))
                Spacer(Modifier.height(8.dp))
                sortedQueryParams.forEach { (name, value) ->
                    KeyValueRow(name, value)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader(stringResource("section.timing"))
            Spacer(Modifier.height(8.dp))
            OverviewRow(stringResource("label.overview.duration"), durationText)
            OverviewRow(stringResource("label.overview.startTime"), startTimeText)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )
}

@Composable
private fun OverviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            color = JewelTheme.globalColors.text.info,
            modifier = Modifier.width(100.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
