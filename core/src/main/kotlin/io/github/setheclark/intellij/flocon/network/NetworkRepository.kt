package io.github.setheclark.intellij.flocon.network

import io.github.setheclark.intellij.network.BodyType
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {

    /**
     * Observes the calls of the current app session of a device and package. The calls carry no bodies;
     * use [observeCall] for those.
     */
    fun observeCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>>

    /** Observes one call without bodies; use [getBody] for those. */
    fun observeCall(callId: String): Flow<NetworkCallEntity?>

    /** Loads one body of a call in full, off the calling thread. */
    suspend fun getBody(callId: String, type: BodyType): BodyContent

    /** Which bodies exist for a call, without reading their text. */
    suspend fun getBodyTypes(callId: String): Set<BodyType>

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

    /**
     * Clears the list of the current app session of a device and package. Other sessions, devices and apps keep
     * their calls, and the cleared calls stay stored until retention purges them.
     */
    suspend fun clearHistory(deviceId: String, packageName: String)
}
