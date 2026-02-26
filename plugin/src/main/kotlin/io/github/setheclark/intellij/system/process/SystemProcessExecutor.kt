package io.github.setheclark.intellij.system.process

import co.touchlab.kermit.Logger
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.EnvironmentUtil
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.common.Failure
import io.github.openflocon.domain.common.Success
import io.github.setheclark.intellij.util.withPluginTag
import java.io.IOException

/**
 * Real implementation of ProcessExecutor that executes system processes.
 */
@Inject
@SingleIn(AppScope::class)
class SystemProcessExecutor : ProcessExecutor {

    private val log = Logger.withPluginTag("SystemProcessExecutor")

    override fun execute(command: String): ProcessResult {
        return try {
            val parts = command.split(Regex("\\s+"))
            val executable = parts.first()
            val args = parts.drop(1)

            val commandLine = createCommandLine(executable, args)

            log.d { "Executing: ${commandLine.commandLineString}" }

            executeProcess(commandLine, command)
        } catch (e: IOException) {
            log.e(e) { "Failed to execute command: $command" }
            e.asResult(command)
        } catch (e: InterruptedException) {
            log.e(e) { "Command execution interrupted: $command" }
            e.asResult(command)
        }
    }

    override fun execute(vararg command: String): ProcessResult {
        val commandString = command.joinToString(" ")
        return try {
            val commandLine = createCommandLine(command.first(), command.drop(1))

            log.d { "Executing: ${commandLine.commandLineString}" }

            executeProcess(commandLine, commandString)
        } catch (e: IOException) {
            log.e(e) { "Failed to execute command: $commandString" }
            e.asResult(commandString)
        } catch (e: InterruptedException) {
            log.e(e) { "Command execution interrupted: $commandString" }
            e.asResult(commandString)
        }
    }

    private fun createCommandLine(executable: String, args: List<String>): GeneralCommandLine {
        return GeneralCommandLine(executable)
            .withParameters(args)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
            .also { commandLine ->
                EnvironmentUtil.getEnvironmentMap().forEach { (key, value) ->
                    commandLine.environment[key] = value
                }
            }
    }

    private fun executeProcess(commandLine: GeneralCommandLine, originalCommand: String): ProcessResult {
        val process = commandLine.createProcess()
        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        return result(exitCode, output, errorOutput).also {
            if (it is Failure) {
                log.w { "Command failed (exit code: $exitCode): $originalCommand" }
                log.v { "Error output: $errorOutput" }
            }
        }
    }

    private fun result(exitCode: Int, output: String, errorOutput: String): ProcessResult {
        return if (exitCode == 0) {
            Success(output)
        } else {
            val errorMessage =
                "Command failed with exit code $exitCode. Error:\n$errorOutput"
            Failure(IOException(errorMessage))
        }
    }

    private fun IOException.asResult(command: String): ProcessResult {
        val errorMessage = "Error executing command '$command': $message"
        return Failure(IOException(errorMessage))
    }

    private fun InterruptedException.asResult(command: String): ProcessResult {
        val errorMessage = "Command execution interrupted for '$command': $message"
        return Failure(IOException(errorMessage))
    }
}
