package io.github.setheclark.intellij.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import dev.zacsweers.metro.createGraphFactory

/**
 * Factory for creating the Flocon tool window.
 * Registered in plugin.xml as a tool window factory.
 */
class FloconToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        val graph = createGraphFactory<UiGraph.Factory>()
            .create(project)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(
            graph.networkInspectorPanel,
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
