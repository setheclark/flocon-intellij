package io.github.setheclark.intellij.ui.network.detail

import androidx.compose.ui.Modifier
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.ui.network.detail.request.RequestPanel
import io.github.setheclark.intellij.ui.network.detail.response.ResponsePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.jewel.bridge.JewelComposePanel
import java.awt.BorderLayout
import javax.swing.JPanel

@Inject
class DetailPanel(
    private val project: Project,
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val viewModel: DetailPanelViewModel,
    private val requestPanel: RequestPanel,
    private val responsePanel: ResponsePanel,
) : JPanel(BorderLayout()) {

    private val tabs = JBTabsFactory.createTabs(project)

    private val emptyLabel = JBLabel("Select a request to view details").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    // Compose overview panel wrapper
    private val overviewPanelWrapper = JPanel(BorderLayout())
    private var currentOverviewPanel: javax.swing.JComponent? = null

    init {
        border = JBUI.Borders.customLineLeft(JBColor.border())

        tabs.addTab(TabInfo(overviewPanelWrapper).setText("Overview"))
        tabs.addTab(TabInfo(responsePanel).setText("Response"))
        tabs.addTab(TabInfo(requestPanel).setText("Request"))

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
        updateOverviewPanel(call)
        requestPanel.showRequest(call.request, call.name, call.startTime)
        responsePanel.showResponse(call.response, call.name, call.startTime)
    }

    /**
     * Updates the overview panel by recreating the Compose panel with new data.
     *
     * This approach is necessary due to Compose-Swing interop limitations.
     * Will be simplified in Phase 4.2 when migrating to pure Compose.
     */
    private fun updateOverviewPanel(call: NetworkCallEntity?) {
        // Remove old panel if exists
        currentOverviewPanel?.let { overviewPanelWrapper.remove(it) }

        // Create new Compose panel with the call data
        val newPanel = JewelComposePanel(content = {
            io.github.setheclark.intellij.ui.compose.components.NetworkOverview(
                call = call,
                modifier = Modifier
            )
        })

        overviewPanelWrapper.add(newPanel, BorderLayout.CENTER)
        currentOverviewPanel = newPanel

        overviewPanelWrapper.revalidate()
        overviewPanelWrapper.repaint()
    }
}
