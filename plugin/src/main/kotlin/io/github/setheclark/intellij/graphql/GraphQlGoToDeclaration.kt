package io.github.setheclark.intellij.graphql

import com.intellij.find.FindManager
import com.intellij.find.findInProject.FindInProjectManager
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.util.concurrency.AppExecutorUtil
import io.github.setheclark.intellij.PluginBundle
import java.util.concurrent.Callable

/** Lets the user pick one of several matching declarations. */
fun interface GraphQlDeclarationChooser {
    fun choose(project: Project, matches: List<GraphQlDeclaration>, onChosen: (GraphQlDeclaration) -> Unit)
}

object GraphQlPopupChooser : GraphQlDeclarationChooser {
    override fun choose(project: Project, matches: List<GraphQlDeclaration>, onChosen: (GraphQlDeclaration) -> Unit) {
        val baseDir = project.guessProjectDir()
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(matches)
            .setTitle(PluginBundle.message("graphql.goTo.chooser.title"))
            .setRenderer(
                textListCellRenderer<GraphQlDeclaration> { declaration ->
                    val path = baseDir?.let { VfsUtilCore.getRelativePath(declaration.file, it) } ?: declaration.file.path
                    "${declaration.keyword} — $path:${declaration.line + 1}"
                },
            )
            .setItemChosenCallback(onChosen)
            .createPopup()
            .showCenteredInCurrentWindow(project)
    }
}

/**
 * Jumps from a GraphQL call to its operation in the project. Waits for indexing to finish, then opens the
 * single match, asks which of several to open, or notifies when there is none.
 */
class GraphQlGoToDeclaration(
    private val project: Project,
    private val chooser: GraphQlDeclarationChooser = GraphQlPopupChooser,
) {

    /** [onResolved] receives the keyword of the declaration that was opened. */
    fun go(ref: GraphQlOperationRef, onResolved: (keyword: String) -> Unit = {}) {
        val name = ref.operationName
        if (name.isNullOrBlank()) {
            notify(ref.unavailableReason.orEmpty(), NotificationType.INFORMATION)
            return
        }

        ReadAction.nonBlocking(Callable { GraphQlDeclarationFinder.find(project, ref) })
            .inSmartMode(project)
            .expireWith(project)
            .finishOnUiThread(ModalityState.defaultModalityState()) { matches ->
                when (matches.size) {
                    0 -> notifyNoMatch(name)
                    1 -> open(matches.single(), onResolved)
                    else -> chooser.choose(project, matches) { open(it, onResolved) }
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun open(declaration: GraphQlDeclaration, onResolved: (String) -> Unit) {
        OpenFileDescriptor(project, declaration.file, declaration.offset).navigate(true)
        onResolved(declaration.keyword)
    }

    private fun notifyNoMatch(name: String) {
        notify(PluginBundle.message("graphql.goTo.notFound", name), NotificationType.INFORMATION) { notification ->
            notification.addAction(
                NotificationAction.createSimpleExpiring(PluginBundle.message("graphql.goTo.findInFiles")) {
                    findInFiles(name)
                },
            )
        }
    }

    private fun findInFiles(text: String) {
        val model = FindManager.getInstance(project).findInProjectModel.clone()
        model.stringToFind = text
        model.isWholeWordsOnly = true
        FindInProjectManager.getInstance(project).findInProject(SimpleDataContext.getProjectContext(project), model)
    }

    private fun notify(
        content: String,
        type: NotificationType,
        configure: (com.intellij.notification.Notification) -> Unit = {},
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(content, type)
            .also(configure)
            .notify(project)
    }

    companion object {
        const val NOTIFICATION_GROUP = "Network Inspector"
    }
}
