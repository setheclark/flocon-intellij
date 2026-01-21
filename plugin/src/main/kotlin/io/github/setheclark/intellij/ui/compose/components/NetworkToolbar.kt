package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import io.github.setheclark.intellij.ui.network.NetworkInspectorIntent
import org.jetbrains.skia.Image as SkiaImage
import java.awt.image.BufferedImage
import javax.swing.Icon

/**
 * Network inspector toolbar with action buttons.
 *
 * Pure Compose implementation migrated in Phase 5.
 * Replaces IntelliJ's ActionToolbar with Compose icon buttons.
 *
 * @param autoScrollEnabled Whether auto-scroll is enabled
 * @param serverState Current server state
 * @param onAction Callback for dispatching actions
 * @param modifier Optional modifier
 */
@Composable
fun NetworkToolbar(
    autoScrollEnabled: Boolean,
    serverState: MessageServerState,
    onAction: (NetworkInspectorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear All button
        IconActionButton(
            icon = AllIcons.Actions.GC,
            contentDescription = "Clear All",
            onClick = { onAction(NetworkInspectorIntent.ClearAll) }
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Auto-scroll toggle button
        IconActionButton(
            icon = AllIcons.RunConfigurations.Scroll_down,
            contentDescription = if (autoScrollEnabled) "Disable auto-scroll" else "Enable auto-scroll",
            onClick = {
                if (autoScrollEnabled) {
                    onAction(NetworkInspectorIntent.DisableAutoScroll)
                } else {
                    onAction(NetworkInspectorIntent.EnableAutoScroll)
                }
            }
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Server start/stop/retry button
        val (serverIcon, serverDescription, serverEnabled) = when (serverState) {
            is MessageServerState.Running -> Triple(
                AllIcons.Actions.Suspend,
                "Stop Message Server",
                true
            )
            is MessageServerState.Stopped -> Triple(
                AllIcons.Actions.Execute,
                "Start Message Server",
                true
            )
            is MessageServerState.Error -> Triple(
                AllIcons.Actions.Restart,
                "Retry Message Server",
                true
            )
            is MessageServerState.Starting -> Triple(
                AllIcons.Actions.Execute,
                "Starting...",
                false
            )
            is MessageServerState.Stopping -> Triple(
                AllIcons.Actions.Suspend,
                "Stopping...",
                false
            )
        }

        IconActionButton(
            icon = serverIcon,
            contentDescription = serverDescription,
            enabled = serverEnabled,
            onClick = {
                when (serverState) {
                    is MessageServerState.Running -> onAction(NetworkInspectorIntent.StopServer)
                    is MessageServerState.Stopped, is MessageServerState.Error -> onAction(NetworkInspectorIntent.StartServer)
                    else -> { /* ignore */ }
                }
            }
        )
    }
}

/**
 * Helper composable to display an IntelliJ icon as a clickable button.
 */
@Composable
private fun IconActionButton(
    icon: Icon,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        SwingIconImage(
            icon = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            alpha = if (enabled) 1f else 0.5f
        )
    }
}

/**
 * Converts a Swing Icon to a Compose Image.
 */
@Composable
private fun SwingIconImage(
    icon: Icon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val bufferedImage = BufferedImage(
        icon.iconWidth,
        icon.iconHeight,
        BufferedImage.TYPE_INT_ARGB
    )
    val graphics = bufferedImage.createGraphics()
    icon.paintIcon(null, graphics, 0, 0)
    graphics.dispose()

    val bytes = bufferedImage.getRaster().dataBuffer.let { buffer ->
        ByteArray(buffer.size).also { array ->
            for (i in array.indices) {
                array[i] = buffer.getElem(i).toByte()
            }
        }
    }

    // Convert to Skia image then to Compose ImageBitmap
    val skiaImage = SkiaImage.makeFromEncoded(
        // Convert ARGB to PNG bytes for Skia
        bufferedImage.let { img ->
            val baos = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(img, "png", baos)
            baos.toByteArray()
        }
    )

    Image(
        painter = BitmapPainter(skiaImage.toComposeImageBitmap()),
        contentDescription = contentDescription,
        modifier = modifier,
        alpha = alpha,
        colorFilter = if (alpha < 1f) ColorFilter.tint(IntelliJTheme.colors.foreground.copy(alpha = alpha)) else null
    )
}
