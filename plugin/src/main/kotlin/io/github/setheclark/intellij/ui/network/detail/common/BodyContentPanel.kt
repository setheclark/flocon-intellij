package io.github.setheclark.intellij.ui.network.detail.common

import co.touchlab.kermit.Logger
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JButton
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Panel for displaying request or response body with syntax highlighting and code folding.
 * Uses IntelliJ's EditorTextField for proper IDE-style rendering.
 * Shows "Not available" when body is null/empty.
 * Includes a floating button to open content in a scratch file.
 */
@Inject
class BodyContentPanel(
    private val project: Project,
) : JPanel(BorderLayout()) {

    private val log = Logger.withPluginTag("BodyContentPanel")

    private val json = Json { prettyPrint = true }
    private val emptyLabel = JBLabel("Not available").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    private var currentEditorTextField: EditorTextField? = null
    private var currentContext: ScratchFileContext? = null

    private val contentPanel = JPanel(BorderLayout())

    private val scratchButton = JButton(AllIcons.Actions.MenuSaveall).apply {
        toolTipText = "Open in Scratch File"
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        isOpaque = false
        isVisible = false
        addActionListener { openInScratchFile() }
    }

    // Overlay container using JLayeredPane with manual bounds management
    private val overlayPane = JLayeredPane().apply {
        add(contentPanel)
        setLayer(contentPanel, JLayeredPane.DEFAULT_LAYER.toInt())
        add(scratchButton)
        setLayer(scratchButton, JLayeredPane.PALETTE_LAYER.toInt())

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                updateLayeredPaneBounds()
            }

            override fun componentShown(e: ComponentEvent) {
                updateLayeredPaneBounds()
            }
        })
    }

    private fun updateLayeredPaneBounds() {
        // Content panel fills the entire area
        contentPanel.setBounds(0, 0, overlayPane.width, overlayPane.height)

        // Position button in top-right corner
        val buttonSize = scratchButton.preferredSize
        val padding = JBUI.scale(8)
        scratchButton.setBounds(
            overlayPane.width - buttonSize.width - padding,
            padding,
            buttonSize.width,
            buttonSize.height,
        )
    }

    init {
        add(overlayPane, BorderLayout.CENTER)
        showEmpty()
    }

    private fun showEmpty() {
        currentEditorTextField = null
        currentContext = null
        scratchButton.isVisible = false

        contentPanel.removeAll()
        contentPanel.add(emptyLabel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    fun showBody(body: String?, contentType: String?, context: ScratchFileContext? = null) {
        currentContext = context

        if (body.isNullOrEmpty()) {
            showEmpty()
            return
        }

        val fileType = getFileType(contentType)

        val formattedBody = if (contentType?.contains("json", ignoreCase = true) == true) {
            formatJson(body)
        } else {
            body
        }

        try {
            val language = if (fileType is LanguageFileType) fileType.language else Language.ANY

            val editorTextField = object : LanguageTextField(language, project, formattedBody) {
                override fun createEditor(): EditorEx {
                    val editor = super.createEditor()
                    editor.colorsScheme = EditorColorsManager.getInstance().globalScheme
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
                    editor.isViewer = true
                    editor.caretModel.moveToOffset(0)
                    editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                    return editor
                }
            }.apply {
                @Suppress("UsePropertyAccessSyntax")
                setOneLineMode(false)
            }

            currentEditorTextField = editorTextField
            scratchButton.isVisible = context != null

            contentPanel.removeAll()
            contentPanel.add(editorTextField, BorderLayout.CENTER)
            contentPanel.revalidate()
            contentPanel.repaint()

            // Update bounds after content change
            SwingUtilities.invokeLater { updateLayeredPaneBounds() }
        } catch (e: Exception) {
            log.w(e) { "Failed to create editor, falling back to text area" }
            showFallbackText(formattedBody)
        }
    }

    private fun showFallbackText(text: String) {
        currentEditorTextField = null
        scratchButton.isVisible = currentContext != null

        contentPanel.removeAll()
        val textArea = JBTextArea(text).apply {
            isEditable = false
            font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
            border = JBUI.Borders.empty(4)
        }
        contentPanel.add(JBScrollPane(textArea), BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun openInScratchFile() {
        val context = currentContext ?: return
        if (context.body.isEmpty()) return

        val filename = generateFilename(context)
        val extension = getExtension(context.contentType)
        val language = getLanguage(context.contentType)

        // Pre-format JSON content for readability
        val formattedBody = if (context.contentType?.contains("json", ignoreCase = true) == true) {
            formatJson(context.body)
        } else {
            context.body
        }

        try {
            val scratchFile = WriteCommandAction.writeCommandAction(project)
                .compute<com.intellij.openapi.vfs.VirtualFile?, Exception> {
                    ScratchRootType.getInstance().createScratchFile(
                        project,
                        "$filename.$extension",
                        language,
                        formattedBody,
                        ScratchFileService.Option.create_new_always,
                    )
                }

            scratchFile?.let { virtualFile ->
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to create scratch file" }
        }
    }

    private fun generateFilename(context: ScratchFileContext): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val timestamp = Instant.ofEpochMilli(context.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)

        val sanitizedName = context.queryName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(50)

        return buildString {
            append(sanitizedName)
            append("_")
            append(context.bodyType.name.lowercase())
            if (context.bodyType == ScratchFileContext.BodyType.RESPONSE && context.statusCode != null) {
                append("_")
                append(context.statusCode)
            }
            append("_")
            append(timestamp)
        }
    }

    private fun getExtension(contentType: String?): String {
        return when {
            contentType == null -> "txt"
            contentType.contains("json", ignoreCase = true) -> "json"
            contentType.contains("xml", ignoreCase = true) -> "xml"
            contentType.contains("html", ignoreCase = true) -> "html"
            contentType.contains("javascript", ignoreCase = true) -> "js"
            contentType.contains("css", ignoreCase = true) -> "css"
            else -> "txt"
        }
    }

    private fun getLanguage(contentType: String?): Language {
        val fileType = getFileType(contentType)
        return fileType.let {
            Language.findLanguageByID(it.name) ?: Language.findLanguageByID("TEXT") ?: Language.ANY
        }
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
        } catch (_: Exception) {
            jsonString
        }
    }
}
