package io.github.setheclark.intellij.di

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.ui.ViewModelScopeDisposable
import io.github.setheclark.intellij.ui.network.NetworkInspectorPanel
import io.github.setheclark.intellij.ui.network.detail.DetailPanelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@GraphExtension
@SingleIn(ProjectScope::class)
interface ProjectGraph {

    val networkInspectorPanel: NetworkInspectorPanel

    val detailPanelFactory: DetailPanelFactory

    /**
     * Disposable that cancels the UI scope.
     * Register this with the tool window content for proper cleanup.
     */
    val viewModelScopeDisposable: ViewModelScopeDisposable

    @Provides
    @SingleIn(ProjectScope::class)
    @ViewModelCoroutineScope
    fun provideViewModelScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides project: Project): ProjectGraph
    }
}
