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
) : JPanel(BorderLayout()) {

    private val tabs = JBTabsFactory.createTabs(project)

    private val emptyLabel = JBLabel("Select a request to view details").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    // Compose panel wrappers
    private val overviewPanelWrapper = JPanel(BorderLayout())
    private var currentOverviewPanel: javax.swing.JComponent? = null

    private val requestPanelWrapper = JPanel(BorderLayout())
    private var currentRequestPanel: javax.swing.JComponent? = null

    private val responsePanelWrapper = JPanel(BorderLayout())
    private var currentResponsePanel: javax.swing.JComponent? = null

    init {
        border = JBUI.Borders.customLineLeft(JBColor.border())

        tabs.addTab(TabInfo(overviewPanelWrapper).setText("Overview"))
        tabs.addTab(TabInfo(responsePanelWrapper).setText("Response"))
        tabs.addTab(TabInfo(requestPanelWrapper).setText("Request"))

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
        updateRequestPanel(call)
        updateResponsePanel(call)
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
        val newPanel = JewelComposePanel(
            focusOnClickInside = false,
            content = {
                io.github.setheclark.intellij.ui.compose.components.NetworkOverview(
                    call = call,
                    modifier = Modifier
                )
            }
        )

        overviewPanelWrapper.add(newPanel, BorderLayout.CENTER)
        currentOverviewPanel = newPanel

        overviewPanelWrapper.revalidate()
        overviewPanelWrapper.repaint()
    }

    /**
     * Updates the request panel by recreating the Compose panel with new data.
     *
     * This approach is necessary due to Compose-Swing interop limitations.
     * Will be simplified in Phase 4.2 when migrating to pure Compose.
     */
    private fun updateRequestPanel(call: NetworkCallEntity) {
        // Remove old panel if exists
        currentRequestPanel?.let { requestPanelWrapper.remove(it) }

        // Create new Compose panel with the request data
        val newPanel = JewelComposePanel(
            focusOnClickInside = true,
            content = {
                io.github.setheclark.intellij.ui.compose.components.RequestView(
                    project = project,
                    request = call.request,
                    callName = call.name,
                    timestamp = call.startTime,
                    modifier = Modifier
                )
            }
        )

        requestPanelWrapper.add(newPanel, BorderLayout.CENTER)
        currentRequestPanel = newPanel

        requestPanelWrapper.revalidate()
        requestPanelWrapper.repaint()
    }

    /**
     * Updates the response panel by recreating the Compose panel with new data.
     *
     * This approach is necessary due to Compose-Swing interop limitations.
     * Will be simplified in Phase 4.2 when migrating to pure Compose.
     */
    private fun updateResponsePanel(call: NetworkCallEntity) {
        // Remove old panel if exists
        currentResponsePanel?.let { responsePanelWrapper.remove(it) }

        // Create new Compose panel with the response data
        val newPanel = JewelComposePanel(
            focusOnClickInside = true,
            content = {
                io.github.setheclark.intellij.ui.compose.components.ResponseView(
                    project = project,
                    response = call.response,
                    callName = call.name,
                    timestamp = call.startTime,
                    modifier = Modifier
                )
            }
        )

        responsePanelWrapper.add(newPanel, BorderLayout.CENTER)
        currentResponsePanel = newPanel

        responsePanelWrapper.revalidate()
        responsePanelWrapper.repaint()
    }
}
