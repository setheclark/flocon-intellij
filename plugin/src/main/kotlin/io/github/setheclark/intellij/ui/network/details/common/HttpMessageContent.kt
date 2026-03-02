package io.github.setheclark.intellij.ui.network.details.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.component.TabbedContent

@Composable
fun HttpMessageContent(
    headers: Map<String, String>?,
    body: String?,
    contentType: String?,
    scratchContext: ScratchFileContext?,
    bodyContentPanel: BodyContentPanel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(body, contentType) {
        bodyContentPanel.showBody(body, contentType, scratchContext)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource("tab.headers"),
        stringResource("tab.body"),
    )

    TabbedContent(
        tabs = tabs,
        selectedIndex = selectedTabIndex,
        onTabSelect = {
            // K2 bug: https://youtrack.jetbrains.com/projects/KT/issues/KT-78881/K2-False-positive-Assigned-value-is-never-read-in-composable-function
            @Suppress("AssignedValueIsNeverRead")
            selectedTabIndex = it
        },
        modifier = modifier,
    ) { index ->
        when (index) {
            0 -> HeadersTable(headers, Modifier.fillMaxSize())
            1 -> SwingPanel(modifier = Modifier.fillMaxSize(), factory = { bodyContentPanel })
        }
    }
}
