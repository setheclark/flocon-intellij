package io.github.setheclark.intellij.flocon.network

import kotlinx.coroutines.flow.Flow

interface NetworkRepository {

    /**
     * Observes the calls of the current app session of a device and package. The calls carry no bodies;
     * use [observeCall] for those.
     */
    fun observeCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>>

    fun observeCall(callId: String): Flow<NetworkCallEntity?>

    /**
     * Observes the current app instance for a device/package combination.
     * Emits the new appInstance whenever it changes (i.e., when a new session starts).
     */
    fun observeCurrentAppInstance(deviceId: String, packageName: String): Flow<String?>

    /**
     * Ids of the calls in the current app session of a device and package whose request or response body
     * contains [text], ignoring case.
     */
    suspend fun searchBodies(deviceId: String, packageName: String, text: String): Set<String>

    suspend fun deleteAll()
}
