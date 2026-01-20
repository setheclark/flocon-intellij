package io.github.setheclark.intellij.ui.compose.bridge

import androidx.compose.runtime.Composable
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.jewel.bridge.JewelComposePanel
import javax.swing.JComponent

/**
 * Creates a JewelComposePanel that hosts Compose content with Jewel theme integration.
 *
 * This is useful for embedding Compose content in existing Swing containers
 * during the migration phase.
 *
 * Note: For tool windows, prefer using Jewel's `addComposeTab` directly.
 *
 * @param parent Optional parent Disposable for lifecycle management
 * @param content The Compose content to display
 * @return A JComponent containing the Compose content
 */
fun createComposePanel(
    parent: Disposable? = null,
    content: @Composable () -> Unit
): JComponent {
    val composePanel = JewelComposePanel(content = content)

    if (parent != null && composePanel is Disposable) {
        Disposer.register(parent, composePanel as Disposable)
    }

    return composePanel
}
