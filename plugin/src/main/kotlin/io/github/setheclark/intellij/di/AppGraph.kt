package io.github.setheclark.intellij.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.flocon.FloconBindingContainer
import io.github.setheclark.intellij.network.InMemoryNetworkDataSource
import io.github.setheclark.intellij.network.NetworkDataSource
import io.github.setheclark.intellij.services.ApplicationServiceDelegate
import io.github.setheclark.intellij.settings.NetworkStorageSettingsProvider
import io.github.setheclark.intellij.settings.NetworkStorageSettingsProviderImpl
import io.github.setheclark.intellij.system.Environment
import io.github.setheclark.intellij.system.IntellijEnvironment
import io.github.setheclark.intellij.system.process.ProcessExecutor
import io.github.setheclark.intellij.system.process.SystemProcessExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@DependencyGraph(
    bindingContainers = [
        FloconBindingContainer::class,
    ],
)
interface AppGraph : ProjectGraph.Factory {

    val applicationServiceDelegate: ApplicationServiceDelegate

    @Provides
    @SingleIn(AppScope::class)
    fun provideProcessExecutor(impl: SystemProcessExecutor): ProcessExecutor = impl

    @Provides
    @SingleIn(AppScope::class)
    fun provideNetworkDataSource(impl: InMemoryNetworkDataSource): NetworkDataSource = impl

    @Provides
    @SingleIn(AppScope::class)
    fun provideNetworkStorageSettingsProvider(impl: NetworkStorageSettingsProviderImpl): NetworkStorageSettingsProvider = impl

    @Provides
    @SingleIn(AppScope::class)
    fun provideEnvironment(impl: IntellijEnvironment): Environment = impl

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
