package io.github.setheclark.intellij.process

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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