package io.github.setheclark.intellij.ui.network.detail.request

import com.intellij.ui.components.JBTabbedPane
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.ui.network.detail.common.BodyContentPanel
import io.github.setheclark.intellij.ui.network.detail.common.HeadersTablePanel
import io.github.setheclark.intellij.ui.network.detail.common.ScratchFileContext
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Panel containing nested tabs for request headers and body.
 */
@Inject
class RequestPanel(
    private val bodyPanel: BodyContentPanel,
) : JPanel(BorderLayout()) {

    private val tabbedPane = JBTabbedPane()
    private val headersPanel = HeadersTablePanel()

    init {
        tabbedPane.apply {
            addTab("Headers", headersPanel)
            addTab("Body", bodyPanel)
        }
        add(tabbedPane, BorderLayout.CENTER)
    }

    fun showRequest(request: NetworkRequest, callName: String, timestamp: Long) {
        headersPanel.showHeaders(request.headers)

        val contentType = request.headers.entries
            .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
            ?.value

        val context = request.body?.let {
            ScratchFileContext(
                queryName = callName,
                bodyType = ScratchFileContext.BodyType.REQUEST,
                statusCode = null,
                timestamp = timestamp,
                body = it,
                contentType = contentType
            )
        }

        bodyPanel.showBody(request.body, contentType, context)
    }
}
