package io.github.setheclark.intellij.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.flocon.FloconBindingContainer
import io.github.setheclark.intellij.network.InMemoryNetworkDataSource
import io.github.setheclark.intellij.network.NetworkDataSource
import io.github.setheclark.intellij.process.ProcessExecutor
import io.github.setheclark.intellij.process.SystemProcessExecutor
import io.github.setheclark.intellij.services.ApplicationServiceDelegate
import io.github.setheclark.intellij.settings.NetworkStorageSettingsProvider
import io.github.setheclark.intellij.settings.NetworkStorageSettingsProviderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@DependencyGraph(
    bindingContainers = [
        FloconBindingContainer::class,
    ]
)
interface AppGraph : ProjectGraph.Factory {

    val applicationServiceDelegate: ApplicationServiceDelegate

    @Binds
    val SystemProcessExecutor.bind: ProcessExecutor

    @Binds
    val InMemoryNetworkDataSource.bind: NetworkDataSource

    @Binds
    val NetworkStorageSettingsProviderImpl.bind: NetworkStorageSettingsProvider

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