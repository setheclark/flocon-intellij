package io.github.setheclark.intellij.ui.network.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.github.setheclark.intellij.di.appGraph

class NetworkCallFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile) = file is NetworkCallVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        file as NetworkCallVirtualFile
        val factory = appGraph.create(project).detailPanelFactory
        val (panel, scope) = factory.create(file.callId)
        return NetworkCallFileEditor(file, panel, scope)
    }

    override fun getEditorTypeId() = "network-call-editor"
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
