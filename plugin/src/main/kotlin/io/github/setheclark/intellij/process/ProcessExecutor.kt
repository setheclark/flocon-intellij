package io.github.setheclark.intellij.process

/**
 * Interface for executing system processes.
 * This abstraction allows for testing without actually executing system commands.
 */
interface ProcessExecutor {
    /**
     * Execute a command and wait for it to complete.
     * @param command The command to execute (single string, will be parsed by the shell)
     * @return The result of the process execution
     */
    fun execute(command: String): ProcessResult

    /**
     * Execute a command with arguments and wait for it to complete.
     * @param command The command parts (first element is the executable, rest are arguments)
     * @return The result of the process execution
     */
    fun execute(vararg command: String): ProcessResult
}
