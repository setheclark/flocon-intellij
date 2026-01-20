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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.ui.compose.theme.IntelliJTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Network call detail view with tabs for Overview, Response, and Request.
 *
 * Pure Compose implementation migrated in Phase 4.2.
 * Replaces Swing JBTabs with Compose tab navigation.
 *
 * @param project IntelliJ project for editor integration
 * @param call Network call to display (null shows empty state)
 * @param modifier Optional modifier
 */
@Composable
fun NetworkDetailView(
    project: Project,
    call: NetworkCallEntity?,
    modifier: Modifier = Modifier
) {
    if (call == null) {
        // Empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a request to view details",
                style = IntelliJTheme.typography.body.copy(
                    color = IntelliJTheme.colors.foreground.copy(alpha = 0.5f)
                )
            )
        }
    } else {
        NetworkDetailContent(
            project = project,
            call = call,
            modifier = modifier
        )
    }
}

/**
 * Content view when a call is selected.
 */
@Composable
private fun NetworkDetailContent(
    project: Project,
    call: NetworkCallEntity,
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
            DetailTabHeader(
                text = "Overview",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            DetailTabHeader(
                text = "Response",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
            DetailTabHeader(
                text = "Request",
                isSelected = selectedTab == 2,
                onClick = { selectedTab = 2 }
            )
        }

        // Tab content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> NetworkOverview(
                    call = call,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> ResponseView(
                    project = project,
                    response = call.response,
                    callName = call.name,
                    timestamp = call.startTime,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> RequestView(
                    project = project,
                    request = call.request,
                    callName = call.name,
                    timestamp = call.startTime,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Tab header button.
 */
@Composable
private fun DetailTabHeader(
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) IntelliJTheme.colors.foreground
                else IntelliJTheme.colors.foreground.copy(alpha = 0.6f)
            )
        )
    }
}
