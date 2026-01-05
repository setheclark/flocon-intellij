package io.github.setheclark.intellij.ui.network.detail

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.UiCoroutineScope
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.ui.network.detail.overview.OverviewPanel
import io.github.setheclark.intellij.ui.network.detail.request.RequestPanel
import io.github.setheclark.intellij.ui.network.detail.response.ResponsePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel

@Inject
class DetailPanel(
    @param:UiCoroutineScope private val scope: CoroutineScope,
    private val viewModel: DetailPanelViewModel,
    private val overviewPanel: OverviewPanel,
    private val requestPanel: RequestPanel,
    private val responsePanel: ResponsePanel,
) : JPanel(BorderLayout()) {

    private val tabbedPane = JBTabbedPane()

    private val emptyLabel = JBLabel("Select a request to view details").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    init {
        border = JBUI.Borders.customLineLeft(JBColor.border())

        tabbedPane.apply {
            addTab("Overview", overviewPanel)
            addTab("Response", responsePanel)
            addTab("Request", requestPanel)
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

    private fun updateDetails(call: NetworkCallEntity) {
        overviewPanel.showOverview(call)
        requestPanel.showRequest(call.request, call.name, call.startTime)
        responsePanel.showResponse(call.response, call.name, call.startTime)
    }
}
