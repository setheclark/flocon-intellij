package io.github.setheclark.intellij.ui.network.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.network.list.formatDuration
import io.github.setheclark.intellij.ui.network.list.formatSize
import io.github.setheclark.intellij.ui.network.list.methodColor
import io.github.setheclark.intellij.ui.network.list.statusColor
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallDetailStatusBar(
    call: NetworkCallEntity,
    onCopyCurl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = JewelTheme.isDark

    var elapsedMs by remember(call.callId) {
        mutableLongStateOf(System.currentTimeMillis() - call.startTime)
    }
    LaunchedEffect(call.callId, call.response) {
        if (call.response == null) {
            while (true) {
                delay(100)
                elapsedMs = System.currentTimeMillis() - call.startTime
            }
        }
    }

    val isPending = call.response == null
    val isGrpc = call.request.type is NetworkRequest.Type.Grpc
    val isGraphQl = call.request.type is NetworkRequest.Type.GraphQl

    val statusText = when (val r = call.response) {
        is NetworkResponse.Success -> r.statusCode?.toString() ?: stringResource("label.statusSuccess")
        is NetworkResponse.Failure -> "Error"
        null -> stringResource("label.pending")
    }
    val statusColor = when (val r = call.response) {
        is NetworkResponse.Success -> r.statusCode?.let { statusColor(it, isDark) } ?: Color.Unspecified
        is NetworkResponse.Failure -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
        null -> JewelTheme.globalColors.text.info
    }

    val durationText = when (val r = call.response) {
        is NetworkResponse.Success -> formatDuration(r.durationMs)
        is NetworkResponse.Failure -> formatDuration(r.durationMs)
        null -> stringResource("label.detail.elapsed", formatDuration(elapsedMs.toDouble()))
    }

    val responseSize = (call.response as? NetworkResponse.Success)?.size
    val requestSize = call.request.size

    val methodColor = methodColor(call.request.method, isDark)

    // Strip the "METHOD " prefix from the name if present — the method pill already shows it.
    val displayName = remember(call.name, call.request.method) {
        val prefix = "${call.request.method} "
        if (call.name.startsWith(prefix)) call.name.removePrefix(prefix) else call.name
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Method pill
        Text(
            text = call.request.method,
            color = methodColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )

        // Protocol badge
        when {
            isGrpc -> Badge(stringResource("badge.grpc"), isDark)
            isGraphQl -> Badge(stringResource("badge.graphql"), isDark)
        }

        // Call name (takes remaining space)
        Text(
            text = displayName,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
        )

        // Status
        Text(
            text = statusText,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )

        Separator()

        // Duration
        Text(
            text = durationText,
            color = if (isPending) JewelTheme.globalColors.text.info else Color.Unspecified,
            fontSize = 11.sp,
        )

        // Sizes (only if available)
        if (requestSize != null || responseSize != null) {
            Separator()
            Text(
                text = buildSizeText(requestSize, responseSize),
                color = JewelTheme.globalColors.text.info,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.width(2.dp))

        // Copy as cURL
        IconActionButton(
            key = AllIconsKeys.Actions.Copy,
            contentDescription = stringResource("action.copyAsCurl.text"),
            onClick = onCopyCurl,
            focusable = false,
            tooltip = { Text(stringResource("action.copyAsCurl.text")) },
        )
    }
}

@Composable
private fun Badge(text: String, isDark: Boolean) {
    Text(
        text = text,
        color = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0),
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
    )
}

@Composable
private fun Separator() {
    Text(
        text = "·",
        color = JewelTheme.globalColors.text.info,
        fontSize = 11.sp,
    )
}

private fun buildSizeText(requestSize: Long?, responseSize: Long?): String = buildString {
    requestSize?.let { append("↑ ${formatSize(it)}") }
    if (requestSize != null && responseSize != null) append("  ")
    responseSize?.let { append("↓ ${formatSize(it)}") }
}
