package io.github.setheclark.intellij.ui.network.detail.request

import com.intellij.openapi.project.Project
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.PluginBundle
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
    private val project: Project,
    private val bodyPanel: BodyContentPanel,
) : JPanel(BorderLayout()) {

    private val tabs = JBTabsFactory.createTabs(project)
    private val headersPanel = HeadersTablePanel()

    init {
        tabs.addTab(TabInfo(headersPanel).setText(PluginBundle.message("tab.headers")))
        tabs.addTab(TabInfo(bodyPanel).setText(PluginBundle.message("tab.body")))
        add(tabs.component, BorderLayout.CENTER)
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
                contentType = contentType,
            )
        }

        bodyPanel.showBody(request.body, contentType, context)
    }
}
