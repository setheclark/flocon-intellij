package io.github.setheclark.intellij.ui.network.list

import androidx.compose.runtime.Immutable

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
)
