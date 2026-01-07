package io.github.setheclark.intellij.ui.network.detail.response

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.ui.network.detail.common.BodyContentPanel
import io.github.setheclark.intellij.ui.network.detail.common.HeadersTablePanel
import io.github.setheclark.intellij.ui.network.detail.common.ScratchFileContext
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Panel containing nested tabs for response headers and body.
 * Shows empty state when response is null.
 */
@Inject
class ResponsePanel(
    private val project: Project,
    private val bodyPanel: BodyContentPanel,
) : JPanel(BorderLayout()) {

    private val tabs = JBTabsFactory.createTabs(project)
    private val headersPanel = HeadersTablePanel()

    private val emptyLabel = JBLabel("No response").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    init {
        tabs.addTab(TabInfo(headersPanel).setText("Headers"))
        tabs.addTab(TabInfo(bodyPanel).setText("Body"))
        showEmpty()
    }

    fun showResponse(response: NetworkResponse?, callName: String, timestamp: Long) {
        when (response) {
            is NetworkResponse.Success -> {
                headersPanel.showHeaders(response.headers)

                val context = response.body?.let {
                    ScratchFileContext(
                        queryName = callName,
                        bodyType = ScratchFileContext.BodyType.RESPONSE,
                        statusCode = response.statusCode,
                        timestamp = timestamp,
                        body = it,
                        contentType = response.contentType
                    )
                }

                bodyPanel.showBody(response.body, response.contentType, context)
                showContent()
            }
            is NetworkResponse.Failure -> {
                headersPanel.showHeaders(emptyMap())
                bodyPanel.showBody("Error: ${response.issue}", null, null)
                showContent()
            }
            null -> showEmpty()
        }
    }

    private fun showEmpty() {
        removeAll()
        add(emptyLabel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun showContent() {
        removeAll()
        add(tabs.component, BorderLayout.CENTER)
        revalidate()
        repaint()
    }
}
