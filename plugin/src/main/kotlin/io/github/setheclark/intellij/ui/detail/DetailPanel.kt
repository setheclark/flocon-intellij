package io.github.setheclark.intellij.ui.detail

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.UiCoroutineScope
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Panel displaying detailed information about a selected network call.
 * Contains tabs for Headers, Request Body, Response Body, and Timing.
 *
 * Observes [DetailPanelViewModel] for the selected call.
 *
 * Uses injected [UiCoroutineScope] for coroutines - lifecycle managed by [io.github.setheclark.intellij.ui.UiScopeDisposable].
 */
@Inject
class DetailPanel(
    @param:UiCoroutineScope private val scope: CoroutineScope,
    private val viewModel: DetailPanelViewModel,
    private val timingPanel: TimingPanel,
    private val requestBodyPanel: BodyPanel,
    private val responseBodyPanel: BodyPanel,
) : JPanel(BorderLayout()) {

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
            viewModel.selectedCall.collectLatest { call ->
                if (call != null) {
                    updateDetails(call)
                    showDetails()
                } else {
                    showEmpty()
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
}
