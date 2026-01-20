package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text

/**
 * A reusable loading state component.
 *
 * @param message Optional message to display below the loading indicator
 * @param modifier Optional modifier for the container
 */
@Composable
fun LoadingState(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp)
            )

            if (message != null) {
                Text(
                    text = message,
                    style = IntelliJTheme.typography.body.copy(
                        color = IntelliJTheme.colors.foreground.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                )
            }
        }
    }
}
