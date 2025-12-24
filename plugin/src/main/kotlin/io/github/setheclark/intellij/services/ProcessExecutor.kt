package io.github.setheclark.intellij.services

import java.io.IOException

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

    /**
     * Check if a command exists and is executable.
     * @param command The command to check
     * @return true if the command can be executed
     */
    fun isCommandAvailable(command: String): Boolean
}

/**
 * Real implementation of ProcessExecutor that executes system processes.
 */
class SystemProcessExecutor : ProcessExecutor {

    override fun execute(command: String): ProcessResult {
        return try {
            @Suppress("DEPRECATION")
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            ProcessResult(exitCode, output, errorOutput)
        } catch (e: IOException) {
            ProcessResult(-1, "", e.message ?: "IOException occurred")
        } catch (e: InterruptedException) {
            ProcessResult(-1, "", e.message ?: "Process interrupted")
        }
    }

    override fun execute(vararg command: String): ProcessResult {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(false)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            ProcessResult(exitCode, output, errorOutput)
        } catch (e: IOException) {
            ProcessResult(-1, "", e.message ?: "IOException occurred")
        } catch (e: InterruptedException) {
            ProcessResult(-1, "", e.message ?: "Process interrupted")
        }
    }

    override fun isCommandAvailable(command: String): Boolean {
        return try {
            val process = ProcessBuilder(command, "version")
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: IOException) {
            false
        }
    }
}
