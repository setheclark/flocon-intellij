package io.github.setheclark.intellij.ui.network.list

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.component.styling.LocalSelectableLazyColumnStyle

@Composable
fun NetworkCallList(
    listState: NetworkCallListState,
    selectedCallId: String?,
    sortAscending: Boolean,
    onIntent: (NetworkCallListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortedCalls = remember(listState.calls, sortAscending) {
        if (sortAscending) {
            listState.calls.sortedBy { it.startTime }
        } else {
            listState.calls.sortedByDescending { it.startTime }
        }
    }

    val lazyListState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    val currentOnIntent by rememberUpdatedState(onIntent)

    // Scroll to newest entry when auto-scroll is active
    LaunchedEffect(sortedCalls.size, listState.autoScrollEnabled, sortAscending) {
        if (listState.autoScrollEnabled && sortedCalls.isNotEmpty()) {
            isAutoScrolling = true
            try {
                val target = if (sortAscending) sortedCalls.size - 1 else 0
                lazyListState.scrollToItem(target)
            } finally {
                isAutoScrolling = false
            }
        }
    }

    // Disable auto-scroll when user scrolls
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .filter { it }
            .collect {
                if (!isAutoScrolling) {
                    currentOnIntent(NetworkCallListIntent.DisableAutoScroll)
                }
            }
    }

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .onFocusChanged { isFocused = it.hasFocus }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onIntent(NetworkCallListIntent.ClearCallSelection)
                    true
                } else {
                    false
                }
            },
    ) {
        @Suppress("DEPRECATION")
        VerticallyScrollableContainer(
            scrollState = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(sortedCalls, key = { it.callId }) { call ->
                    TwoLineCallRow(
                        call = call,
                        isSelected = call.callId == selectedCallId,
                        isActive = isFocused,
                        onSelect = {
                            onIntent(NetworkCallListIntent.SelectCall(call.callId))
                            focusRequester.requestFocus()
                        },
                        onOpen = {
                            onIntent(NetworkCallListIntent.OpenCallInTab(call.callId, call.name))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TwoLineCallRow(
    call: NetworkCallListItem,
    isSelected: Boolean,
    isActive: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val style = LocalSelectableLazyColumnStyle.current
    val itemState = ListItemState(isSelected = isSelected, isActive = isActive)
    val bgColor by style.simpleListItemStyle.colors.backgroundFor(itemState)
    val contentColor by style.simpleListItemStyle.colors.contentFor(itemState)
    val isDark = JewelTheme.isDark

    val isError = call.status != null && call.status in 400..599
    val errorColor = call.status?.let { statusColor(it, isDark) } ?: Color.Unspecified

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(
                if (isError) {
                    Modifier.drawBehind {
                        drawRect(
                            color = errorColor,
                            topLeft = Offset.Zero,
                            size = Size(2.dp.toPx(), size.height),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .pointerInput(call.callId) {
                detectTapGestures(
                    onPress = { onSelect() },
                    onDoubleTap = { onOpen() },
                )
            }
            .padding(start = if (isError) 6.dp else 8.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
    ) {
        Column {
            // Line 1: method pill, type badge, status badge, name, time, duration, size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val methodColor = methodColor(call.method, isDark)
                Pill(text = call.method, color = if (isSelected) contentColor else methodColor)

                val typeLabelText = typeLabel(call.requestType)
                if (typeLabelText != null) {
                    val typeColor = typeBadgeColor(call.requestType, isDark) ?: contentColor
                    Pill(
                        text = typeLabelText,
                        color = if (isSelected) contentColor else typeColor,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                val statusText = call.status?.toString() ?: "···"
                val statusColor = call.status?.let { statusColor(it, isDark) } ?: contentColor
                Pill(
                    text = statusText,
                    color = if (isSelected) contentColor else statusColor,
                    modifier = Modifier.padding(start = 4.dp),
                )

                Text(
                    text = call.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor,
                )

                val secondaryColor = if (isSelected) contentColor else JewelTheme.globalColors.text.info
                Text(
                    text = formatTime(call.startTime),
                    color = secondaryColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = call.duration?.let { formatDuration(it) } ?: "···",
                    color = secondaryColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = formatSize(call.size),
                    color = secondaryColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            // Line 2: full URL
            Text(
                text = call.url,
                color = if (isSelected) contentColor else JewelTheme.globalColors.text.info,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp, top = 1.dp),
            )
        }
    }
}

@Composable
private fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}
