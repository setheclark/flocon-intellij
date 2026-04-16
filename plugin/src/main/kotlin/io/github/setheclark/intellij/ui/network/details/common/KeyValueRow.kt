package io.github.setheclark.intellij.ui.network.details.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalTextStyle
import org.jetbrains.jewel.ui.component.Text

@Composable
fun KeyValueRow(
    key: String,
    value: String,
    keyWidth: Dp? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = key,
            fontWeight = FontWeight.Bold,
            color = JewelTheme.globalColors.text.info,
            modifier = Modifier
                .then(if (keyWidth != null) Modifier.width(keyWidth) else Modifier)
                .padding(end = 4.dp, top = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
        )
    }
}

/**
 * Measures all [keys] with the bold text style used by [KeyValueRow] and returns the width of the
 * longest one (plus a small gap), so all rows in a section can share a consistent key column width.
 */
@Composable
fun rememberMaxKeyWidth(keys: List<String>): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = LocalTextStyle.current.merge(TextStyle(fontWeight = FontWeight.Bold))
    return remember(keys, density, style) {
        val maxPx = keys.maxOfOrNull { textMeasurer.measure(it, style).size.width } ?: 0
        with(density) { maxPx.toDp() } + 12.dp  // gap between key and value
    }
}
