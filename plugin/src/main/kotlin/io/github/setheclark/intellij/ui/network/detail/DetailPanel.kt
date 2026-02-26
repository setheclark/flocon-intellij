package io.github.setheclark.intellij.ui.network.detail

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.JBUI
import io.github.setheclark.intellij.PluginBundle
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.ui.network.detail.overview.OverviewPanel
import io.github.setheclark.intellij.ui.network.detail.request.RequestPanel
import io.github.setheclark.intellij.ui.network.detail.response.ResponsePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel

class DetailPanel(
    private val project: Project,
    private val scope: CoroutineScope,
    private val viewModel: DetailPanelViewModel,
    private val overviewPanel: OverviewPanel,
    private val requestPanel: RequestPanel,
    private val responsePanel: ResponsePanel,
) : JPanel(BorderLayout()) {

    private val tabs = JBTabsFactory.createTabs(project)

    private val emptyLabel = JBLabel(PluginBundle.message("label.selectRequest")).apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    init {
        border = JBUI.Borders.customLineLeft(JBColor.border())

        tabs.addTab(TabInfo(overviewPanel).setText(PluginBundle.message("tab.overview")))
        tabs.addTab(TabInfo(responsePanel).setText(PluginBundle.message("tab.response")))
        tabs.addTab(TabInfo(requestPanel).setText(PluginBundle.message("tab.request")))

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
        add(tabs.component, BorderLayout.CENTER)
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
