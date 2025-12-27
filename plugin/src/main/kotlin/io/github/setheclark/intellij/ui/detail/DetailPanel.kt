package io.github.setheclark.intellij.ui.detail

import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.ui.mvi.NetworkInspectorViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Panel displaying detailed information about a selected network call.
 * Contains tabs for Headers, Request Body, Response Body, and Timing.
 *
 * Observes [NetworkInspectorViewModel.state] for the selected call.
 */
@Inject
class DetailPanel(
    private val viewModel: NetworkInspectorViewModel,
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
            viewModel.state
                .map { it.selectedCall }
                .distinctUntilChanged()
                .collectLatest { call ->
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
