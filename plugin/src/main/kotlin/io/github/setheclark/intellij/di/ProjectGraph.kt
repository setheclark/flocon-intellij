package io.github.setheclark.intellij.di

import com.intellij.openapi.project.Project
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import io.github.setheclark.intellij.services.FloconApplicationService
import io.github.setheclark.intellij.services.FloconProjectService
import io.github.setheclark.intellij.ui.NetworkInspectorPanel

@GraphExtension(ProjectScope::class)
interface ProjectGraph {

    val networkInspectorPanel: NetworkInspectorPanel

    @GraphExtension.Factory
    interface Factory {
        fun createProjectGraph(
            @Provides project: Project,
            @Provides projectService: FloconProjectService,
            @Provides appService: FloconApplicationService,
        ): ProjectGraph
    }
}