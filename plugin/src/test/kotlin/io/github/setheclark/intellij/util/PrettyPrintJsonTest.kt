package io.github.setheclark.intellij.util

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrettyPrintJsonTest {

    @Test
    fun `valid json is indented`() = runTest {
        prettyPrintJson("""{"a":[1,2],"b":"x"}""") shouldBe
            "{\n    \"a\": [\n        1,\n        2\n    ],\n    \"b\": \"x\"\n}"
    }

    @Test
    fun `invalid json returns null so the caller shows the raw body`() = runTest {
        prettyPrintJson("{not json") shouldBe null
    }
}
