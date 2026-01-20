package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import io.github.setheclark.intellij.ui.network.detail.common.ScratchFileContext
import org.jetbrains.jewel.ui.component.Text

/**
 * Compose version of RequestPanel with tabs for Headers and Body.
 *
 * Migrated in Phase 2.2. Uses Compose tab navigation with Swing
 * components wrapped inside (HeadersTablePanel, BodyContentPanel).
 *
 * @param project IntelliJ project for editor integration
 * @param request Network request data to display
 * @param callName Name of the network call
 * @param timestamp Request timestamp
 * @param modifier Optional modifier
 */
@Composable
fun RequestView(
    project: Project,
    request: NetworkRequest,
    callName: String,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IntelliJTheme.colors.background)
        ) {
            TabHeader(
                text = "Headers",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            TabHeader(
                text = "Body",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
        }

        // Tab content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> HeadersTableView(
                    headers = request.headers,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> {
                    val contentType = request.headers.entries
                        .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                        ?.value

                    val context = request.body?.let {
                        ScratchFileContext(
                            queryName = callName,
                            bodyType = ScratchFileContext.BodyType.REQUEST,
                            statusCode = null,
                            timestamp = timestamp,
                            body = it,
                            contentType = contentType
                        )
                    }

                    BodyContentView(
                        project = project,
                        body = request.body,
                        contentType = contentType,
                        context = context,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Simple tab header button.
 */
@Composable
private fun TabHeader(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) IntelliJTheme.colors.background
                else IntelliJTheme.colors.background.copy(alpha = 0.7f)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = IntelliJTheme.typography.body.copy(
                color = if (isSelected) IntelliJTheme.colors.foreground
                else IntelliJTheme.colors.foreground.copy(alpha = 0.6f)
            )
        )
    }
}
