package io.github.setheclark.intellij.ui.detail

import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel

/**
 * Panel for displaying request and response headers.
 */
class HeadersPanel : JPanel(BorderLayout()) {

    private val requestHeadersArea = createTextArea()
    private val responseHeadersArea = createTextArea()

    init {
        val splitPane = JBSplitter(true, 0.5f).apply {
            firstComponent = createSection("Request Headers", requestHeadersArea)
            secondComponent = createSection("Response Headers", responseHeadersArea)
        }
        add(splitPane, BorderLayout.CENTER)
    }

    fun showHeaders(call: NetworkCallEntry) {
        requestHeadersArea.text = formatHeaders(call.request.headers)
        responseHeadersArea.text = call.response?.headers?.let { formatHeaders(it) } ?: "(No response)"
    }

    private fun formatHeaders(headers: Map<String, String>): String {
        return headers.entries
            .sortedBy { it.key.lowercase() }
            .joinToString("\n") { "${it.key}: ${it.value}" }
    }

    private fun createSection(title: String, textArea: JBTextArea): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JBLabel(title).apply {
                border = JBUI.Borders.empty(4, 8)
                font = font.deriveFont(Font.BOLD)
            }, BorderLayout.NORTH)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
        }
    }

    private fun createTextArea(): JBTextArea {
        return JBTextArea().apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            border = JBUI.Borders.empty(4)
        }
    }
}

