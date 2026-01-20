package io.github.setheclark.intellij.ui.network.list

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.ui.network.NetworkInspectorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.jewel.bridge.JewelComposePanel
import java.awt.BorderLayout
import javax.swing.JPanel

@Inject
class NetworkCallListPanel(
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkCallListViewModel,
    private val parentViewModel: NetworkInspectorViewModel,
) : JPanel(BorderLayout()) {

    init {
        setupComposeTable()
    }

    /**
     * Setup Compose-based network call table.
     *
     * Uses NetworkCallTableContainer which internally collects state
     * from the view model, enabling proper reactive updates.
     */
    private fun setupComposeTable() {
        val composePanel = JewelComposePanel(
            focusOnClickInside = true,
            content = {
                // Collect selected call ID from parent view model
                val selectedCallId by parentViewModel.state
                    .map { it.selectedCallId }
                    .distinctUntilChanged()
                    .collectAsState(initial = null)

                io.github.setheclark.intellij.ui.compose.components.NetworkCallTableContainer(
                    viewModel = viewModel,
                    selectedCallId = selectedCallId,
                    modifier = Modifier
                )
            }
        )

        add(composePanel, BorderLayout.CENTER)
    }
}
