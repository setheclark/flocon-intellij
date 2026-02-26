package io.github.setheclark.intellij.ui.network.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import io.github.setheclark.intellij.ui.network.detail.DetailPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent

class NetworkCallFileEditor(
    private val file: NetworkCallVirtualFile,
    private val panel: DetailPanel,
    private val scope: CoroutineScope,
) : FileEditor {
    private val userData = UserDataHolderBase()
    private val pcs = PropertyChangeSupport(this)

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = panel
    override fun getName() = "Network Call"
    override fun getFile() = file
    override fun isModified() = false
    override fun isValid() = true
    override fun setState(state: FileEditorState) {}
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
    override fun addPropertyChangeListener(l: PropertyChangeListener) {
        pcs.addPropertyChangeListener(l)
    }
    override fun removePropertyChangeListener(l: PropertyChangeListener) {
        pcs.removePropertyChangeListener(l)
    }
    override fun dispose() {
        scope.cancel()
    }
    override fun <T : Any?> getUserData(key: Key<T>) = userData.getUserData(key)
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        userData.putUserData(key, value)
    }
}
