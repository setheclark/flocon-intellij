package io.github.setheclark.intellij.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.coroutineScope

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
