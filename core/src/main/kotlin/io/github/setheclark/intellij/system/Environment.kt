package io.github.setheclark.intellij.system

interface Environment {
    fun getEnvironmentVariable(key: String): String?
}
