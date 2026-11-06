package io.github.setheclark.intellij.system.process

import io.kotest.matchers.shouldBe
import org.junit.Test

class CommandLineSplitterTest {

    @Test
    fun `quoted argument with spaces stays one argument with quotes preserved`() {
        splitCommandLine("""adb shell am start -d "my link with spaces"""") shouldBe
            listOf("adb", "shell", "am", "start", "-d", "\"my link with spaces\"")
    }

    @Test
    fun `quote in the middle of an argument is kept`() {
        splitCommandLine("""adb shell echo a="b c" d""") shouldBe
            listOf("adb", "shell", "echo", "a=\"b c\"", "d")
    }

    @Test
    fun `unquoted commands split exactly as on whitespace`() {
        val commands = listOf(
            "/sdk/platform-tools/adb devices",
            "/sdk/platform-tools/adb -s emulator-5554 reverse tcp:9023 tcp:9023",
            "/sdk/platform-tools/adb -s ABC123 forward tcp:9024 tcp:9024",
            "adb version",
        )
        commands.forEach { splitCommandLine(it) shouldBe it.split(Regex("\\s+")) }
    }
}
