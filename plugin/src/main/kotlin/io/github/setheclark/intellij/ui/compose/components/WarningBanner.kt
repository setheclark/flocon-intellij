package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Strips HTML tags from text and returns plain text.
 * Simple implementation for basic HTML tags used in warning messages.
 */
private fun String.stripHtmlTags(): String {
    return this
        .replace("<html>", "")
        .replace("</html>", "")
        .replace("<b>", "")
        .replace("</b>", "")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}

/**
 * Compose version of the WarningBanner component.
 * Displays a warning message with an icon in a colored banner.
 *
 * @param text The warning text to display (can include basic HTML formatting)
 * @param isVisible Whether the banner should be visible
 * @param modifier Optional modifier for the banner
 */
@Composable
fun WarningBanner(
    text: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // Always render to maintain layout, but conditionally show content
    if (isVisible && text.isNotEmpty()) {
        val warningBackgroundColor = if (org.jetbrains.jewel.foundation.theme.JewelTheme.isDark) {
            Color(0xFF5C4813)
        } else {
            Color(0xFFFFF3CD)
        }

        val warningTextColor = if (org.jetbrains.jewel.foundation.theme.JewelTheme.isDark) {
            Color(0xFFFFE69C)
        } else {
            Color(0xFF856404)
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(warningBackgroundColor)
                .border(
                    width = 1.dp,
                    color = IntelliJTheme.colors.border,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: Add icon support - need to figure out correct Jewel Icon API
            Text(
                text = "⚠",
                style = IntelliJTheme.typography.body.copy(
                    color = warningTextColor,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = text.stripHtmlTags(),
                style = IntelliJTheme.typography.body.copy(
                    color = warningTextColor,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
            )
        }
    }
}
