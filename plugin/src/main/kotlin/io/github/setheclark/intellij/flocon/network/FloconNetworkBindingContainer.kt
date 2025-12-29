package io.github.setheclark.intellij.flocon.network

import com.flocon.data.remote.server.Server
import com.flocon.data.remote.server.ServerJvm
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.flocon.network.datasource.InMemoryNetworkDataSource
import io.github.setheclark.intellij.flocon.network.datasource.NetworkDataSource
import kotlinx.serialization.json.Json

@BindingContainer
interface FloconNetworkBindingContainer {

    @Binds
    val InMemoryNetworkDataSource.bind: NetworkDataSource

    @Binds
    val NetworkRepositoryImpl.bind: NetworkRepository

    companion object {

        @Provides
        @SingleIn(AppScope::class)
        fun provideServer(json: Json): Server = ServerJvm(json)
    }
}