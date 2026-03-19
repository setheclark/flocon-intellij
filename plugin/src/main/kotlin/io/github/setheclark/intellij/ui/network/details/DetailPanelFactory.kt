package io.github.setheclark.intellij.ui.network.details

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.ui.WithProject
import io.github.setheclark.intellij.ui.network.usecase.ObserveCallUseCase
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.jewel.bridge.compose
import java.awt.BorderLayout
import javax.swing.JPanel

enum class DetailLayoutMode {
    ToolWindow,
    Editor,
}

@Inject
class DetailPanelFactory(
    private val project: Project,
    private val observeCallUseCase: ObserveCallUseCase,
) {
    fun create(callId: String, layoutMode: DetailLayoutMode = DetailLayoutMode.ToolWindow): DetailsPanel {
        val viewModel = DetailPanelViewModel(
            callIdFlow = flowOf(callId),
            observeCallUseCase = observeCallUseCase,
            layoutMode = layoutMode,
        )
        return DetailsPanel(project, viewModel)
    }

    class DetailsPanel(
        project: Project,
        viewModel: DetailPanelViewModel,
    ) : JPanel(BorderLayout()) {

        init {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.customLineLeft(JBColor.border())
            add(
                compose(focusOnClickInside = true) {
                    WithProject(project) {
                        DetailsContent(viewModel)
                    }
                },
                BorderLayout.CENTER,
            )
        }
    }
}
