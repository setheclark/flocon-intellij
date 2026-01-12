package io.github.setheclark.intellij.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import io.github.setheclark.intellij.di.appGraph
import org.jetbrains.jewel.bridge.addComposeTab

/**
 * Factory for creating the Flocon tool window.
 * Registered in plugin.xml as a tool window factory.
 */
class FloconToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val uiGraph = project.appGraph.create(project)

        toolWindow.addComposeTab("Network") {
//            val backgroundColor by remember(JewelTheme.isDark) { mutableStateOf(JBColor.PanelBackground.toComposeColor()) }
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(backgroundColor)
//            ) {
//                Text("Testing...")
//            }

            SwingPanel(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    uiGraph.networkInspectorPanel
                }
            )
        }
        // Register UiScopeDisposable - cancels all UI coroutines when tool window closes
        content.setDisposer(uiGraph.viewModelScopeDisposable)

//        val contentFactory = ContentFactory.getInstance()
//        val content = contentFactory.createContent(
//            uiGraph.networkInspectorPanel,
//            "Network",
//            false
//        )
//        content.isCloseable = false
//
//        // Register UiScopeDisposable - cancels all UI coroutines when tool window closes
//        content.setDisposer(uiGraph.uiScopeDisposable)
//
//        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        // Tool window is always available
        return true
    }
}
