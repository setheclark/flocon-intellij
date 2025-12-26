package io.github.setheclark.intellij.services

import io.github.setheclark.intellij.process.ProcessResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Basic tests for AdbService.
 * Note: More comprehensive tests would require mocking the IntelliJ Platform
 * service infrastructure or using integration tests.
 */
class AdbServiceTest {

    @Test
    fun `ProcessResult isSuccess returns true for exit code 0`() {
        val result = ProcessResult(0, "output", "")
        result.isSuccess shouldBe true
    }

    @Test
    fun `ProcessResult isSuccess returns false for non-zero exit code`() {
        val result = ProcessResult(1, "", "error")
        result.isSuccess shouldBe false
    }

    @Test
    fun `ProcessResult stores output and error correctly`() {
        val result = ProcessResult(0, "stdout content", "stderr content")
        result.output shouldBe "stdout content"
        result.errorOutput shouldBe "stderr content"
    }
}
