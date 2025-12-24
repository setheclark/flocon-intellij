package io.github.setheclark.intellij.fixtures

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * Common test utilities and fixtures.
 */
object TestFixtures {

    /**
     * Creates a test coroutine scope with a StandardTestDispatcher.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun createTestScope(): TestScope = TestScope()

    /**
     * Creates a test dispatcher for use in services.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun createTestDispatcher(): CoroutineDispatcher = StandardTestDispatcher()
}

/**
 * Extension to help with common test patterns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.advanceUntilIdleAndGet() {
    testScheduler.advanceUntilIdle()
}
