package io.github.setheclark.intellij.network

import co.touchlab.kermit.Logger
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Memory-only [NetworkDataSource], used when the history database cannot be opened.
 *
 * - Call metadata is held in memory, bodies in a [BodyStore] that compresses and truncates them.
 * - The oldest calls are evicted beyond [MAX_STORED_CALLS].
 * - Calls of earlier app sessions are kept, but only the current session is listed.
 */
class InMemoryNetworkDataSource(
    private val bodyStore: BodyStore = BodyStore(),
) : NetworkDataSource {

    private val log = Logger.withPluginTag("InMemoryNetworkDataSource")

    private val mutex = Mutex()

    // Entities are stored without bodies; bodies live in the BodyStore.
    // LinkedHashMap keeps insertion order for FIFO eviction.
    private val calls = MutableStateFlow<LinkedHashMap<String, StoredCall>>(linkedMapOf())

    // App instance of the most recent call for each device/package combination.
    private val currentAppInstances = MutableStateFlow<Map<DevicePackageKey, String>>(emptyMap())

    override fun observeCurrentAppInstance(deviceId: String, packageName: String): Flow<String?> {
        val key = DevicePackageKey(deviceId, packageName)
        return currentAppInstances
            .map { it[key] }
            .distinctUntilChanged()
    }

    override suspend fun insert(entity: NetworkCallEntity) {
        val requestBodyKey = bodyStore.store(entity.callId, BodyType.REQUEST, entity.request.body)
        val responseBodyKey = storeResponseBody(entity)

        val storedCall = StoredCall(
            entity = entity.stripBodies(),
            requestBodyKey = requestBodyKey,
            responseBodyKey = responseBodyKey,
        )

        mutex.withLock {
            currentAppInstances.update { it + (DevicePackageKey(entity.deviceId, entity.packageName) to entity.appInstance) }
            calls.update { current ->
                val updated = LinkedHashMap(current)
                // Re-inserting an ID moves it to the end, like a new call
                updated.remove(entity.callId)
                updated[entity.callId] = storedCall
                evictOldestIfNeeded(updated)
                updated
            }
        }
    }

    override suspend fun update(entity: NetworkCallEntity) {
        mutex.withLock {
            val current = calls.value[entity.callId] ?: return

            val responseBodyKey = storeResponseBody(entity)

            val storedCall = current.copy(
                entity = entity.stripBodies(),
                responseBodyKey = responseBodyKey,
            )

            calls.update { currentMap ->
                val updated = LinkedHashMap(currentMap)
                updated[entity.callId] = storedCall
                updated
            }
        }
    }

    override suspend fun getByCallId(callId: String): NetworkCallEntity? {
        val storedCall = calls.value[callId] ?: return null
        return storedCall.hydrate()
    }

    override fun observeByCallId(callId: String): Flow<NetworkCallEntity?> {
        return calls.map { map ->
            map[callId]?.hydrate()
        }
    }

    override fun observeCurrentSessionCalls(
        deviceId: String,
        packageName: String,
    ): Flow<List<NetworkCallEntity>> {
        val key = DevicePackageKey(deviceId, packageName)
        return combine(calls, currentAppInstances) { map, instances ->
            val appInstance = instances[key] ?: return@combine emptyList()
            map.values
                .map { it.entity }
                .filter {
                    it.deviceId == deviceId && it.packageName == packageName && it.appInstance == appInstance
                }
        }
    }

    override suspend fun searchBodies(deviceId: String, packageName: String, text: String): Set<String> {
        val appInstance = currentAppInstances.value[DevicePackageKey(deviceId, packageName)] ?: return emptySet()
        return calls.value.values
            .filter {
                it.entity.deviceId == deviceId && it.entity.packageName == packageName && it.entity.appInstance == appInstance
            }
            .filter { call ->
                val hydrated = call.hydrate()
                hydrated.request.body.contains(text) ||
                    (hydrated.response as? NetworkResponse.Success)?.body.contains(text)
            }
            .map { it.entity.callId }
            .toSet()
    }

    private fun String?.contains(text: String) = this?.contains(text, ignoreCase = true) == true

    override suspend fun deleteByCallId(callId: String) {
        mutex.withLock {
            bodyStore.removeAll(callId)
            calls.update { current ->
                val updated = LinkedHashMap(current)
                updated.remove(callId)
                updated
            }
        }
    }

    override suspend fun deleteByDeviceAndPackage(deviceId: String, packageName: String) {
        mutex.withLock {
            val toRemove = calls.value.values
                .filter { it.entity.deviceId == deviceId && it.entity.packageName == packageName }
                .map { it.entity.callId }

            toRemove.forEach { bodyStore.removeAll(it) }

            calls.update { current ->
                val updated = LinkedHashMap(current)
                toRemove.forEach { updated.remove(it) }
                updated
            }
            currentAppInstances.update { it - DevicePackageKey(deviceId, packageName) }
        }
    }

    override suspend fun deleteAll() {
        mutex.withLock {
            bodyStore.clear()
            calls.value = linkedMapOf()
            currentAppInstances.value = emptyMap()
        }
    }

    private suspend fun storeResponseBody(entity: NetworkCallEntity): String? =
        when (val response = entity.response) {
            is NetworkResponse.Success -> bodyStore.store(entity.callId, BodyType.RESPONSE, response.body)
            is NetworkResponse.Failure, null -> null
        }

    private suspend fun evictOldestIfNeeded(map: LinkedHashMap<String, StoredCall>) {
        while (map.size > MAX_STORED_CALLS) {
            val oldest = map.entries.firstOrNull() ?: break
            log.v { "Evicting oldest call: ${oldest.key}" }
            bodyStore.removeAll(oldest.key)
            map.remove(oldest.key)
        }
    }

    private fun NetworkCallEntity.stripBodies(): NetworkCallEntity {
        return copy(
            request = request.copy(body = null),
            response = when (val r = response) {
                is NetworkResponse.Success -> r.copy(body = null)
                is NetworkResponse.Failure -> r
                null -> null
            },
        )
    }

    private suspend fun StoredCall.hydrate(): NetworkCallEntity {
        val requestBody = bodyStore.retrieve(requestBodyKey)
        val responseBody = bodyStore.retrieve(responseBodyKey)

        return entity.copy(
            request = entity.request.copy(body = requestBody),
            response = when (val r = entity.response) {
                is NetworkResponse.Success -> r.copy(body = responseBody)
                is NetworkResponse.Failure -> r
                null -> null
            },
        )
    }

    private companion object {
        const val MAX_STORED_CALLS = 1000
    }
}

/**
 * Internal representation of a stored network call.
 * Bodies are stored separately and referenced by key.
 */
private data class StoredCall(
    val entity: NetworkCallEntity,
    val requestBodyKey: String?,
    val responseBodyKey: String?,
)

/**
 * Key for tracking app sessions by device and package.
 */
private data class DevicePackageKey(
    val deviceId: String,
    val packageName: String,
)
