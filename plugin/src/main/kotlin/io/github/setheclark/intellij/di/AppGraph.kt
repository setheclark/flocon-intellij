package io.github.setheclark.intellij.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.services.AdbService
import io.github.setheclark.intellij.services.FloconApplicationService

@SingleIn(AppScope::class)
@DependencyGraph
interface AppGraph : ProjectGraph.Factory{

    val adbService: AdbService

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides applicationService: FloconApplicationService): AppGraph
    }
}