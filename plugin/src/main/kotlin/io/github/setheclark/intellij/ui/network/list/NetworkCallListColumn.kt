package io.github.setheclark.intellij.ui.network.list

import io.github.setheclark.intellij.PluginBundle

enum class NetworkCallListColumn(
    val preferredWidth: Int,
    val minWidth: Int,
) {
    TIME(preferredWidth = 75, minWidth = 55),
    NAME(preferredWidth = 150, minWidth = 40),
    STATUS(preferredWidth = 50, minWidth = 30),
    METHOD(preferredWidth = 50, minWidth = 30),
    URL(preferredWidth = 400, minWidth = 50),
    DURATION(preferredWidth = 50, minWidth = 30),
    SIZE(preferredWidth = 50, minWidth = 30),
    ;

    val displayName: String get() = PluginBundle.message("column.${name.lowercase()}")
}
