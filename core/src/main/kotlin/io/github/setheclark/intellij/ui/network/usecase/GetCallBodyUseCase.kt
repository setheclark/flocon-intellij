package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.BodyContent
import io.github.setheclark.intellij.flocon.network.NetworkRepository
import io.github.setheclark.intellij.network.BodyType

/** Loads request and response bodies on demand; bodies are never part of the observed call. */
@Inject
class GetCallBodyUseCase(
    private val networkRepository: NetworkRepository,
) {
    suspend operator fun invoke(callId: String, type: BodyType): BodyContent =
        networkRepository.getBody(callId, type)

    /** Which bodies exist, without reading any body text. */
    suspend fun available(callId: String): Set<BodyType> = networkRepository.getBodyTypes(callId)
}
