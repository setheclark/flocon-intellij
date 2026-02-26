package io.github.setheclark.intellij.flocon.coroutines

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Inject
@SingleIn(AppScope::class)
class DispatcherProviderImpl : DispatcherProvider {
    override val ui: CoroutineDispatcher = Dispatchers.Main
    override val viewModel: CoroutineDispatcher = Dispatchers.Default
    override val domain: CoroutineDispatcher = Dispatchers.Default
    override val data: CoroutineDispatcher = Dispatchers.IO
}
