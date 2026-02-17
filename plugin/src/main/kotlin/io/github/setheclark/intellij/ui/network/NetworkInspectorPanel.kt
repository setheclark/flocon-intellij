package io.github.setheclark.intellij.ui.network

import co.touchlab.kermit.Logger
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.settings.CallDetailOpenMode
import io.github.setheclark.intellij.settings.NetworkStorageSettingsState
import io.github.setheclark.intellij.ui.WarningBanner
import io.github.setheclark.intellij.ui.network.detail.DetailPanelFactory
import io.github.setheclark.intellij.ui.network.editor.NetworkCallVirtualFile
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterPanel
import io.github.setheclark.intellij.ui.network.list.NetworkCallListPanel
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

@Inject
class NetworkInspectorPanel(
    private val project: Project,
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkInspectorViewModel,
    private val networkCallListPanel: NetworkCallListPanel,
    private val detailPanelFactory: DetailPanelFactory,
    private val filterPanel: NetworkFilterPanel,
) : SimpleToolWindowPanel(false, true) {

    private val log = Logger.withPluginTag("NetworkInspectorPanel")

    private val statusLabel = JBLabel()
    private val warningBanner = WarningBanner()
    private lateinit var actionToolbar: ActionToolbar
    private val openTabs: MutableMap<String, Content> = mutableMapOf()
    private val openFiles: MutableMap<String, NetworkCallVirtualFile> = mutableMapOf()

    init {
        // Filter row + warning banner span the full width at the top
        val filterAndWarningPanel = JPanel(BorderLayout()).apply {
            add(filterPanel, BorderLayout.NORTH)
            add(warningBanner, BorderLayout.SOUTH)
        }

        // Vertical icons sit to the left of the list, below the filter row
        val listWithToolbar = JPanel(BorderLayout()).apply {
            add(createToolbarPanel(), BorderLayout.WEST)
            add(networkCallListPanel, BorderLayout.CENTER)
        }

        val contentPanel = JPanel(BorderLayout()).apply {
            add(filterAndWarningPanel, BorderLayout.NORTH)
            add(listWithToolbar, BorderLayout.CENTER)
            add(createStatusBar(), BorderLayout.SOUTH)
        }

        setContent(contentPanel)

        // Observe state from ViewModel
        observeState()
    }

    private fun createToolbarPanel(): JComponent {
        val actionGroup = DefaultActionGroup().apply {
            add(ClearAction())
            addSeparator()
            add(AutoScrollAction())
            addSeparator()
            add(StartStopServerAction())
        }

        actionToolbar = ActionManager.getInstance()
            .createActionToolbar("NetworkToolbar", actionGroup, false)
            .apply {
                targetComponent = this@NetworkInspectorPanel
            }

        return actionToolbar.component
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

        // Observe action state to update toolbar toggle button
        viewModel.latestUpdate({ it.autoScrollEnabled to it.serverState }) {
            actionToolbar.updateActionsAsync()
        }

        // Observe call-open events
        scope.launch {
            viewModel.openCallInTabEvent.collect { (callId, callName) ->
                openCallDetails(callId, callName)
            }
        }

        // Subscribe for editor window cleanup
        val busConnection = project.messageBus.connect()
        busConnection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    if (file is NetworkCallVirtualFile) {
                        openFiles.remove(file.callId)
                    }
                }
            }
        )
        scope.launch {
            try { awaitCancellation() } finally { busConnection.disconnect() }
        }
    }

    private fun openCallDetails(callId: String, callName: String) {
        when (NetworkStorageSettingsState.getInstance().callDetailOpenMode) {
            CallDetailOpenMode.TOOL_WINDOW_TAB -> openAsToolWindowTab(callId, callName)
            CallDetailOpenMode.EDITOR_WINDOW -> openAsEditorWindow(callId, callName)
        }
    }

    private fun openAsToolWindowTab(callId: String, callName: String) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Network Inspector") ?: return
        val contentManager = toolWindow.contentManager

        val existing = openTabs[callId]
        if (existing != null) {
            contentManager.setSelectedContent(existing)
            return
        }

        val tabTitle = if (callName.length > 30) callName.take(30) + "…" else callName
        val (panel, tabScope) = detailPanelFactory.create(callId)

        val content = ContentFactory.getInstance().createContent(panel, tabTitle, true)
        content.isCloseable = true
        content.setDisposer(Disposable {
            tabScope.cancel()
            openTabs.remove(callId)
        })

        openTabs[callId] = content
        contentManager.addContent(content)
        contentManager.setSelectedContent(content)
    }

    private fun openAsEditorWindow(callId: String, callName: String) {
        val file = openFiles.getOrPut(callId) {
            val title = if (callName.length > 60) callName.take(60) + "…" else callName
            NetworkCallVirtualFile(callId, title)
        }
        FileEditorManager.getInstance(project).openFile(file, true)
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
