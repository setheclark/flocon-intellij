package io.github.setheclark.intellij.ui.network.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.setheclark.intellij.stringResource
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
fun DetailsContent(
    viewModel: DetailPanelViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedCall by viewModel.selectedCall.collectAsState(initial = null)
    val currentCall = selectedCall

    if (currentCall == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource("label.selectRequest"),
                color = JewelTheme.globalColors.text.info,
            )
        }
        return
    }

    EditorDetailLayout(
        call = currentCall,
        modifier = modifier.fillMaxSize(),
    )
}
