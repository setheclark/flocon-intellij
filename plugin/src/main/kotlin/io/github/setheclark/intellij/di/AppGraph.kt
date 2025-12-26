package io.github.setheclark.intellij.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.data.DeviceRepository
import io.github.setheclark.intellij.data.DeviceRepositoryImpl
import io.github.setheclark.intellij.data.NetworkCallRepository
import io.github.setheclark.intellij.data.NetworkCallRepositoryImpl
import io.github.setheclark.intellij.managers.adb.AdbManager
import io.github.setheclark.intellij.managers.network.NetworkEventProcessor
import io.github.setheclark.intellij.managers.server.MessageRouter
import io.github.setheclark.intellij.managers.server.ServerManager
import io.github.setheclark.intellij.services.*
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@DependencyGraph
interface AppGraph : ProjectGraph.Factory {

    // Managers
    val adbManager: AdbManager
    val networkEventProcessor: NetworkEventProcessor
    val messageRouter: MessageRouter
    val serverManager: ServerManager

    // Repositories
    val deviceRepository: DeviceRepository
    val networkCallRepository: NetworkCallRepository

    @Provides
    @SingleIn(AppScope::class)
    fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository = impl

    @Provides
    @SingleIn(AppScope::class)
    fun bindNetworkCallRepository(impl: NetworkCallRepositoryImpl): NetworkCallRepository = impl

    // Infrastructure providers
    @Provides
    @SingleIn(AppScope::class)
    fun provideProcessExecutor(impl: SystemProcessExecutor): ProcessExecutor = impl

    @Provides
    @SingleIn(AppScope::class)
    fun provideServerFactory(impl: FloconServerFactory): ServerFactory = impl

    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides applicationService: FloconApplicationService): AppGraph
    }
}