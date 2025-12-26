package io.github.setheclark.intellij.process

/**
 * Result of a process execution.
 */
data class ProcessResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String = ""
) {
    val isSuccess: Boolean get() = exitCode == 0
}