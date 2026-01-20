package io.github.setheclark.intellij.ui.network.detail

import androidx.compose.ui.Modifier
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.jewel.bridge.JewelComposePanel
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Detail panel for displaying network call information.
 *
 * Migrated to pure Compose in Phase 4.2.
 * Now uses NetworkDetailViewContainer which handles state collection internally,
 * eliminating the need for panel recreation workarounds.
 */
@Inject
class DetailPanel(
    private val project: Project,
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val viewModel: DetailPanelViewModel,
) : JPanel(BorderLayout()) {

    init {
        border = JBUI.Borders.customLineLeft(JBColor.border())

        // Create Compose panel once - it handles all state reactivity internally
        val composePanel = JewelComposePanel(
            focusOnClickInside = true,
            content = {
                io.github.setheclark.intellij.ui.compose.components.NetworkDetailViewContainer(
                    project = project,
                    viewModel = viewModel,
                    modifier = Modifier
                )
            }
        )

        add(composePanel, BorderLayout.CENTER)
    }
}
