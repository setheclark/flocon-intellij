package io.github.setheclark.intellij.ui.network.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.LocalProject
import io.github.setheclark.intellij.ui.component.ContentType
import io.github.setheclark.intellij.ui.component.EditorText
import io.github.setheclark.intellij.ui.network.details.common.EmptyContent
import io.github.setheclark.intellij.ui.network.details.common.ScratchFileContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BodyPane(
    call: NetworkCallEntity,
    modifier: Modifier = Modifier,
) {
    val isGrpc = call.request.type is NetworkRequest.Type.Grpc

    val responseBody: String? = when (val r = call.response) {
        is NetworkResponse.Success -> r.body
        is NetworkResponse.Failure -> stringResource("label.networkError", r.issue)
        null -> null
    }
    val responseContentType = (call.response as? NetworkResponse.Success)?.contentType
    val requestBody = call.request.body
    val requestContentType = call.request.headers.entries
        .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value

    // Default to response; fall back to request if response has no body
    val defaultToResponse = responseBody != null || call.response == null
    var showResponse by remember(call.callId) { mutableStateOf(defaultToResponse) }

    val activeBody = if (showResponse) responseBody else requestBody
    val activeContentType = if (showResponse) responseContentType else requestContentType

    val responseSectionLabel = if (isGrpc) stringResource("section.responseMessage") else stringResource("section.responseBody")
    val requestSectionLabel = if (isGrpc) stringResource("section.requestMessage") else stringResource("section.requestBody")

    val scope = rememberCoroutineScope()
    val project = LocalProject.current

    val scratchContext = activeBody?.let {
        ScratchFileContext(
            queryName = call.name,
            bodyType = if (showResponse) ScratchFileContext.BodyType.RESPONSE else ScratchFileContext.BodyType.REQUEST,
            contentType = activeContentType,
            statusCode = (call.response as? NetworkResponse.Success)?.statusCode,
            timestamp = call.startTime,
            body = it,
        )
    }

    Column(modifier = modifier) {
        // Toggle header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BodyToggleTab(
                label = requestSectionLabel,
                selected = !showResponse,
                onClick = { showResponse = false },
            )
            Spacer(Modifier.width(16.dp))
            BodyToggleTab(
                label = responseSectionLabel,
                selected = showResponse,
                onClick = { showResponse = true },
            )
            Spacer(Modifier.weight(1f))
            if (scratchContext != null) {
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
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())

        Box(modifier = Modifier.fillMaxSize()) {
            // Both editors stay composed simultaneously so tab switching is instant.
            // The inactive editor is collapsed to 0×0 rather than removed from composition.
            if (requestBody != null) {
                EditorText(
                    text = requestBody,
                    contentType = ContentType.fromMimeType(requestContentType),
                    modifier = if (!showResponse) Modifier.fillMaxSize() else Modifier.size(0.dp),
                )
            } else if (!showResponse) {
                EmptyContent(stringResource("label.noBody"), Modifier.fillMaxSize())
            }

            if (responseBody != null) {
                EditorText(
                    text = responseBody,
                    contentType = ContentType.fromMimeType(responseContentType),
                    modifier = if (showResponse) Modifier.fillMaxSize() else Modifier.size(0.dp),
                )
            } else if (showResponse) {
                val emptyLabel = if (call.response == null) stringResource("label.pending") else stringResource("label.noBody")
                EmptyContent(emptyLabel, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun BodyToggleTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) JewelTheme.globalColors.text.normal else JewelTheme.globalColors.text.info,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )
}

private fun openInScratchFile(project: Project, context: ScratchFileContext) {
    val contentType = ContentType.fromMimeType(context.contentType)
    val fileType = FileTypeManager.getInstance().getFileTypeByExtension(contentType.extension)
    val language = if (fileType is LanguageFileType) fileType.language else Language.ANY
    val filename = generateScratchFilename(context)
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

private fun generateScratchFilename(context: ScratchFileContext): String {
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
