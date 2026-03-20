package io.github.setheclark.intellij.ui.network.filter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.setheclark.intellij.domain.models.RequestTypeFilter
import io.github.setheclark.intellij.domain.models.StatusGroup
import io.github.setheclark.intellij.stringResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.lazy.rememberSelectableLazyListState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.hints.Stateful
import org.jetbrains.jewel.ui.theme.textFieldStyle

@OptIn(ExperimentalJewelApi::class)
@Composable
fun NetworkFilterPanel(
    viewModel: NetworkFilterViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState(
        initial = NetworkFilterPanelState(
            devices = DevicesRenderModel(emptyList(), -1),
            filterText = "",
        ),
    )

    val searchState = rememberTextFieldState()

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .distinctUntilChanged()
            .collectLatest { text ->
                viewModel.dispatch(NetworkFilterIntent.UpdateFilter(text))
            }
    }

    // Workaround for JEWEL-1244: sync external selection changes into list state
    val listState = rememberSelectableLazyListState()
    val devices = state.devices
    LaunchedEffect(devices.selectedIndex) {
        val key = devices.devices.getOrNull(devices.selectedIndex)?.toString()
        listState.selectedKeys = if (key != null) setOf(key) else emptySet()
    }

    val activeFilterCount = state.activeMethodFilters.size + state.activeStatusFilters.size + state.activeTypeFilters.size

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListComboBox(
                modifier = Modifier.widthIn(max = 200.dp),
                items = devices.devices.map { it.toString() },
                selectedIndex = devices.selectedIndex,
                onSelectedItemChange = { index ->
                    devices.devices.getOrNull(index)?.let { device ->
                        viewModel.dispatch(NetworkFilterIntent.UpdateDeviceSelection(device))
                    }
                },
                listState = listState,
                itemKeys = { _, item -> item },
            )

            TextField(
                modifier = Modifier.weight(1f),
                state = searchState,
                placeholder = {
                    Text(text = stringResource("label.filter.placeholder"))
                },
                leadingIcon = {
                    Icon(
                        key = AllIconsKeys.Actions.Find,
                        contentDescription = null,
                    )
                },
                trailingIcon = if (searchState.text.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { searchState.setTextAndPlaceCursorAtEnd("") },
                            style = JewelTheme.textFieldStyle.iconButtonStyle,
                        ) { buttonState ->
                            Icon(
                                key = AllIconsKeys.General.Close,
                                contentDescription = null,
                                hint = Stateful(buttonState),
                            )
                        }
                    }
                } else {
                    null
                },
            )

            FiltersToggleButton(
                expanded = state.filterExpanded,
                activeCount = activeFilterCount,
                onClick = { viewModel.dispatch(NetworkFilterIntent.ToggleFilterPanel) },
            )
        }

        AnimatedVisibility(visible = state.filterExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Method chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource("filter.label.method"),
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.info,
                    )
                    listOf("GET", "POST", "PUT", "DELETE", "PATCH").forEach { method ->
                        FilterChip(
                            label = method,
                            active = method in state.activeMethodFilters,
                            activeColor = methodChipColor(method),
                            onClick = { viewModel.dispatch(NetworkFilterIntent.ToggleMethodFilter(method)) },
                        )
                    }
                }

                // Status + Type chips on one row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource("filter.label.status"),
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.info,
                    )
                    listOf(
                        StatusGroup.S2xx to "2xx",
                        StatusGroup.S3xx to "3xx",
                        StatusGroup.S4xx to "4xx",
                        StatusGroup.S5xx to "5xx",
                    ).forEach { (group, label) ->
                        val isDark = JewelTheme.isDark
                        FilterChip(
                            label = label,
                            active = group in state.activeStatusFilters,
                            activeColor = statusGroupChipColor(group, isDark),
                            onClick = { viewModel.dispatch(NetworkFilterIntent.ToggleStatusFilter(group)) },
                        )
                    }

                    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(text = "·", color = JewelTheme.globalColors.text.info)
                    }

                    Text(
                        text = stringResource("filter.label.type"),
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.info,
                    )
                    listOf(
                        RequestTypeFilter.Http to "REST",
                        RequestTypeFilter.GraphQl to "GQL",
                        RequestTypeFilter.Grpc to "gRPC",
                    ).forEach { (type, label) ->
                        FilterChip(
                            label = label,
                            active = type in state.activeTypeFilters,
                            activeColor = JewelTheme.globalColors.text.info,
                            onClick = { viewModel.dispatch(NetworkFilterIntent.ToggleTypeFilter(type)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltersToggleButton(
    expanded: Boolean,
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = JewelTheme.isDark
    val bgColor = if (activeCount > 0) {
        if (isDark) Color(0xFF3D5A80).copy(alpha = 0.4f) else Color(0xFF1565C0).copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                key = if (expanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.Filter,
                contentDescription = null,
            )
            Text(
                text = if (activeCount > 0) {
                    stringResource("filter.button.filtersWithCount", activeCount)
                } else {
                    stringResource("filter.button.filters")
                },
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = JewelTheme.isDark
    val bgColor = if (active) activeColor.copy(alpha = if (isDark) 0.25f else 0.15f) else Color.Transparent
    val borderColor = if (active) activeColor else JewelTheme.globalColors.borders.normal
    val textColor = if (active) activeColor else JewelTheme.globalColors.text.normal

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

private fun methodChipColor(method: String): Color = when (method.uppercase()) {
    "GET" -> Color(0xFF2E7D32)
    "POST" -> Color(0xFF1565C0)
    "PUT" -> Color(0xFFE65100)
    "DELETE" -> Color(0xFFC62828)
    "PATCH" -> Color(0xFF6A1B9A)
    else -> Color.Gray
}

private fun statusGroupChipColor(group: StatusGroup, isDark: Boolean): Color = when (group) {
    StatusGroup.S2xx -> if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    StatusGroup.S3xx -> if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
    StatusGroup.S4xx -> if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
    StatusGroup.S5xx -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
}
