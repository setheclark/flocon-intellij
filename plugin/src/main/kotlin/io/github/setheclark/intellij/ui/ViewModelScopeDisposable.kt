package io.github.setheclark.intellij.ui

import com.intellij.openapi.Disposable
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * Disposable that cancels the UI CoroutineScope when the tool window is disposed.
 *
 * Register this with the tool window content to ensure all UI coroutines
 * are cancelled when the tool window closes.
 */
@Inject
class ViewModelScopeDisposable(
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
) : Disposable {

    override fun dispose() {
        scope.cancel()
    }
}
