package io.github.setheclark.intellij.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.theme.defaultTabStyle

@Composable
fun TabbedContent(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (selectedIndex: Int) -> Unit,
) {
    Column(modifier = modifier) {
        TabStrip(
            tabs = tabs.mapIndexed { index, name ->
                TabData.Default(
                    selected = index == selectedIndex,
                    closable = false,
                    content = { SimpleTabContent(label = name, state = it) },
                    onClick = { onTabSelect(index) },
                )
            },
            style = JewelTheme.defaultTabStyle,
            interactionSource = remember { MutableInteractionSource() },
            modifier = Modifier.fillMaxWidth(),
        )
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        content(selectedIndex)
    }
}
