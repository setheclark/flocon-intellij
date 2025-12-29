package io.github.setheclark.intellij.flocon.network

import kotlinx.coroutines.flow.Flow

interface NetworkRepository {

    fun observeCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>>

    fun observeCall(callId: String): Flow<NetworkCallEntity?>
}