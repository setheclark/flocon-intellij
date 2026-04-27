package io.github.setheclark.intellij.ui.network.details.common

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.PluginBundle.message
import io.github.setheclark.intellij.stringResource
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Text
import java.awt.datatransfer.StringSelection

@Composable
fun HeadersTable(
    headers: Map<String, String>?,
    modifier: Modifier = Modifier,
) {
    val sortedHeaders = remember(headers) {
        headers?.entries?.sortedBy { it.key.lowercase() }.orEmpty()
    }
    val keyWidth = rememberMaxKeyWidth(sortedHeaders.map { it.key })

    if (sortedHeaders.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource("label.noHeaders"))
        }
        return
    }

    Column(modifier = modifier) {
        sortedHeaders.forEach { (name, value) ->
            HeaderRow(name = name, value = value, keyWidth = keyWidth)
        }
    }
}

@Composable
private fun HeaderRow(name: String, value: String, keyWidth: Dp) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Workaround for https://issuetracker.google.com/issues/300781578?pli=1
    DisableSelection {
        ContextMenuArea(
            items = {
                listOf(
                    ContextMenuItem(message("action.copyName.text")) {
                        scope.launch { clipboard.copyString(name) }
                    },
                    ContextMenuItem(message("action.copyValue.text")) {
                        scope.launch { clipboard.copyString(value) }
                    },
                    ContextMenuItem(message("action.copyNameValue.text", name, value)) {
                        scope.launch { clipboard.copyString("$name: $value") }
                    },
                )
            },
        ) {
            KeyValueRow(key = name, value = value, keyWidth = keyWidth)
        }
    }
}

suspend fun Clipboard.copyString(text: String) {
    setClipEntry(ClipEntry(StringSelection(text)))
}
