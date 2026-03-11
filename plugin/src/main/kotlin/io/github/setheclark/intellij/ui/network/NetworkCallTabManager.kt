package io.github.setheclark.intellij.ui.network

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.di.ProjectScope
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.settings.CallDetailOpenMode
import io.github.setheclark.intellij.settings.NetworkStorageSettingsState
import io.github.setheclark.intellij.ui.PluginToolWindowFactory
import io.github.setheclark.intellij.ui.network.details.DetailPanelFactory
import io.github.setheclark.intellij.ui.network.editor.NetworkCallVirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Inject
@SingleIn(ProjectScope::class)
class NetworkCallTabManager(
    private val project: Project,
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkInspectorViewModel,
    private val detailPanelFactory: DetailPanelFactory,
) {

    private val openTabs: MutableMap<String, Content> = mutableMapOf()
    private val openFiles: MutableMap<String, NetworkCallVirtualFile> = mutableMapOf()

    init {
        observeState()
    }

    private fun observeState() {
        // Observe call-open events
        scope.launch {
            viewModel.openCallInTabEvent.collect { (callId, callName) ->
                ApplicationManager.getApplication().invokeLater {
                    openCallDetails(callId, callName)
                }
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
            },
        )
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                busConnection.disconnect()
            }
        }
    }

    private fun openCallDetails(callId: String, callName: String) {
        when (NetworkStorageSettingsState.getInstance().callDetailOpenMode) {
            CallDetailOpenMode.TOOL_WINDOW_TAB -> openAsToolWindowTab(callId, callName)
            CallDetailOpenMode.EDITOR_WINDOW -> openAsEditorWindow(callId, callName)
        }
    }

    private fun openAsToolWindowTab(callId: String, callName: String) {
        val toolWindow = ToolWindowManager
            .getInstance(project)
            .getToolWindow(PluginToolWindowFactory.ID) ?: return
        val contentManager = toolWindow.contentManager

        val existing = openTabs[callId]
        if (existing != null) {
            contentManager.setSelectedContent(existing)
            return
        }

        val tabTitle = if (callName.length > 30) callName.take(30) + "…" else callName
        val panel = detailPanelFactory.create(callId)

        val content = ContentFactory.getInstance().createContent(panel, tabTitle, true)
        content.isCloseable = true
        Disposer.register(content) {
            openTabs.remove(callId)
        }

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
}
