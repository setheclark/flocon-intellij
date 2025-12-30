package io.github.setheclark.intellij.ui.network

import co.touchlab.kermit.Logger
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.di.UiCoroutineScope
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.ui.WarningBanner
import io.github.setheclark.intellij.ui.network.detail.DetailPanel
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterPanel
import io.github.setheclark.intellij.ui.network.list.NetworkCallListPanel
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel

@Inject
class NetworkInspectorPanel(
    @param:UiCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkInspectorViewModel,
    private val networkCallListPanel: NetworkCallListPanel,
    private val detailPanel: DetailPanel,
    private val filterPanel: NetworkFilterPanel,
) : SimpleToolWindowPanel(true, true) {

    private val log = Logger.withPluginTag("NetworkInspectorPanel")

    private val statusLabel = JBLabel()
    private val warningBanner = WarningBanner()
    private var mainSplitter: JBSplitter

    init {
        // Create combined toolbar with actions and filters
        val toolbarPanel = createToolbarPanel()
        setToolbar(toolbarPanel)

        // Create main content with split pane (horizontal: list on left, details on right)
        mainSplitter = JBSplitter(false, 0.35f).apply {
            firstComponent = networkCallListPanel
            secondComponent = null  // Hidden initially until a request is selected
            setHonorComponentsMinimumSize(true)
        }

        // Add status bar at bottom
        val contentPanel = JPanel(BorderLayout()).apply {
            add(warningBanner, BorderLayout.NORTH)
            add(mainSplitter, BorderLayout.CENTER)
            add(createStatusBar(), BorderLayout.SOUTH)
        }

        setContent(contentPanel)

        // Observe state from ViewModel
        observeState()
    }

    private fun createToolbarPanel(): JPanel {
        val actionGroup = DefaultActionGroup().apply {
            add(ClearAction())
            addSeparator()
            add(AutoScrollAction())
            addSeparator()
            add(StartStopServerAction())
        }

        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("NetworkToolbar", actionGroup, true)
            .apply {
                targetComponent = this@NetworkInspectorPanel
            }

        // Combine action toolbar and filter panel
        return JPanel(BorderLayout()).apply {
            add(actionToolbar.component, BorderLayout.WEST)
            add(filterPanel, BorderLayout.CENTER)
        }
    }


    private fun observeState() {
        // Observe server state for status bar
        viewModel.latestUpdate(NetworkInspectorState::serverState) { serverState ->
            updateStatusLabel(serverState)
        }

        // Observe ADB status for warning banner
        viewModel.latestUpdate({ it.serverState to it.adbStatus }) { (serverState, adbState) ->
            updateWarningBanner(adbState, serverState)
        }

        // Observe selected call to show/hide detail panel
        viewModel.latestUpdate(NetworkInspectorState::isDetailVisible) { isVisible ->
            log.v { "isDetailVisible: $isVisible" }
            mainSplitter.secondComponent = if (isVisible) detailPanel else null
        }
    }

    private fun <T> NetworkInspectorViewModel.latestUpdate(
        transform: (NetworkInspectorState) -> T,
        block: (T) -> Unit,
    ) {
        scope.launch {
            state.map(transform).distinctUntilChanged().collectLatest { t ->
                block(t)
            }
        }
    }

    private fun createStatusBar(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 8)
            add(statusLabel, BorderLayout.WEST)
        }
    }

    private fun updateWarningBanner(
        adbStatus: AdbStatus,
        serverStatus: MessageServerState
    ) {
        when {
            adbStatus == AdbStatus.NotFound -> {
                warningBanner.setText(
                    "<html><b>ADB not found.</b> USB device connections won't work. " +
                            "Add 'adb' to PATH or set ANDROID_HOME environment variable.</html>"
                )
                warningBanner.isVisible = true
            }

            serverStatus is MessageServerState.Error -> {
                val message = serverStatus.message
                warningBanner.setText(
                    "<html><b>ERROR: '$message'</b> You may have the Flocon desktop app running.  " +
                            "If so, close the app and click the server retry action above.</html>"
                )
                warningBanner.isVisible = true
            }

            else -> {
                warningBanner.isVisible = false
            }
        }
        warningBanner.revalidate()
        warningBanner.repaint()
    }

    private fun updateStatusLabel(state: MessageServerState) {
        statusLabel.text = when (state) {
            MessageServerState.Stopped -> "Message server stopped"
            MessageServerState.Running -> "Message server running"
            is MessageServerState.Error -> "Error: ${state.message}"
            MessageServerState.Starting -> "Message server starting"
            MessageServerState.Stopping -> "Message server stopping"
        }
    }

    // Action implementations - dispatch intents to ViewModel

    private inner class ClearAction : AnAction(
        "Clear All",
        "Clear all captured network traffic",
        AllIcons.Actions.GC
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            viewModel.dispatch(NetworkInspectorIntent.ClearAll)
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    private inner class AutoScrollAction : ToggleAction(
        "Auto-scroll to latest",
        "Automatically scroll to show new requests",
        AllIcons.RunConfigurations.Scroll_down
    ) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return viewModel.state.value.autoScrollEnabled
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state) {
                viewModel.dispatch(NetworkInspectorIntent.EnableAutoScroll)
            } else {
                viewModel.dispatch(NetworkInspectorIntent.DisableAutoScroll)
            }
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class StartStopServerAction : AnAction() {
        override fun update(e: AnActionEvent) {
            when (viewModel.state.value.serverState) {
                is MessageServerState.Running -> {
                    e.presentation.text = "Stop Message Server"
                    e.presentation.description = "Stop the server"
                    e.presentation.icon = AllIcons.Actions.Suspend
                    e.presentation.isEnabled = true
                }

                is MessageServerState.Stopped -> {
                    e.presentation.text = "Start Message Server"
                    e.presentation.description = "Start the server"
                    e.presentation.icon = AllIcons.Actions.Execute
                    e.presentation.isEnabled = true
                }

                is MessageServerState.Error -> {
                    e.presentation.text = "Retry Message Server"
                    e.presentation.description = "Retry starting the server"
                    e.presentation.icon = AllIcons.Actions.Restart
                    e.presentation.isEnabled = true
                }

                is MessageServerState.Starting -> {
                    e.presentation.icon = AllIcons.Actions.Execute
                    e.presentation.isEnabled = false
                }

                is MessageServerState.Stopping -> {
                    e.presentation.icon = AllIcons.Actions.Suspend
                    e.presentation.isEnabled = false
                }
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            when (viewModel.state.value.serverState) {
                is MessageServerState.Running -> viewModel.dispatch(NetworkInspectorIntent.StopServer)
                is MessageServerState.Stopped, is MessageServerState.Error -> viewModel.dispatch(NetworkInspectorIntent.StartServer)
                else -> { /* ignore */
                }
            }
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}
