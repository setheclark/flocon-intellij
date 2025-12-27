package io.github.setheclark.intellij.process

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.common.Failure
import io.github.openflocon.domain.common.Success
import java.io.IOException

/**
 * Real implementation of ProcessExecutor that executes system processes.
 */
@Inject
@SingleIn(AppScope::class)
class SystemProcessExecutor : ProcessExecutor {

    override fun execute(command: String): ProcessResult {
        return try {
            @Suppress("DEPRECATION")
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            result(exitCode, output, errorOutput)
        } catch (e: IOException) {
            e.asResult(command)
        } catch (e: InterruptedException) {
            e.asResult(command)
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
            result(exitCode, output, errorOutput)
        } catch (e: IOException) {
            e.asResult(command.joinToString(" "))
        } catch (e: InterruptedException) {
            e.asResult(command.joinToString(" "))
        }
    }

//    override fun isCommandAvailable(command: String): Boolean {
//        return try {
//            val process = ProcessBuilder(command, "version")
//                .redirectErrorStream(true)
//                .start()
//            val exitCode = process.waitFor()
//            exitCode == 0
//        } catch (e: IOException) {
//            false
//        }
//    }

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