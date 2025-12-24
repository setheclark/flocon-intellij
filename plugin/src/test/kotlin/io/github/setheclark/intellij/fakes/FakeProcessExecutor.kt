package io.github.setheclark.intellij.fakes

import io.github.setheclark.intellij.services.ProcessExecutor
import io.github.setheclark.intellij.services.ProcessResult

/**
 * Fake implementation of ProcessExecutor for testing.
 * Allows predefined responses for specific commands.
 */
class FakeProcessExecutor : ProcessExecutor {

    private val commandResponses = mutableMapOf<String, ProcessResult>()
    private val availableCommands = mutableSetOf<String>()
    val executedCommands = mutableListOf<List<String>>()

    /**
     * Set the result that should be returned for a specific command.
     * The command key is the first element of the command array.
     */
    fun setResponse(commandKey: String, result: ProcessResult) {
        commandResponses[commandKey] = result
    }

    /**
     * Mark a command as available for isCommandAvailable checks.
     */
    fun setCommandAvailable(command: String, available: Boolean = true) {
        if (available) {
            availableCommands.add(command)
        } else {
            availableCommands.remove(command)
        }
    }

    /**
     * Default result to return when no specific response is set.
     */
    var defaultResult = ProcessResult(0, "", "")

    override fun execute(command: String): ProcessResult {
        executedCommands.add(listOf(command))
        return commandResponses[command] ?: defaultResult
    }

    override fun execute(vararg command: String): ProcessResult {
        executedCommands.add(command.toList())
        val key = command.firstOrNull() ?: ""
        return commandResponses[key] ?: defaultResult
    }

    override fun isCommandAvailable(command: String): Boolean {
        return command in availableCommands
    }

    /**
     * Clear all recorded commands and responses.
     */
    fun reset() {
        commandResponses.clear()
        availableCommands.clear()
        executedCommands.clear()
        defaultResult = ProcessResult(0, "", "")
    }
}
