package io.github.setheclark.intellij.graphql

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import io.github.setheclark.intellij.PluginBundle

/** The parts of a GraphQL call needed to find its declaration in the project. */
data class GraphQlOperationRef(
    val operationName: String?,
    val operationType: String?,
) {
    /** Why the declaration cannot be looked up, or null when it can. */
    val unavailableReason: String?
        get() = if (operationName.isNullOrBlank()) PluginBundle.message("graphql.goTo.unavailable.anonymous") else null
}

/** An operation definition found in a project `.graphql` file. [keyword] is `query`, `mutation` or `subscription`. */
data class GraphQlDeclaration(
    val file: VirtualFile,
    val offset: Int,
    val line: Int,
    val keyword: String,
)

/**
 * Finds `query|mutation|subscription <name>` definitions by scanning project files with GraphQL extensions.
 * Uses only core platform APIs so it works without the GraphQL plugin.
 */
object GraphQlDeclarationFinder {

    val EXTENSIONS = listOf("graphql", "gql", "graphqls")

    private const val MAX_FILE_LENGTH = 5_000_000

    /** Requires smart mode and a read action. Declarations whose keyword equals the call's operation type come first. */
    fun find(project: Project, ref: GraphQlOperationRef): List<GraphQlDeclaration> {
        val name = ref.operationName?.takeIf { it.isNotBlank() } ?: return emptyList()
        val pattern = Regex("""(?<![\w"])(query|mutation|subscription)[\s,]+${Regex.escape(name)}(?![\w])""")
        val scope = GlobalSearchScope.projectScope(project)
        val files = EXTENSIONS.flatMap { FilenameIndex.getAllFilesByExt(project, it, scope) }.distinct()

        return files
            .filter { !it.isDirectory }
            .flatMap { file -> declarationsIn(file, pattern) }
            .sortedWith(
                compareBy<GraphQlDeclaration> { !it.keyword.equals(ref.operationType, ignoreCase = true) }
                    .thenBy { it.file.path }
                    .thenBy { it.offset },
            )
    }

    private fun declarationsIn(file: VirtualFile, pattern: Regex): List<GraphQlDeclaration> {
        if (file.length > MAX_FILE_LENGTH) return emptyList()
        val text: CharSequence = FileDocumentManager.getInstance().getDocument(file)?.immutableCharSequence
            ?: String(file.contentsToByteArray(), file.charset)

        return pattern.findAll(text).mapNotNull { match ->
            val offset = match.range.first
            val lineStart = text.lastIndexOf('\n', offset - 1) + 1
            if (text.subSequence(lineStart, offset).contains('#')) return@mapNotNull null
            val line = (0 until offset).count { text[it] == '\n' }
            GraphQlDeclaration(file, offset, line, match.groupValues[1])
        }.toList()
    }
}
