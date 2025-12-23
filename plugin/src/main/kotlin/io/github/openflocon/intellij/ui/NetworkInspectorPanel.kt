package io.github.openflocon.intellij.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import io.github.openflocon.intellij.services.AdbService
import io.github.openflocon.intellij.services.AdbStatus
import io.github.openflocon.intellij.services.FloconApplicationService
import io.github.openflocon.intellij.services.FloconProjectService
import io.github.openflocon.intellij.services.ServerState
import io.github.openflocon.intellij.ui.detail.DetailPanel
import io.github.openflocon.intellij.ui.filter.FilterPanel
import io.github.openflocon.intellij.ui.list.NetworkCallListPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
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
    private val adbService = service<AdbService>()

    private val networkCallListPanel = NetworkCallListPanel(project)
    private val detailPanel = DetailPanel(project)
    private val filterPanel = FilterPanel(project)
    private val statusLabel = JBLabel()
    private val warningBanner = createWarningBanner()

    init {
        // Create toolbar
        val toolbar = createToolbar()
        setToolbar(toolbar.component)

        // Create main content with split pane
        val mainSplitter = JBSplitter(false, 0.5f).apply {
            firstComponent = networkCallListPanel  // Panel handles its own scrolling
            secondComponent = detailPanel
        }

        // Create top panel with warning and filters
        val topPanel = JPanel(BorderLayout()).apply {
            add(warningBanner, BorderLayout.NORTH)
            add(filterPanel, BorderLayout.SOUTH)
        }

        // Add status bar at bottom
        val contentPanel = JPanel(BorderLayout()).apply {
            add(topPanel, BorderLayout.NORTH)
            add(mainSplitter, BorderLayout.CENTER)
            add(createStatusBar(), BorderLayout.SOUTH)
        }

        setContent(contentPanel)

        // Observe server state for status bar
        observeServerState()
        // Observe ADB status for warning banner
        observeAdbStatus()
    }

    private fun createToolbar(): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(ClearAction())
            addSeparator()
            add(AutoScrollAction())
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

    private fun createWarningBanner(): JPanel {
        val warningColor = JBColor(0xFFF3CD, 0x5C4813) // Yellow warning background
        val textColor = JBColor(0x856404, 0xFFE69C)    // Dark text on light, light text on dark

        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = warningColor
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(8, 12)
            )
            isVisible = false // Hidden by default

            val iconLabel = JBLabel(AllIcons.General.Warning)
            val textLabel = JBLabel().apply {
                foreground = textColor
            }

            add(iconLabel)
            add(textLabel)

            // Store text label for updates
            putClientProperty("textLabel", textLabel)
        }
    }

    private fun observeAdbStatus() {
        scope.launch {
            adbService.adbStatus.collectLatest { status ->
                SwingUtilities.invokeLater {
                    updateWarningBanner(status)
                }
            }
        }
    }

    private fun updateWarningBanner(status: AdbStatus) {
        val textLabel = warningBanner.getClientProperty("textLabel") as? JBLabel
        when (status) {
            is AdbStatus.NotFound -> {
                textLabel?.text = "<html><b>ADB not found.</b> USB device connections won't work. " +
                    "Add 'adb' to PATH or set ANDROID_HOME environment variable.</html>"
                warningBanner.isVisible = true
            }
            else -> {
                warningBanner.isVisible = false
            }
        }
        warningBanner.revalidate()
        warningBanner.repaint()
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
        filterPanel.dispose()
    }

    // Action implementations

    private inner class ClearAction : AnAction(
        "Clear All",
        "Clear all captured network traffic",
        AllIcons.Actions.GC
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            floconService.clearAll()
            networkCallListPanel.resetAutoScroll()
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    private inner class AutoScrollAction : ToggleAction(
        "Auto-scroll to Latest",
        "Automatically scroll to show new requests",
        AllIcons.RunConfigurations.Scroll_down
    ) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return networkCallListPanel.isAutoScrollEnabled()
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state) {
                networkCallListPanel.enableAutoScroll()
            }
            // When toggled off, we don't need to do anything -
            // user interaction already disables auto-scroll
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
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
            val appService = service<FloconApplicationService>()
            when (floconService.serverState.value) {
                is ServerState.Running -> appService.stopServer()
                is ServerState.Stopped -> appService.startServer()
                else -> { /* ignore */ }
            }
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}
