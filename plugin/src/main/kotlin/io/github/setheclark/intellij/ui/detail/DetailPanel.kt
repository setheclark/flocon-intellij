package io.github.setheclark.intellij.ui.detail

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import io.github.setheclark.intellij.services.FloconProjectService
import io.github.setheclark.intellij.services.NetworkCallEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.awt.BorderLayout
import java.awt.Font
import java.time.Instant
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Panel displaying detailed information about a selected network call.
 * Contains tabs for Headers, Request Body, Response Body, and Timing.
 */
class DetailPanel(
    private val project: Project
) : JPanel(BorderLayout()), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val floconService = project.service<FloconProjectService>()

    private val tabbedPane = JBTabbedPane()
    private val headersPanel = HeadersPanel()
    private val requestBodyPanel = BodyPanel(project, this)
    private val responseBodyPanel = BodyPanel(project, this)
    private val timingPanel = TimingPanel()
    private val emptyLabel = JBLabel("Select a request to view details").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    init {
        tabbedPane.apply {
            addTab("Headers", headersPanel)
            addTab("Request", requestBodyPanel)
            addTab("Response", responseBodyPanel)
            addTab("Timing", timingPanel)
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
            floconService.selectedCall.collectLatest { call ->
                SwingUtilities.invokeLater {
                    if (call != null) {
                        updateDetails(call)
                        showDetails()
                    } else {
                        showEmpty()
                    }
                }
            }
        }
    }

    private fun updateDetails(call: NetworkCallEntry) {
        headersPanel.showHeaders(call)
        requestBodyPanel.showBody(call.request.body, call.request.contentType)
        responseBodyPanel.showBody(call.response?.body, call.response?.contentType)
        timingPanel.showTiming(call)
    }

    override fun dispose() {
        scope.cancel()
    }
}

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

/**
 * Panel for displaying request or response body with syntax highlighting and code folding.
 * Uses IntelliJ's EditorTextField for proper IDE-style rendering.
 */
class BodyPanel(
    private val project: Project,
    private val parentDisposable: Disposable
) : JPanel(BorderLayout()) {

    companion object {
        private val LOG = logger<BodyPanel>()
    }

    private val json = Json { prettyPrint = true }
    private val emptyLabel = JBLabel("(Empty body)").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    private var currentEditorTextField: EditorTextField? = null

    init {
        showEmpty()
    }

    private fun showEmpty() {
        currentEditorTextField = null
        removeAll()
        add(emptyLabel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    fun showBody(body: String?, contentType: String?) {
        if (body.isNullOrEmpty()) {
            showEmpty()
            return
        }

        // Determine file type based on content type
        val fileType = getFileType(contentType)

        // Format content if JSON
        val formattedBody = if (contentType?.contains("json", ignoreCase = true) == true) {
            formatJson(body)
        } else {
            body
        }

        // Create EditorTextField with syntax highlighting
        try {
            val editorTextField = object : EditorTextField(formattedBody, project, fileType) {
                override fun createEditor(): EditorEx {
                    val editor = super.createEditor()
                    editor.settings.apply {
                        isLineNumbersShown = true
                        isWhitespacesShown = false
                        isFoldingOutlineShown = true
                        isAutoCodeFoldingEnabled = true
                        additionalLinesCount = 0
                        additionalColumnsCount = 0
                        isRightMarginShown = false
                        isCaretRowShown = false
                        isUseSoftWraps = true
                    }
                    editor.setVerticalScrollbarVisible(true)
                    editor.setHorizontalScrollbarVisible(true)
                    return editor
                }
            }.apply {
                @Suppress("UsePropertyAccessSyntax")
                setOneLineMode(false)
                isViewer = true
            }

            currentEditorTextField = editorTextField

            removeAll()
            add(editorTextField, BorderLayout.CENTER)
            revalidate()
            repaint()
        } catch (e: Exception) {
            LOG.warn("Failed to create editor, falling back to text area", e)
            showFallbackText(formattedBody)
        }
    }

    private fun showFallbackText(text: String) {
        currentEditorTextField = null
        removeAll()
        val textArea = JBTextArea(text).apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            border = JBUI.Borders.empty(4)
        }
        add(JBScrollPane(textArea), BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun getFileType(contentType: String?): FileType {
        val fileTypeManager = FileTypeManager.getInstance()
        return when {
            contentType == null -> PlainTextFileType.INSTANCE
            contentType.contains("json", ignoreCase = true) ->
                fileTypeManager.getFileTypeByExtension("json")
            contentType.contains("xml", ignoreCase = true) ->
                fileTypeManager.getFileTypeByExtension("xml")
            contentType.contains("html", ignoreCase = true) ->
                fileTypeManager.getFileTypeByExtension("html")
            contentType.contains("javascript", ignoreCase = true) ->
                fileTypeManager.getFileTypeByExtension("js")
            contentType.contains("css", ignoreCase = true) ->
                fileTypeManager.getFileTypeByExtension("css")
            else -> PlainTextFileType.INSTANCE
        }
    }

    private fun formatJson(jsonString: String): String {
        return try {
            val element = json.parseToJsonElement(jsonString)
            json.encodeToString(JsonElement.serializer(), element)
        } catch (e: Exception) {
            jsonString // Return original if parsing fails
        }
    }
}

/**
 * Panel for displaying timing information about a network call.
 */
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
