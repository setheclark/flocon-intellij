package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.NetworkRepository

@Inject
class ClearAllNetworkCallsUseCase(
    private val networkRepository: NetworkRepository,
) {
    suspend operator fun invoke() {
        networkRepository.deleteAll()
    }
}
