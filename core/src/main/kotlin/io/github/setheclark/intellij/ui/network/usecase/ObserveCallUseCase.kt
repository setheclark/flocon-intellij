package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

@Inject
class ObserveCallUseCase(
    private val networkRepository: NetworkRepository,
) {
    operator fun invoke(callId: String): Flow<NetworkCallEntity> =
        networkRepository.observeCall(callId).filterNotNull()
}
