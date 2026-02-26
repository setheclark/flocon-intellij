package io.github.setheclark.intellij.ui.network.editor

import com.intellij.testFramework.LightVirtualFile

class NetworkCallVirtualFile(
    val callId: String,
    callName: String,
) : LightVirtualFile(callName, NetworkCallFileType, "") {
    init {
        isWritable = false
    }
}
