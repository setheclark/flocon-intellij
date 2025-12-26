//package io.github.setheclark.intellij.ui.actions
//
//import com.intellij.icons.AllIcons
//import com.intellij.openapi.actionSystem.AnAction
//import com.intellij.openapi.actionSystem.AnActionEvent
//import com.intellij.openapi.components.service
//import io.github.setheclark.intellij.services.ApplicationService
//import io.github.setheclark.intellij.services.ProjectService
//
///**
// * Action to clear all captured network traffic.
// */
//class ClearAllAction : AnAction(
//    "Clear All",
//    "Clear all captured network traffic",
//    AllIcons.Actions.GC
//) {
//    override fun actionPerformed(e: AnActionEvent) {
//        val project = e.project ?: return
//        val floconService = project.service<ApplicationService>()
//        floconService.appGraph.
//    }
//
//    override fun update(e: AnActionEvent) {
//        e.presentation.isEnabled = e.project != null
//    }
//}
