package io.github.setheclark.intellij.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import io.github.setheclark.intellij.ui.LocalProject
import io.github.setheclark.intellij.util.IntellijUtil.formatText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ContentType(val extension: String) {
    Json("json"),
    Xml("xml"),
    Html("html"),
    Js("js"),
    Css("css"),
    Yaml("yaml"),
    GraphQL("graphql"),
    Unknown("txt"),
    ;

    companion object {
        fun fromMimeType(contentType: String?): ContentType {
            if (contentType == null) return Unknown
            val lower = contentType.lowercase()
            return when {
                lower.contains("json") -> Json
                lower.contains("xml") -> Xml
                lower.contains("html") -> Html
                lower.contains("javascript") -> Js
                lower.contains("css") -> Css
                lower.contains("yaml") -> Yaml
                lower.contains("graphql") -> GraphQL
                lower.startsWith("text/") -> Unknown
                else -> Unknown
            }
        }
    }
}

@Composable
fun EditorText(
    text: String,
    contentType: ContentType,
    modifier: Modifier = Modifier,
) {
    val project = LocalProject.current

    val editor = rememberEditor(
        project = project,
        text = text,
        contentType = contentType,
    )

    editor?.let {
        SwingPanel(
            modifier = modifier,
            factory = { editor.component },
        )
    }
}

@Composable
private fun rememberEditor(
    project: Project,
    text: String,
    contentType: ContentType,
): Editor? {
    var editor by remember { mutableStateOf<Editor?>(null) }

    DisposableEffect(project, contentType) {
        val scope = CoroutineScope(Dispatchers.EDT)
        val disposable = Disposer.newDisposable("editor")

        scope.launch {
            val fileType = FileTypeManager.getInstance().getFileTypeByExtension(contentType.extension)
            val formattedText = formatText(project, text, fileType)
            val doc = EditorFactory.getInstance().createDocument(formattedText)
            val createdEditor = EditorFactory.getInstance()
                .createEditor(doc, project, fileType, true) as Editor

            createdEditor.settings.apply {
                isLineNumbersShown = true
                isAutoCodeFoldingEnabled = true
                setGutterIconsShown(false)
            }
            createdEditor.setBorder(null)

            editor = createdEditor
        }

        onDispose {
            scope.launch {
                Disposer.dispose(disposable)
                editor?.let(EditorFactory.getInstance()::releaseEditor)
                editor = null
            }
        }
    }

    return editor
}
