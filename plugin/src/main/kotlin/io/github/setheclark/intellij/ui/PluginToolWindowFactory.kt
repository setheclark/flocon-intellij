package io.github.setheclark.intellij.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.Content
import com.intellij.ui.tabs.impl.MorePopupAware
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

        (toolWindow as? ToolWindowEx)?.setTabActions(showHiddenTabsAction(toolWindow))
    }

    private fun showHiddenTabsAction(toolWindow: ToolWindow): AnAction =
        object : AnAction(AllIcons.General.ChevronDown), DumbAware {

            override fun getActionUpdateThread() = ActionUpdateThread.EDT

            override fun update(e: AnActionEvent) {
                // Only show the button when the tab strip is actually overflowing.
                val morePopupAware = MorePopupAware.KEY.getData(e.dataContext)
                e.presentation.isVisible = morePopupAware?.canShowMorePopup() == true
            }

            override fun actionPerformed(e: AnActionEvent) {
                val contentManager = toolWindow.contentManager
                val contents = contentManager.contents.toList()

                val step = object : BaseListPopupStep<Content>(null, contents) {
                    override fun getTextFor(value: Content) = value.displayName ?: ""
                    override fun onChosen(value: Content, finalChoice: Boolean): PopupStep<*>? {
                        contentManager.setSelectedContent(value, true)
                        return FINAL_CHOICE
                    }
                }

                val popup = JBPopupFactory.getInstance().createListPopup(step)
                // Position under the chevron button that was clicked, not under a tab.
                val clickSource = e.inputEvent?.component
                if (clickSource != null) {
                    popup.showUnderneathOf(clickSource)
                } else {
                    popup.showInBestPositionFor(e.dataContext)
                }
            }
        }
}
