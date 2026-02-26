package io.github.setheclark.intellij.settings

import io.github.setheclark.intellij.PluginBundle

enum class CallDetailOpenMode(private val bundleKey: String) {
    TOOL_WINDOW_TAB("setting.callDetailOpenMode.toolWindowTab"),
    EDITOR_WINDOW("setting.callDetailOpenMode.editorWindow"),
    ;

    val displayName: String get() = PluginBundle.message(bundleKey)
}
