package io.github.openflocon.intellij.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating the Flocon tool window.
 * Registered in plugin.xml as a tool window factory.
 */
class FloconToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val networkInspectorPanel = NetworkInspectorPanel(project)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            networkInspectorPanel,
            "Network",
            false
        )
        content.isCloseable = false

        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        // Tool window is always available
        return true
    }
}
