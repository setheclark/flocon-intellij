package io.github.setheclark.intellij.ui.network.editor

import com.intellij.openapi.fileTypes.FileType
import io.github.setheclark.intellij.PluginBundle
import javax.swing.Icon

object NetworkCallFileType : FileType {
    override fun getName() = "NetworkCall"
    override fun getDescription() = PluginBundle.message("filetype.networkCall.description")
    override fun getDefaultExtension() = ""
    override fun getIcon(): Icon? = null
    override fun isBinary() = true
    override fun isReadOnly() = true
}
