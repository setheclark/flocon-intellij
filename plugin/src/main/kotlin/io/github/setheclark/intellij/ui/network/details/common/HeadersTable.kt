package io.github.setheclark.intellij.ui.network.details.common

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.stringResource
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Text

@Composable
fun HeadersTable(
    headers: Map<String, String>?,
    modifier: Modifier = Modifier,
) {
    if (headers.isNullOrEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource("label.noHeaders"))
        }
        return
    }

    val sortedHeaders = remember(headers) {
        headers.entries.sortedBy { it.key.lowercase() }
    }

    LazyColumn(modifier = modifier) {
        items(sortedHeaders, key = { it.key }) { (name, value) ->
            HeaderRow(name = name, value = value)
        }
    }
}

@Composable
private fun HeaderRow(name: String, value: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val copyName = stringResource("action.copyName.text")
    val copyValue = stringResource("action.copyValue.text")
    val copy = stringResource("action.copyNameValue.text", name, value)

    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem(copyName) { scope.launch { clipboard.setText(name) } },
                ContextMenuItem(copyValue) { scope.launch { clipboard.setText(value) } },
                ContextMenuItem(copy) { scope.launch { clipboard.setText("$name: $value") } },
            )
        },
    ) {
        KeyValueRow(key = name, value = value)
    }
}

private suspend fun Clipboard.setText(text: String) {
    setClipEntry(ClipEntry(AnnotatedString(text)))
}
