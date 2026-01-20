package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.ui.network.detail.common.BodyContentPanel
import io.github.setheclark.intellij.ui.network.detail.common.ScratchFileContext

/**
 * Compose wrapper for Swing BodyContentPanel.
 *
 * Temporary wrapper during migration (Phase 2.2-4.1).
 * Will be wrapped more cleanly in Phase 4.1 (BodyContentPanel must remain Swing
 * due to EditorTextField having no Compose equivalent).
 *
 * @param project IntelliJ project for editor integration
 * @param body Body content to display
 * @param contentType MIME type for syntax highlighting
 * @param context Scratch file context for "Open in Editor" action
 * @param modifier Optional modifier
 */
@Composable
fun BodyContentView(
    project: Project,
    body: String?,
    contentType: String?,
    context: ScratchFileContext?,
    modifier: Modifier = Modifier
) {
    val panel = remember { BodyContentPanel(project) }

    SwingPanel(
        factory = { panel },
        update = {
            // EditorTextField creation requires read access
            // Use invokeLater to allow proper editor initialization while still providing read access
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().runReadAction {
                    panel.showBody(body, contentType, context)
                }
            }
        },
        modifier = modifier
    )
}
