package io.github.setheclark.intellij.ui.network.editor

import com.intellij.openapi.fileTypes.FileType
import javax.swing.Icon

object NetworkCallFileType : FileType {
    override fun getName() = "NetworkCall"
    override fun getDescription() = "Network Call Detail"
    override fun getDefaultExtension() = ""
    override fun getIcon(): Icon? = null
    override fun isBinary() = true
    override fun isReadOnly() = true
}
