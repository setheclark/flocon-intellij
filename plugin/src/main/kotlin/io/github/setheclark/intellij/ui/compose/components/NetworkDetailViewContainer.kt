package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.ui.network.detail.DetailPanelViewModel

/**
 * Container that handles state collection for NetworkDetailView.
 *
 * This wrapper enables proper reactive state updates within JewelComposePanel
 * during Swing-Compose interop (Phase 4.2). The composable collects state
 * internally, allowing the UI to react to changes without panel recreation.
 *
 * @param project IntelliJ project for editor integration
 * @param viewModel The view model providing selected call state
 * @param onClose Optional callback when close button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun NetworkDetailViewContainer(
    project: Project,
    viewModel: DetailPanelViewModel,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedCall by viewModel.selectedCall.collectAsState(initial = null)

    NetworkDetailView(
        project = project,
        call = selectedCall,
        onClose = onClose,
        modifier = modifier
    )
}
