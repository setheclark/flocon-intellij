package io.github.setheclark.intellij.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.LocalProject
import io.github.setheclark.intellij.util.IntellijUtil.formatText
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import kotlin.coroutines.cancellation.CancellationException

private val log = Logger.withPluginTag("EditorText")

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

private sealed interface EditorState {
    data object Loading : EditorState
    data class Ready(val editor: Editor) : EditorState
    data object Unavailable : EditorState
}

@Composable
fun EditorText(
    text: String,
    contentType: ContentType,
    modifier: Modifier = Modifier,
) {
    val project = LocalProject.current

    if (isLikelyBinary(text)) {
        EditorPlaceholder(
            text = stringResource("label.binaryContent", text.length),
            modifier = modifier,
        )
        return
    }

    val state = rememberEditor(
        project = project,
        text = text,
        contentType = contentType,
    )

    when (state) {
        EditorState.Loading -> Box(modifier = modifier)

        is EditorState.Ready -> {
            SwingPanel(
                modifier = modifier,
                factory = {
                    UiDataProvider.wrapComponent(state.editor.component) { sink ->
                        sink[CommonDataKeys.EDITOR] = state.editor
                        sink[CommonDataKeys.PROJECT] = project
                    }
                },
            )
        }

        EditorState.Unavailable -> EditorPlaceholder(
            text = stringResource("label.editorUnavailable"),
            modifier = modifier,
        )
    }
}

@Composable
private fun EditorPlaceholder(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = JewelTheme.globalColors.text.info)
    }
}

@Composable
private fun rememberEditor(
    project: Project,
    text: String,
    contentType: ContentType,
): EditorState {
    var state by remember { mutableStateOf<EditorState>(EditorState.Loading) }
    val compositionScope = rememberCoroutineScope()

    DisposableEffect(project, contentType) {
        val disposable = Disposer.newDisposable("editor")

        val creationJob = compositionScope.launch(Dispatchers.EDT) {
            try {
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

                state = EditorState.Ready(createdEditor)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                log.w("Failed to create editor for network call body", e)
            }
        }

        onDispose {
            creationJob.cancel()
            val toRelease = (state as? EditorState.Ready)?.editor
            state = EditorState.Loading
            Disposer.dispose(disposable)
            toRelease?.let(EditorFactory.getInstance()::releaseEditor)
        }
    }

    // Update document content when text changes, without recreating the editor
    LaunchedEffect(text) {
        val currentEditor = (state as? EditorState.Ready)?.editor ?: return@LaunchedEffect
        withContext(Dispatchers.EDT) {
            try {
                val fileType = FileTypeManager.getInstance().getFileTypeByExtension(contentType.extension)
                val formattedText = formatText(project, text, fileType)
                WriteCommandAction.writeCommandAction(project).run<Throwable> {
                    currentEditor.document.setText(formattedText)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                log.w("Failed to update editor content", e)
            }
        }
    }

    return state
}

private fun isLikelyBinary(text: String): Boolean {
    if (text.isEmpty()) return false
    val sampleSize = minOf(text.length, 8192)
    var controlCount = 0
    for (i in 0 until sampleSize) {
        val c = text[i]
        if (c == '\u0000') return true
        if (c.code < 0x20 && c != '\n' && c != '\r' && c != '\t') {
            controlCount++
        }
    }
    return controlCount * 100 > sampleSize
}
