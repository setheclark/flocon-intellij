package io.github.setheclark.intellij.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.cancellation.CancellationException

object IntellijUtil {

    suspend fun formatText(
        project: Project,
        text: String,
        fileType: FileType,
    ): String = coroutineScope {
        val normalizedText = text.replace("\r\n", "\n").replace("\r", "\n")
        try {
            val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText(
                    "temp.${fileType.defaultExtension}",
                    fileType,
                    normalizedText,
                )
            WriteCommandAction.writeCommandAction(project).compute<String, Exception> {
                CodeStyleManager.getInstance(project).reformat(psiFile)
                psiFile.text
            }
        } catch (_: Throwable) {
            normalizedText
        }
    }
}

/**
 * Largest non-JSON body (in chars) run through the IDE formatter. The reformat needs a write
 * action on the UI thread, so bigger bodies are shown as received instead of freezing the IDE.
 */
internal const val MAX_IDE_FORMAT_CHARS = 200_000

private val prettyJson = Json { prettyPrint = true }

/** Pretty-prints [text] as JSON off the UI thread; returns null if it is not valid JSON. */
internal suspend fun prettyPrintJson(text: String): String? = withContext(Dispatchers.Default) {
    try {
        prettyJson.encodeToString(JsonElement.serializer(), prettyJson.parseToJsonElement(text))
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

/**
 * Formats a body for display without blocking the UI thread for JSON. Invalid JSON and bodies over
 * [MAX_IDE_FORMAT_CHARS] fall back to the unformatted text.
 */
suspend fun IntellijUtil.formatBody(project: Project, text: String, fileType: FileType): String {
    if (fileType.defaultExtension == "json") {
        prettyPrintJson(text)?.let { return it }
    }
    if (text.length > MAX_IDE_FORMAT_CHARS) return text
    return formatText(project, text, fileType)
}
