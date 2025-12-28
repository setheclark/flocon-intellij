package io.github.setheclark.intellij.di

import dev.zacsweers.metro.*
import io.github.setheclark.intellij.flocon.network.NetworkLocalDataSourceImpl
import io.github.openflocon.data.core.network.datasource.NetworkLocalWebsocketDataSource
import io.github.openflocon.data.core.network.datasource.NetworkMocksLocalDataSource
import io.github.openflocon.data.core.network.datasource.NetworkQualityLocalDataSource
import io.github.openflocon.data.core.network.datasource.NetworkRemoteDataSource
import io.github.openflocon.data.core.network.datasource.NetworkReplayDataSource
import io.github.openflocon.data.core.network.repository.NetworkRepositoryImpl
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.network.repository.NetworkImageRepository
import io.github.setheclark.intellij.data.DeviceRepository
import io.github.setheclark.intellij.data.DeviceRepositoryImpl
import io.github.setheclark.intellij.data.NetworkCallRepository
import io.github.setheclark.intellij.data.NetworkCallRepositoryImpl
import io.github.setheclark.intellij.managers.server.ServerManager
import io.github.setheclark.intellij.network.PluginNetworkRepository
import io.github.setheclark.intellij.network.PluginNetworkRepositoryImpl
import io.github.setheclark.intellij.process.ProcessExecutor
import io.github.setheclark.intellij.process.SystemProcessExecutor
import io.github.setheclark.intellij.services.ApplicationServiceDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@DependencyGraph(
    bindingContainers = [
        FloconBindingContainer::class,
        UseCaseBindingContainer::class,
    ]
)
interface AppGraph : UiGraph.Factory {

    val applicationServiceDelegate: ApplicationServiceDelegate

    val serverManager: ServerManager

    @Binds
    val DeviceRepositoryImpl.bind: DeviceRepository

    @Binds
    val NetworkCallRepositoryImpl.bind: NetworkCallRepository

    @Binds
    val SystemProcessExecutor.bind: ProcessExecutor

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