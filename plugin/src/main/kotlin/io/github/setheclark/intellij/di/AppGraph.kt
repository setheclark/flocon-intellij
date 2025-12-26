package io.github.setheclark.intellij.di

import dev.zacsweers.metro.*
import io.github.setheclark.intellij.data.DeviceRepository
import io.github.setheclark.intellij.data.DeviceRepositoryImpl
import io.github.setheclark.intellij.data.NetworkCallRepository
import io.github.setheclark.intellij.data.NetworkCallRepositoryImpl
import io.github.setheclark.intellij.managers.adb.AdbManager
import io.github.setheclark.intellij.managers.network.NetworkEventProcessor
import io.github.setheclark.intellij.managers.server.MessageRouter
import io.github.setheclark.intellij.managers.server.ServerManager
import io.github.setheclark.intellij.process.ProcessExecutor
import io.github.setheclark.intellij.process.SystemProcessExecutor
import io.github.setheclark.intellij.services.FloconServerFactory
import io.github.setheclark.intellij.services.ServerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@DependencyGraph
interface AppGraph : UiGraph.Factory {

    // Managers
    val adbManager: AdbManager
    val networkEventProcessor: NetworkEventProcessor
    val messageRouter: MessageRouter
    val serverManager: ServerManager

    // Repositories
    val deviceRepository: DeviceRepository
    val networkCallRepository: NetworkCallRepository

    @Binds
    val DeviceRepositoryImpl.bind: DeviceRepository

    @Binds
    val NetworkCallRepositoryImpl.bind: NetworkCallRepository

    @Binds
    val SystemProcessExecutor.bind: ProcessExecutor

    @Binds
    val FloconServerFactory.bind: ServerFactory

    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides @AppCoroutineScope scope: CoroutineScope,
        ): AppGraph
    }
}