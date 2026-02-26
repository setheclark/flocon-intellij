package io.github.setheclark.intellij

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "strings.Plugin"

object PluginBundle : DynamicBundle(BUNDLE) {
    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
