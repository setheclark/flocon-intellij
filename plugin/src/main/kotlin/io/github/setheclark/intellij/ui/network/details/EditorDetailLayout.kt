package io.github.setheclark.intellij.ui.network.details

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.settings.NetworkStorageSettingsState
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.network.details.common.HeadersTable
import io.github.setheclark.intellij.ui.network.details.common.KeyValueRow
import io.github.setheclark.intellij.ui.network.details.common.rememberMaxKeyWidth
import io.github.setheclark.intellij.util.buildCurlCommand
import io.github.setheclark.intellij.util.parseQueryParameters
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Cursor
import java.awt.datatransfer.StringSelection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(FlowPreview::class)
@Composable
fun EditorDetailLayout(
    call: NetworkCallEntity,
    modifier: Modifier = Modifier,
) {
    val settings = remember { NetworkStorageSettingsState.getInstance() }
    val density = LocalDensity.current

    var leftPanelWidthDp by remember { mutableStateOf(settings.detailLeftPanelWidthDp) }
    var isLeftPanelCollapsed by remember { mutableStateOf(false) }

    // Persist left panel width with debounce
    LaunchedEffect(Unit) {
        snapshotFlow { leftPanelWidthDp }
            .debounce(300)
            .collect { settings.detailLeftPanelWidthDp = it }
    }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        CallDetailStatusBar(
            call = call,
            onCopyCurl = {
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(StringSelection(buildCurlCommand(call))))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel
            if (!isLeftPanelCollapsed) {
                Box(
                    modifier = Modifier
                        .width(leftPanelWidthDp.dp)
                        .fillMaxHeight(),
                ) {
                    EditorLeftPanel(
                        call = call,
                        onCollapse = { isLeftPanelCollapsed = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Vertical drag handle
            PanelDivider(
                collapsed = isLeftPanelCollapsed,
                onToggleCollapse = { isLeftPanelCollapsed = !isLeftPanelCollapsed },
                onDrag = { dragAmountPx ->
                    val deltaDp = with(density) { dragAmountPx.toDp().value }
                    leftPanelWidthDp = (leftPanelWidthDp + deltaDp).coerceIn(120f, 600f)
                },
            )

            // Body pane
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                BodyPane(call = call, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PanelDivider(
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
) {
    // Drag gesture lives only on the two divider segments; the icon is a separate tap target.
    val dragModifier = Modifier
        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.x)
            }
        }

    Column(
        modifier = Modifier.width(18.dp).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().then(dragModifier)) {
            Divider(
                orientation = Orientation.Vertical,
                modifier = Modifier.fillMaxHeight().align(Alignment.Center),
            )
        }

        Icon(
            key = if (collapsed) AllIconsKeys.General.ArrowRight else AllIconsKeys.General.ArrowLeft,
            contentDescription = if (collapsed) "Expand panel" else "Collapse panel",
            modifier = Modifier
                .size(18.dp)
                .padding(vertical = 4.dp)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.DEFAULT_CURSOR)))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onToggleCollapse() })
                },
            tint = JewelTheme.globalColors.text.info,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth().then(dragModifier)) {
            Divider(
                orientation = Orientation.Vertical,
                modifier = Modifier.fillMaxHeight().align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun EditorLeftPanel(
    call: NetworkCallEntity,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    }
    val startTimeText = remember(call.startTime) {
        timeFormatter.format(Instant.ofEpochMilli(call.startTime))
    }
    val queryParams = remember(call.request.url) { parseQueryParameters(call.request.url) }
    val sortedQueryParams = remember(queryParams) { queryParams.entries.sortedBy { it.key.lowercase() } }

    val responseHeaders = (call.response as? NetworkResponse.Success)?.headers
    val requestHeaders = call.request.headers.takeIf { it.isNotEmpty() }

    // Key column widths — computed unconditionally so composable call rules are satisfied
    val urlLabel = stringResource("label.detail.url")
    val startTimeLabel = stringResource("label.overview.startTime")
    val generalKeyWidth = rememberMaxKeyWidth(listOf(urlLabel, startTimeLabel))
    val queryKeyWidth = rememberMaxKeyWidth(sortedQueryParams.map { it.key })
    val graphqlType = call.request.type as? NetworkRequest.Type.GraphQl
    val graphqlKeys = remember(graphqlType) {
        buildList {
            if (graphqlType != null) {
                graphqlType.operationName?.let { add("Operation Name") }
                add("Operation Type")
                add("Persisted")
                graphqlType.query?.let { add("Has Query") }
            }
        }
    }
    val graphqlKeyWidth = rememberMaxKeyWidth(graphqlKeys)

    var expandedSection by remember(call.callId) { mutableStateOf<String?>("general") }
    fun toggle(key: String) {
        expandedSection = if (expandedSection == key) null else key
    }

    SelectionContainer(modifier = modifier) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // General section
            CollapsibleSection(
                title = stringResource("section.general"),
                expanded = expandedSection == "general",
                onToggle = { toggle("general") },
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    KeyValueRow(key = urlLabel, value = java.net.URLDecoder.decode(call.request.url, "UTF-8"), keyWidth = generalKeyWidth)
                    KeyValueRow(key = startTimeLabel, value = startTimeText, keyWidth = generalKeyWidth)
                }
            }

            // Query params (only if present)
            if (sortedQueryParams.isNotEmpty()) {
                CollapsibleSection(
                    title = stringResource("section.queryParameters"),
                    expanded = expandedSection == "queryParams",
                    onToggle = { toggle("queryParams") },
                    badge = "(${sortedQueryParams.size})",
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        sortedQueryParams.forEach { (key, value) ->
                            KeyValueRow(key = key, value = value, keyWidth = queryKeyWidth)
                        }
                    }
                }
            }

            // GraphQL section (only if GraphQL type)
            val type = call.request.type
            if (type is NetworkRequest.Type.GraphQl) {
                CollapsibleSection(
                    title = stringResource("section.graphql"),
                    expanded = expandedSection == "graphql",
                    onToggle = { toggle("graphql") },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        type.operationName?.let { KeyValueRow(key = "Operation Name", value = it, keyWidth = graphqlKeyWidth) }
                        KeyValueRow(key = "Operation Type", value = type.operationType, keyWidth = graphqlKeyWidth)
                        KeyValueRow(key = "Persisted", value = type.persisted.toString(), keyWidth = graphqlKeyWidth)
                        type.query?.let { KeyValueRow(key = "Has Query", value = "Yes", keyWidth = graphqlKeyWidth) }
                    }
                }
            }

            // Request headers
            CollapsibleSection(
                title = stringResource("section.requestHeaders"),
                expanded = expandedSection == "requestHeaders",
                onToggle = { toggle("requestHeaders") },
                badge = requestHeaders?.size?.let { "($it)" },
            ) {
                HeadersTable(headers = requestHeaders, modifier = Modifier.fillMaxWidth())
            }

            // Response headers
            CollapsibleSection(
                title = stringResource("section.responseHeaders"),
                expanded = expandedSection == "responseHeaders",
                onToggle = { toggle("responseHeaders") },
                badge = responseHeaders?.size?.let { "($it)" },
            ) {
                HeadersTable(headers = responseHeaders, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
