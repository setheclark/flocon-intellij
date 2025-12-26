package io.github.setheclark.intellij.di

import com.intellij.openapi.project.Project
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import io.github.setheclark.intellij.ui.NetworkInspectorPanel

@GraphExtension
interface UiGraph {

    val networkInspectorPanel: NetworkInspectorPanel

    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides project: Project): UiGraph
    }
}