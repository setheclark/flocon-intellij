package io.github.setheclark.intellij.ui.network.list

import androidx.compose.runtime.Immutable
import io.github.setheclark.intellij.domain.models.RequestTypeFilter

@Immutable
data class NetworkCallListItem(
    val callId: String,
    val startTime: Long,
    val name: String,
    val url: String,
    val method: String,
    val status: Int?,
    val duration: Double?,
    val size: Long?,
    val requestType: RequestTypeFilter,
)
