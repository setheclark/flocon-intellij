package io.github.setheclark.intellij.ui.network.list

class NetworkCallListItem(
    val callId: String,
    val startTime: Long,
    val url: String,
    val method: String,
    val status: Int?,
    val duration: Double?,
    val size: Long?,
)