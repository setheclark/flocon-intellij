package io.github.setheclark.intellij.ui.detail

import co.touchlab.kermit.Logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel

/**
 * Panel for displaying request or response body with syntax highlighting and code folding.
 * Uses IntelliJ's EditorTextField for proper IDE-style rendering.
 */
@Inject
class BodyPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {

    private val log = Logger.withTag("BodyPanel")

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
            log.w(e) { "Failed to create editor, falling back to text area" }
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