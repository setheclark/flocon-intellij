package io.github.setheclark.intellij.network

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import kotlinx.coroutines.flow.Flow

interface NetworkDataSource {

    /**
     * Inserts a new network call entry.
     */
    suspend fun insert(entity: NetworkCallEntity)

    /**
     * Updates an existing network call entry.
     */
    suspend fun update(entity: NetworkCallEntity)

    /**
     * Retrieves a specific network call by its ID.
     */
    suspend fun getByCallId(callId: String): NetworkCallEntity?

    /**
     * Observes a specific network call by its ID.
     */
    fun observeByCallId(callId: String): Flow<NetworkCallEntity?>

    /**
     * Retrieves all network calls for a specific device and package.
     */
    suspend fun getByDeviceAndPackage(deviceId: String, packageName: String): List<NetworkCallEntity>

    /**
     * Observes all network calls for a specific device and package.
     */
    fun observeByDeviceAndPackage(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>>

    /**
     * Deletes a specific network call by its ID.
     */
    suspend fun deleteByCallId(callId: String)

    /**
     * Deletes all network calls for a specific device and package.
     */
    suspend fun deleteByDeviceAndPackage(deviceId: String, packageName: String)

    /**
     * Deletes all network call entries.
     */
    suspend fun deleteAll()
}