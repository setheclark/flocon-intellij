package io.github.setheclark.intellij.ui.network.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.component.TabbedContent
import io.github.setheclark.intellij.ui.network.details.common.EmptyContent
import io.github.setheclark.intellij.ui.network.details.common.HttpMessageContent
import io.github.setheclark.intellij.ui.network.details.common.ScratchFileContext
import io.github.setheclark.intellij.ui.network.details.overview.OverviewTab
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
fun DetailsContent(
    viewModel: DetailPanelViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedCall by viewModel.selectedCall.collectAsState(initial = null)
    val currentCall = selectedCall

    if (currentCall == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource("label.selectRequest"),
                color = JewelTheme.globalColors.text.info,
            )
        }
        return
    }

    val tabNames = listOf(
        stringResource("tab.overview"),
        stringResource("tab.response"),
        stringResource("tab.request"),
    )

    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val selectedResponseSubTab by viewModel.selectedResponseSubTab.collectAsState()
    val selectedRequestSubTab by viewModel.selectedRequestSubTab.collectAsState()

    TabbedContent(
        tabs = tabNames,
        selectedIndex = selectedTabIndex,
        onTabSelect = { viewModel.selectedTabIndex.value = it },
        modifier = modifier.fillMaxSize(),
    ) { index ->
        when (index) {
            0 -> OverviewTab(currentCall, Modifier.fillMaxSize())
            1 -> ResponseTab(currentCall, selectedResponseSubTab) { viewModel.selectedResponseSubTab.value = it }
            2 -> RequestTab(currentCall, selectedRequestSubTab) { viewModel.selectedRequestSubTab.value = it }
        }
    }
}

@Composable
private fun RequestTab(
    call: NetworkCallEntity,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
) {
    val request = call.request
    val contentType = request.headers.entries
        .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
        ?.value

    val scratchContext = request.body?.let {
        ScratchFileContext(
            queryName = call.name,
            bodyType = ScratchFileContext.BodyType.REQUEST,
            contentType = contentType,
            statusCode = null,
            timestamp = call.startTime,
            body = it,
        )
    }
    HttpMessageContent(
        headers = request.headers,
        body = request.body,
        contentType = contentType,
        scratchContext = scratchContext,
        selectedTabIndex = selectedTabIndex,
        onTabSelect = onTabSelect,
    )
}

@Composable
private fun ResponseTab(
    call: NetworkCallEntity,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
) {
    val response = call.response

    if (response == null) {
        EmptyContent(stringResource("label.noResponse"))
    } else {
        val header = when (response) {
            is NetworkResponse.Success -> response.headers
            is NetworkResponse.Failure -> emptyMap()
        }
        val body = when (response) {
            is NetworkResponse.Success -> response.body
            is NetworkResponse.Failure -> stringResource("label.networkError", response.issue)
        }
        val contentType = when (response) {
            is NetworkResponse.Success -> response.contentType
            is NetworkResponse.Failure -> null
        }
        val scratchContext = when (response) {
            is NetworkResponse.Success -> response.body?.let {
                ScratchFileContext(
                    queryName = call.name,
                    bodyType = ScratchFileContext.BodyType.RESPONSE,
                    contentType = contentType,
                    statusCode = null,
                    timestamp = call.startTime,
                    body = it,
                )
            }

            is NetworkResponse.Failure -> null
        }

        HttpMessageContent(
            headers = header,
            body = body,
            contentType = contentType,
            scratchContext = scratchContext,
            selectedTabIndex = selectedTabIndex,
            onTabSelect = onTabSelect,
        )
    }
}
