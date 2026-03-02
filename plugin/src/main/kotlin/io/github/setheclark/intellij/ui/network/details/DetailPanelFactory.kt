package io.github.setheclark.intellij.ui.network.details

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.ui.network.details.common.BodyContentPanel
import io.github.setheclark.intellij.ui.network.usecase.ObserveCallUseCase
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.jewel.bridge.compose
import java.awt.BorderLayout
import javax.swing.JPanel

@Inject
class DetailPanelFactory(
    private val project: Project,
    private val observeCallUseCase: ObserveCallUseCase,
) {
    fun create(callId: String): DetailsPanel {
        val viewModel = DetailPanelViewModel(
            callIdFlow = flowOf(callId),
            observeCallUseCase = observeCallUseCase,
        )
        val requestBodyPanel = BodyContentPanel(project)
        val responseBodyPanel = BodyContentPanel(project)
        return DetailsPanel(viewModel, requestBodyPanel, responseBodyPanel)
    }

    class DetailsPanel(
        viewModel: DetailPanelViewModel,
        requestBodyPanel: BodyContentPanel,
        responseBodyPanel: BodyContentPanel,
    ) : JPanel(BorderLayout()) {

        init {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.customLineLeft(JBColor.border())
            add(
                compose(focusOnClickInside = true) { DetailsContent(viewModel, requestBodyPanel, responseBodyPanel) },
                BorderLayout.CENTER,
            )
        }
    }
}
