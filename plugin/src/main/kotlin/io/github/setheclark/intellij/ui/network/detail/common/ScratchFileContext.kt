package io.github.setheclark.intellij.ui.network.detail.common

/**
 * Context information for creating a scratch file from body content.
 */
data class ScratchFileContext(
    val queryName: String,
    val bodyType: BodyType,
    val statusCode: Int?,
    val timestamp: Long,
    val body: String,
    val contentType: String?,
) {
    enum class BodyType {
        REQUEST,
        RESPONSE,
    }
}
