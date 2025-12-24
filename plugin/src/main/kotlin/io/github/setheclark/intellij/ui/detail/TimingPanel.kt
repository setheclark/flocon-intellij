package io.github.setheclark.intellij.ui.detail

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.services.NetworkCallEntry
import java.awt.BorderLayout
import java.awt.Font
import java.time.Instant
import javax.swing.JPanel

/**
 * Panel for displaying timing information about a network call.
 */
@Inject
class TimingPanel : JPanel(BorderLayout()) {

    private val infoArea = JBTextArea().apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        border = JBUI.Borders.empty(8)
    }

    init {
        add(JBLabel("Timing").apply {
            border = JBUI.Borders.empty(4, 8)
            font = font.deriveFont(Font.BOLD)
        }, BorderLayout.NORTH)
        add(JBScrollPane(infoArea), BorderLayout.CENTER)
    }

    fun showTiming(call: NetworkCallEntry) {
        val sb = StringBuilder()
        sb.appendLine("Request Information")
        sb.appendLine("==================")
        sb.appendLine()
        sb.appendLine("URL:        ${call.request.url}")
        sb.appendLine("Method:     ${call.request.method}")
        sb.appendLine("Start Time: ${Instant.ofEpochMilli(call.startTime)}")

        call.duration?.let {
            sb.appendLine("Duration:   ${it}ms")
        }

        call.response?.let { response ->
            sb.appendLine()
            sb.appendLine("Response")
            sb.appendLine("--------")
            sb.appendLine("Status:     ${response.statusCode} ${response.statusMessage ?: ""}")
            response.size?.let { sb.appendLine("Size:       ${formatSize(it)}") }
            response.error?.let { sb.appendLine("Error:      $it") }
        }

        sb.appendLine()
        sb.appendLine("Device Information")
        sb.appendLine("------------------")
        sb.appendLine("Device ID:  ${call.deviceId}")
        sb.appendLine("Package:    ${call.packageName}")

        infoArea.text = sb.toString()
        infoArea.caretPosition = 0
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "${size} bytes"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}
