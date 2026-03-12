package io.github.setheclark.intellij.di

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.ui.network.NetworkCallTabManager
import io.github.setheclark.intellij.ui.network.NetworkInspectorViewModel
import io.github.setheclark.intellij.ui.network.details.DetailPanelFactory
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterViewModel
import io.github.setheclark.intellij.ui.network.list.NetworkCallListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@GraphExtension
@SingleIn(ProjectScope::class)
interface ProjectGraph {

    val project: Project

    val networkInspectorViewModel: NetworkInspectorViewModel

    val networkFilterViewModel: NetworkFilterViewModel

    val networkCallListViewModel: NetworkCallListViewModel

    val networkCallTabManager: NetworkCallTabManager

    val detailPanelFactory: DetailPanelFactory

    @Provides
    @SingleIn(ProjectScope::class)
    @ViewModelCoroutineScope
    fun provideViewModelScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.EDT).also {
        Disposer.register(project) { it.cancel() }
    }

    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides project: Project): ProjectGraph
    }
}
