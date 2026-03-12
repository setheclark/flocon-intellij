package io.github.setheclark.intellij.ui.network.details.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.LocalProject
import io.github.setheclark.intellij.ui.component.ContentType
import io.github.setheclark.intellij.ui.component.EditorText
import io.github.setheclark.intellij.ui.component.TabbedContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HttpMessageContent(
    headers: Map<String, String>?,
    body: String?,
    contentType: String?,
    scratchContext: ScratchFileContext?,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val project = LocalProject.current

    val tabs = listOf(
        stringResource("tab.headers"),
        stringResource("tab.body"),
    )

    TabbedContent(
        tabs = tabs,
        selectedIndex = selectedTabIndex,
        onTabSelect = onTabSelect,
        modifier = modifier,
    ) { index ->
        when (index) {
            0 -> HeadersTable(headers, Modifier.fillMaxSize())

            1 -> Box(modifier = Modifier.fillMaxSize()) {
                if (body == null) {
                    EmptyContent(stringResource("label.bodyNotAvailable"), Modifier.fillMaxSize())
                } else {
                    EditorText(
                        text = body,
                        contentType = ContentType.fromMimeType(contentType),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (scratchContext != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .padding(end = 16.dp),
                    ) {
                        IconActionButton(
                            key = AllIconsKeys.Actions.MenuSaveall,
                            contentDescription = stringResource("tooltip.openInScratchFile"),
                            onClick = {
                                scope.launch(Dispatchers.EDT) {
                                    openInScratchFile(project, scratchContext)
                                }
                            },
                            focusable = false,
                            tooltip = { Text(stringResource("tooltip.openInScratchFile")) },
                        )
                    }
                }
            }
        }
    }
}

private fun openInScratchFile(project: Project, context: ScratchFileContext) {
    val contentType = ContentType.fromMimeType(context.contentType)
    val fileType = FileTypeManager.getInstance().getFileTypeByExtension(contentType.extension)
    val language = if (fileType is LanguageFileType) fileType.language else Language.ANY
    val filename = generateFilename(context)

    try {
        val scratchFile = WriteCommandAction.writeCommandAction(project)
            .compute<VirtualFile?, Exception> {
                ScratchRootType.getInstance().createScratchFile(
                    project,
                    "$filename.${contentType.extension}",
                    language,
                    context.body,
                    ScratchFileService.Option.create_new_always,
                )
            }
        scratchFile?.let { FileEditorManager.getInstance(project).openFile(it, true) }
    } catch (_: Exception) {
        // ignore
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
