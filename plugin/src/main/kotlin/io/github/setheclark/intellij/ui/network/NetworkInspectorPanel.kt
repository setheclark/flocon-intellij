package io.github.setheclark.intellij.ui.network

import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
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
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.ui.network.detail.DetailPanel
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterViewModel
import io.github.setheclark.intellij.ui.network.list.NetworkCallListPanel
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.jewel.bridge.JewelComposePanel
import java.awt.BorderLayout
import javax.swing.JPanel

@Inject
class NetworkInspectorPanel(
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkInspectorViewModel,
    private val filterViewModel: NetworkFilterViewModel,
    private val networkCallListPanel: NetworkCallListPanel,
    private val detailPanel: DetailPanel,
) : SimpleToolWindowPanel(true, true) {

    private val log = Logger.withPluginTag("NetworkInspectorPanel")

    private val statusLabel = JBLabel()
    private lateinit var actionToolbar: ActionToolbar
    private val mainSplitter: JBSplitter

    // Warning banner wrapper
    private val warningBannerWrapper = JPanel(BorderLayout())
    private var currentWarningPanel: javax.swing.JComponent? = null

    // Filter bar wrapper (created once, handles its own state reactivity)
    private val filterBarWrapper = JPanel(BorderLayout())

    init {
        // Create combined toolbar with actions and filters
        val toolbarPanel = createToolbarPanel()
        toolbar = toolbarPanel

        // Observe state and update warning banner
        // Note: Using Swing update pattern due to Compose-Swing interop limitations
        // This will be simplified in Phase 5 when migrating to pure Compose
        scope.launch {
            viewModel.state.collect { state ->
                val warningText = when {
                    state.adbStatus == AdbStatus.NotFound -> {
                        "ADB not found. USB device connections won't work. " +
                                "Add 'adb' to PATH or set ANDROID_HOME environment variable."
                    }
                    state.serverState is MessageServerState.Error -> {
                        val message = (state.serverState as MessageServerState.Error).message
                        "ERROR: '$message' - You may have the Flocon desktop app running. " +
                                "If so, close the app and click the server retry action above."
                    }
                    else -> ""
                }

                val isWarningVisible = state.adbStatus == AdbStatus.NotFound || state.serverState is MessageServerState.Error

                // Update UI on EDT
                javax.swing.SwingUtilities.invokeLater {
                    updateWarningBanner(warningText, isWarningVisible)
                }
            }
        }

        // Create filter bar once - it handles its own state reactivity via NetworkFilterBarContainer
        val filterPanel = JewelComposePanel(
            focusOnClickInside = true,
            content = {
                io.github.setheclark.intellij.ui.compose.components.NetworkFilterBarContainer(
                    viewModel = filterViewModel,
                    modifier = Modifier
                )
            }
        )
        filterBarWrapper.add(filterPanel, BorderLayout.CENTER)

        // Create main content with split pane (horizontal: list on left, details on right)
        mainSplitter = JBSplitter(false, 0.35f).apply {
            firstComponent = networkCallListPanel
            secondComponent = null  // Hidden initially until a request is selected
            setHonorComponentsMinimumSize(false)
        }

        // Add status bar at bottom
        val contentPanel = JPanel(BorderLayout()).apply {
            add(warningBannerWrapper, BorderLayout.NORTH)
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

        actionToolbar = ActionManager.getInstance()
            .createActionToolbar("NetworkToolbar", actionGroup, true)
            .apply {
                targetComponent = this@NetworkInspectorPanel
            }

        // Combine action toolbar and filter panel
        return JPanel(BorderLayout()).apply {
            add(actionToolbar.component, BorderLayout.WEST)
            add(filterBarWrapper, BorderLayout.CENTER)
        }
    }


    private fun observeState() {
        // Observe server state for status bar
        viewModel.latestUpdate(NetworkInspectorState::serverState) { serverState ->
            updateStatusLabel(serverState)
        }

        // Warning banner is now handled by Compose state collection

        // Observe selected call to show/hide detail panel
        viewModel.latestUpdate(NetworkInspectorState::isDetailVisible) { isVisible ->
            log.v { "isDetailVisible: $isVisible" }
            mainSplitter.secondComponent = if (isVisible) detailPanel else null
        }

        // Observe action state to update toolbar toggle button
        viewModel.latestUpdate({ it.autoScrollEnabled to it.serverState }) {
            actionToolbar.updateActionsAsync()
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

    /**
     * Updates the warning banner by recreating the Compose panel.
     *
     * This approach is necessary due to Compose-Swing interop limitations where
     * state changes inside JewelComposePanel don't trigger recomposition properly.
     * Will be simplified in Phase 5 when migrating to pure Compose.
     */
    private fun updateWarningBanner(text: String, isVisible: Boolean) {
        // Remove old panel if exists
        currentWarningPanel?.let { warningBannerWrapper.remove(it) }

        if (isVisible && text.isNotEmpty()) {
            // Create new Compose panel with the warning
            val newPanel = JewelComposePanel(
                focusOnClickInside = false,
                content = {
                    io.github.setheclark.intellij.ui.compose.components.WarningBanner(
                        text = text,
                        isVisible = true,
                        modifier = Modifier
                    )
                }
            )

            warningBannerWrapper.add(newPanel, BorderLayout.CENTER)
            currentWarningPanel = newPanel
        } else {
            currentWarningPanel = null
        }

        warningBannerWrapper.revalidate()
        warningBannerWrapper.repaint()
    }


    private fun createStatusBar(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 8)
            add(statusLabel, BorderLayout.WEST)
        }
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
