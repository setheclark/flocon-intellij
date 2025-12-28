package io.github.setheclark.intellij.ui.list

class NetworkCallListItem(
    val callId: String,
    val startTime: String,
    val url: String,
    val method: String,
    val status: String?,
    val duration: String?,
    val size: String?,
)