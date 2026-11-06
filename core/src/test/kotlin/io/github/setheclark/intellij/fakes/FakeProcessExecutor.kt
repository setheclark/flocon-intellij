package io.github.setheclark.intellij.fakes

import io.github.openflocon.domain.common.Success
import io.github.setheclark.intellij.system.process.ProcessExecutor
import io.github.setheclark.intellij.system.process.ProcessResult
import io.github.setheclark.intellij.system.process.splitCommandLine

/**
 * Records every executed command as its argument list, as the real executor would see it.
 */
class FakeProcessExecutor(
    private val response: (List<String>) -> ProcessResult = { Success("") },
) : ProcessExecutor {

    val executed = mutableListOf<List<String>>()

    override fun execute(command: String): ProcessResult = run(splitCommandLine(command))

    override fun execute(vararg command: String): ProcessResult = run(command.toList())

    private fun run(args: List<String>): ProcessResult {
        executed += args
        return response(args)
    }
}
