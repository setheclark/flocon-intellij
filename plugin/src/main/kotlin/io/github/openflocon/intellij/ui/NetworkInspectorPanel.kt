package io.github.openflocon.intellij.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import io.github.openflocon.intellij.services.FloconProjectService
import io.github.openflocon.intellij.services.ServerState
import io.github.openflocon.intellij.ui.detail.DetailPanel
import io.github.openflocon.intellij.ui.list.NetworkCallListPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Main panel for the Flocon Network Inspector tool window.
 * Contains the toolbar, network call list, and detail panel.
 */
class NetworkInspectorPanel(
    private val project: Project
) : SimpleToolWindowPanel(true, true), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val floconService = project.service<FloconProjectService>()

    private val networkCallListPanel = NetworkCallListPanel(project)
    private val detailPanel = DetailPanel(project)
    private val statusLabel = JBLabel()

    init {
        // Create toolbar
        val toolbar = createToolbar()
        setToolbar(toolbar.component)

        // Create main content with split pane
        val mainSplitter = JBSplitter(false, 0.5f).apply {
            firstComponent = JBScrollPane(networkCallListPanel)
            secondComponent = detailPanel
        }

        // Add status bar at bottom
        val contentPanel = JPanel(BorderLayout()).apply {
            add(mainSplitter, BorderLayout.CENTER)
            add(createStatusBar(), BorderLayout.SOUTH)
        }

        setContent(contentPanel)

        // Observe server state for status bar
        observeServerState()
    }

    private fun createToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(ClearAction())
            addSeparator()
            add(StartStopServerAction())
        }

        return ActionManager.getInstance()
            .createActionToolbar("FloconNetworkToolbar", actionGroup, true)
            .apply {
                targetComponent = this@NetworkInspectorPanel
            }
    }

    private fun createStatusBar(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 8)
            add(statusLabel, BorderLayout.WEST)
        }
    }

    private fun observeServerState() {
        scope.launch {
            floconService.serverState.collectLatest { state ->
                SwingUtilities.invokeLater {
                    updateStatusLabel(state)
                }
            }
        }
    }

    private fun updateStatusLabel(state: ServerState) {
        statusLabel.text = when (state) {
            is ServerState.Stopped -> "Server stopped"
            is ServerState.Starting -> "Starting server..."
            is ServerState.Running -> "Server running on port ${state.port}"
            is ServerState.Stopping -> "Stopping server..."
            is ServerState.Error -> "Error: ${state.message}"
        }
    }

    override fun dispose() {
        scope.cancel()
    }

    // Action implementations

    private inner class ClearAction : AnAction(
        "Clear All",
        "Clear all captured network traffic",
        AllIcons.Actions.GC
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            floconService.clearAll()
        }
    }

    private inner class StartStopServerAction : AnAction() {
        override fun update(e: AnActionEvent) {
            val state = floconService.serverState.value
            when (state) {
                is ServerState.Running -> {
                    e.presentation.text = "Stop Server"
                    e.presentation.description = "Stop the Flocon server"
                    e.presentation.icon = AllIcons.Actions.Suspend
                    e.presentation.isEnabled = true
                }
                is ServerState.Stopped -> {
                    e.presentation.text = "Start Server"
                    e.presentation.description = "Start the Flocon server"
                    e.presentation.icon = AllIcons.Actions.Execute
                    e.presentation.isEnabled = true
                }
                else -> {
                    e.presentation.isEnabled = false
                }
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            val appService = service<io.github.openflocon.intellij.services.FloconApplicationService>()
            when (floconService.serverState.value) {
                is ServerState.Running -> appService.stopServer()
                is ServerState.Stopped -> appService.startServer()
                else -> { /* ignore */ }
            }
        }
    }
}
