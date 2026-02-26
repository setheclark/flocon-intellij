package io.github.setheclark.intellij.flocon.network

import kotlinx.coroutines.flow.Flow

interface NetworkRepository {

    fun observeCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>>

    fun observeCall(callId: String): Flow<NetworkCallEntity?>

    /**
     * Observes the current app instance for a device/package combination.
     * Emits the new appInstance whenever it changes (i.e., when a new session starts).
     */
    fun observeCurrentAppInstance(deviceId: String, packageName: String): Flow<String?>

    suspend fun deleteAll()
}
