package io.github.setheclark.intellij.adb

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.adb.AdbStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
class AdbStatusDataSource {
    private val _status = MutableStateFlow<AdbStatus>(AdbStatus.Initializing)

    val status: StateFlow<AdbStatus> = _status.asStateFlow()

    fun setAdbPath(path: String) {
        _status.value = AdbStatus.Available(path)
    }

    fun adbNotFound() {
        _status.value = AdbStatus.NotFound
    }
}