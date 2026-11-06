package io.github.setheclark.intellij.network

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import kotlinx.coroutines.flow.Flow

interface NetworkDataSource {

    /**
     * Observes the current app session of a device/package combination: the app instance of its most recent call.
     * Emits the new app instance whenever a call from a newer session arrives.
     */
    fun observeCurrentAppInstance(deviceId: String, packageName: String): Flow<String?>

    /**
     * Inserts a new network call entry, replacing any call with the same ID.
     */
    suspend fun insert(entity: NetworkCallEntity)

    /**
     * Stores the response of an existing network call. The request side is fixed at [insert] and is not rewritten.
     * Does nothing when the call is unknown.
     */
    suspend fun update(entity: NetworkCallEntity)

    /**
     * Retrieves a specific network call by its ID, with bodies.
     */
    suspend fun getByCallId(callId: String): NetworkCallEntity?

    /**
     * Observes a specific network call by its ID, with bodies.
     */
    fun observeByCallId(callId: String): Flow<NetworkCallEntity?>

    /**
     * Observes the calls of the current app session of a device and package, oldest first.
     * The calls do not carry bodies; use [getByCallId] or [observeByCallId] for those.
     */
    fun observeCurrentSessionCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>>

    /**
     * Ids of the calls in the current app session of a device and package whose request or response body
     * contains [text], ignoring case.
     */
    suspend fun searchBodies(deviceId: String, packageName: String, text: String): Set<String>

    /**
     * Deletes a specific network call and its bodies.
     */
    suspend fun deleteByCallId(callId: String)

    /**
     * Deletes all network calls and bodies of a specific device and package, across all app sessions.
     */
    suspend fun deleteByDeviceAndPackage(deviceId: String, packageName: String)

    /**
     * Deletes all network calls and bodies.
     */
    suspend fun deleteAll()
}
