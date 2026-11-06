package io.github.setheclark.intellij.flocon.adb

import io.github.openflocon.domain.adb.model.DeviceWithSerialDomainModel
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.common.Failure
import io.github.openflocon.domain.common.Success
import io.github.setheclark.intellij.fakes.FakeProcessExecutor
import io.github.setheclark.intellij.fakes.FakeSettingsRepository
import io.github.setheclark.intellij.system.Environment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExecuteDeviceAdbCommandUseCaseTest {

    private val dispatchers = object : DispatcherProvider {
        override val ui = Dispatchers.Unconfined
        override val viewModel = Dispatchers.Unconfined
        override val domain = Dispatchers.Unconfined
        override val data = Dispatchers.Unconfined
    }

    private val environment = object : Environment {
        override fun getEnvironmentVariable(key: String): String? = null
    }

    private val settings = FakeSettingsRepository()

    private val dataSource = InMemoryAdbLocalDataSource()
    private val executor = FakeProcessExecutor()
    private val useCase = ExecuteDeviceAdbCommandUseCase(
        AdbRepositoryImpl(dataSource, dispatchers, executor, environment),
        settings,
    )

    @Test
    fun `unknown serial returns error and runs nothing`() = runTest {
        val result = useCase("device-1", "shell ls")

        (result is Failure) shouldBe true
        (result as Failure).value.message!! shouldContain "device-1"
        executor.executed shouldBe emptyList()
    }

    @Test
    fun `known serial runs command targeted at that serial`() = runTest {
        dataSource.add(DeviceWithSerialDomainModel("device-1", "SERIAL1"))

        val result = useCase("device-1", """shell echo "a b"""")

        (result is Success) shouldBe true
        executor.executed shouldBe listOf(listOf("/sdk/adb", "-s", "SERIAL1", "shell", "echo", "\"a b\""))
    }

    @Test
    fun `forwarding and device listing commands are unchanged`() = runTest {
        val repo = AdbRepositoryImpl(
            dataSource, dispatchers,
            FakeProcessExecutor { args ->
                if (args.last() == "devices") {
                    Success("List of devices attached\nS1\tdevice\n")
                } else {
                    Success("")
                }
            }.also { forwardExecutor = it },
            environment,
        )

        repo.executeAdbCommand("/sdk/adb", null, "forward tcp:9023 tcp:9023")

        forwardExecutor.executed shouldBe listOf(
            listOf("/sdk/adb", "devices"),
            listOf("/sdk/adb", "-s", "S1", "forward", "tcp:9023", "tcp:9023"),
        )
    }

    private lateinit var forwardExecutor: FakeProcessExecutor
}
