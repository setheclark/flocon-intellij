package io.github.setheclark.intellij.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import io.github.setheclark.intellij.ui.LocalProject
import io.github.setheclark.intellij.util.IntellijUtil.formatText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            factory = {
                UiDataProvider.wrapComponent(it.component) { sink ->
                    sink[CommonDataKeys.EDITOR] = it
                    sink[CommonDataKeys.PROJECT] = project
                }
            },
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
    val compositionScope = rememberCoroutineScope()

    DisposableEffect(project, contentType) {
        val disposable = Disposer.newDisposable("editor")

        val creationJob = compositionScope.launch(Dispatchers.EDT) {
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
            creationJob.cancel()
            val editorToRelease = editor
            editor = null
            Disposer.dispose(disposable)
            editorToRelease?.let(EditorFactory.getInstance()::releaseEditor)
        }
    }

    // Update document content when text changes, without recreating the editor
    LaunchedEffect(text) {
        val currentEditor = editor ?: return@LaunchedEffect
        withContext(Dispatchers.EDT) {
            val fileType = FileTypeManager.getInstance().getFileTypeByExtension(contentType.extension)
            val formattedText = formatText(project, text, fileType)
            WriteCommandAction.writeCommandAction(project).run<Throwable> {
                currentEditor.document.setText(formattedText)
            }
        }
    }

    return editor
}
