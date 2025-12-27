package io.github.setheclark.intellij.di

import com.intellij.openapi.project.Project
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.ui.NetworkInspectorPanel
import io.github.setheclark.intellij.ui.UiScopeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@GraphExtension
@SingleIn(UiScope::class)
interface UiGraph {

    val networkInspectorPanel: NetworkInspectorPanel

    /**
     * Disposable that cancels the UI scope.
     * Register this with the tool window content for proper cleanup.
     */
    val uiScopeDisposable: UiScopeDisposable

    @Provides
    @SingleIn(UiScope::class)
    @UiCoroutineScope
    fun provideUiScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides project: Project): UiGraph
    }
}