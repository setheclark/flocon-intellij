package io.github.setheclark.intellij.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.github.setheclark.intellij.services.AdbService
import io.github.setheclark.intellij.services.FloconApplicationService
import io.github.setheclark.intellij.services.FloconProjectService

@DependencyGraph
interface UiGraph {

    val networkInspectorPanel: NetworkInspectorPanel

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides project: Project): UiGraph
    }

    @Provides
    fun providesFloconProjectService(
        project: Project,
    ): FloconProjectService = project.service()

    @Provides
    fun providesFloconApplicationService(
        project: Project,
    ): FloconApplicationService = project.service()

    @Provides
    fun providesAdbService(
        project: Project,
    ) : AdbService = project.service()
}