package io.github.setheclark.intellij.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import io.github.setheclark.intellij.PluginBundle
import io.github.setheclark.intellij.di.appGraph
import io.github.setheclark.intellij.ui.network.NetworkInspectorContent
import org.jetbrains.jewel.bridge.addComposeTab

/**
 * Factory for creating the Flocon tool window.
 * Registered in plugin.xml as a tool window factory.
 */
class PluginToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        const val ID = "Network Inspector"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val uiGraph = appGraph.create(project)

        toolWindow.addComposeTab(
            tabDisplayName = PluginBundle.message("tab.network"),
            focusOnClickInside = false,
        ) {
            NetworkInspectorContent(uiGraph)
        }
    }
}
