package io.github.setheclark.intellij.ui.network.detail.response

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.ui.network.detail.common.BodyContentPanel
import io.github.setheclark.intellij.ui.network.detail.common.HeadersTablePanel
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Panel containing nested tabs for response headers and body.
 * Shows empty state when response is null.
 */
@Inject
class ResponsePanel(
    private val bodyPanel: BodyContentPanel,
) : JPanel(BorderLayout()) {

    private val tabbedPane = JBTabbedPane()
    private val headersPanel = HeadersTablePanel()

    private val emptyLabel = JBLabel("No response").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    init {
        tabbedPane.apply {
            addTab("Headers", headersPanel)
            addTab("Body", bodyPanel)
        }
        showEmpty()
    }

    fun showResponse(response: NetworkResponse?) {
        when (response) {
            is NetworkResponse.Success -> {
                headersPanel.showHeaders(response.headers)
                bodyPanel.showBody(response.body, response.contentType)
                showContent()
            }
            is NetworkResponse.Failure -> {
                headersPanel.showHeaders(emptyMap())
                bodyPanel.showBody("Error: ${response.issue}", null)
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
        add(tabbedPane, BorderLayout.CENTER)
        revalidate()
        repaint()
    }
}
