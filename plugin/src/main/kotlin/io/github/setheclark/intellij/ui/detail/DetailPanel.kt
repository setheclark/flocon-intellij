package io.github.setheclark.intellij.ui.detail

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.services.FloconProjectService
import io.github.setheclark.intellij.services.NetworkCallEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.awt.BorderLayout
import java.awt.Font
import java.time.Instant
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Panel displaying detailed information about a selected network call.
 * Contains tabs for Headers, Request Body, Response Body, and Timing.
 */
@Inject
class DetailPanel(
    private val floconService: FloconProjectService,
    private val timingPanel: TimingPanel,
    private val requestBodyPanel: BodyPanel,
    private val responseBodyPanel: BodyPanel,
) : JPanel(BorderLayout()), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val tabbedPane = JBTabbedPane()
    private val headersPanel = HeadersPanel()

    private val emptyLabel = JBLabel("Select a request to view details").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    init {
        tabbedPane.apply {
            addTab("Headers", headersPanel)
            addTab("Request", requestBodyPanel)
            addTab("Response", responseBodyPanel)
            addTab("Timing", timingPanel)
        }

        showEmpty()
        observeSelectedCall()
    }

    private fun showEmpty() {
        removeAll()
        add(emptyLabel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun showDetails() {
        removeAll()
        add(tabbedPane, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun observeSelectedCall() {
        scope.launch {
            floconService.selectedCall.collectLatest { call ->
                SwingUtilities.invokeLater {
                    if (call != null) {
                        updateDetails(call)
                        showDetails()
                    } else {
                        showEmpty()
                    }
                }
            }
        }
    }

    private fun updateDetails(call: NetworkCallEntry) {
        headersPanel.showHeaders(call)
        requestBodyPanel.showBody(call.request.body, call.request.contentType)
        responseBodyPanel.showBody(call.response?.body, call.response?.contentType)
        timingPanel.showTiming(call)
    }

    override fun dispose() {
        scope.cancel()
    }
}
