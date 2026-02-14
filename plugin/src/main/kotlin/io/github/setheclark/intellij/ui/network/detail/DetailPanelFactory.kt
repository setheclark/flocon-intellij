package io.github.setheclark.intellij.ui.network.detail

import com.intellij.openapi.project.Project
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.ui.network.detail.common.BodyContentPanel
import io.github.setheclark.intellij.ui.network.detail.overview.OverviewPanel
import io.github.setheclark.intellij.ui.network.detail.request.RequestPanel
import io.github.setheclark.intellij.ui.network.detail.response.ResponsePanel
import io.github.setheclark.intellij.ui.network.usecase.ObserveCallUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf

@Inject
class DetailPanelFactory(
    private val project: Project,
    @param:ViewModelCoroutineScope private val parentScope: CoroutineScope,
    private val observeCallUseCase: ObserveCallUseCase,
) {
    fun create(callId: String): Pair<DetailPanel, CoroutineScope> {
        val tabScope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
        val viewModel = DetailPanelViewModel(
            callIdFlow = flowOf(callId),
            observeCallUseCase = observeCallUseCase,
        )
        val overviewPanel = OverviewPanel()
        val requestPanel = RequestPanel(project, BodyContentPanel(project))
        val responsePanel = ResponsePanel(project, BodyContentPanel(project))
        val panel = DetailPanel(project, tabScope, viewModel, overviewPanel, requestPanel, responsePanel)
        return panel to tabScope
    }
}
