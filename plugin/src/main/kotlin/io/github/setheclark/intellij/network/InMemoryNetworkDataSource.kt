package io.github.setheclark.intellij.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.settings.PluginSettingsProvider
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory network data source with compressed body storage and memory limits.
 *
 * - Network call metadata is stored in memory with bodies stored separately in [BodyStore]
 * - Bodies are compressed using GZIP to reduce memory footprint
 * - Oldest calls are evicted when [io.github.setheclark.intellij.settings.PluginSettings.maxStoredCalls] is exceeded
 * - Bodies are evicted (LRU) when [io.github.setheclark.intellij.settings.PluginSettings.maxBodyCacheSizeBytes] is exceeded
 */
@Inject
@SingleIn(AppScope::class)
class InMemoryNetworkDataSource(
    private val bodyStore: BodyStore,
    private val settingsProvider: PluginSettingsProvider,
) : NetworkDataSource {

    private val log = Logger.withPluginTag("InMemoryNetworkDataSource")

    private val mutex = Mutex()

    // Stores entities without bodies (bodies stored in BodyStore)
    // Using LinkedHashMap to maintain insertion order for FIFO eviction
    private val calls = MutableStateFlow<LinkedHashMap<String, StoredCall>>(linkedMapOf())

    // Tracks current appInstance for each device/package combination
    // When a new session starts (different appInstance), old calls are cleared
    private val currentAppInstances = MutableStateFlow<Map<DevicePackageKey, String>>(emptyMap())

    override fun observeCurrentAppInstance(deviceId: String, packageName: String): Flow<String?> {
        val key = DevicePackageKey(deviceId, packageName)
        return currentAppInstances
            .map { it[key] }
            .distinctUntilChanged()
    }

    override suspend fun insert(entity: NetworkCallEntity) {
        val settings = settingsProvider.getSettings()

        // Check for new session - if appInstance changed, clear previous session's calls
        val devicePackageKey = DevicePackageKey(entity.deviceId, entity.packageName)
        mutex.withLock {
            val previousAppInstance = currentAppInstances.value[devicePackageKey]
            if (previousAppInstance != null && previousAppInstance != entity.appInstance) {
                log.i { "New session detected for ${entity.packageName} on ${entity.deviceId}. Clearing previous session calls." }
                clearCallsForDevicePackageUnsafe(entity.deviceId, entity.packageName)
            }
            currentAppInstances.update { it + (devicePackageKey to entity.appInstance) }
        }

        // Store bodies separately
        val requestBodyKey = bodyStore.store(entity.callId, BodyType.REQUEST, entity.request.body)
        val responseBodyKey = entity.response?.let { response ->
            when (response) {
                is NetworkResponse.Success -> bodyStore.store(entity.callId, BodyType.RESPONSE, response.body)
                is NetworkResponse.Failure -> null
            }
        }

        val storedCall = StoredCall(
            entity = entity.stripBodies(),
            requestBodyKey = requestBodyKey,
            responseBodyKey = responseBodyKey,
        )

        mutex.withLock {
            calls.update { current ->
                val updated = LinkedHashMap(current)
                updated[entity.callId] = storedCall

                // Evict oldest calls if over limit
                evictOldestIfNeeded(updated, settings.maxStoredCalls)

                updated
            }
        }
    }

    override suspend fun update(entity: NetworkCallEntity) {
        mutex.withLock {
            val current = calls.value[entity.callId] ?: return

            // Update bodies in store
            val requestBodyKey = bodyStore.store(entity.callId, BodyType.REQUEST, entity.request.body)
            val responseBodyKey = entity.response?.let { response ->
                when (response) {
                    is NetworkResponse.Success -> bodyStore.store(entity.callId, BodyType.RESPONSE, response.body)
                    is NetworkResponse.Failure -> null
                }
            }

            val storedCall = StoredCall(
                entity = entity.stripBodies(),
                requestBodyKey = requestBodyKey,
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

    override suspend fun getByDeviceAndPackage(
        deviceId: String,
        packageName: String
    ): List<NetworkCallEntity> {
        return calls.value.values
            .filter { it.entity.deviceId == deviceId && it.entity.packageName == packageName }
            .map { it.hydrate() }
    }

    override fun observeByDeviceAndPackage(
        deviceId: String,
        packageName: String
    ): Flow<List<NetworkCallEntity>> {
        return calls.map { map ->
            map.values
                .filter { it.entity.deviceId == deviceId && it.entity.packageName == packageName }
                .map { it.hydrate() }
        }
    }

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

            toRemove.forEach { callId ->
                bodyStore.removeAll(callId)
            }

            calls.update { current ->
                val updated = LinkedHashMap(current)
                toRemove.forEach { updated.remove(it) }
                updated
            }
        }
    }

    override suspend fun deleteAll() {
        mutex.withLock {
            bodyStore.clear()
            calls.value = linkedMapOf()
        }
    }

    private suspend fun evictOldestIfNeeded(map: LinkedHashMap<String, StoredCall>, maxCalls: Int) {
        while (map.size > maxCalls) {
            val oldest = map.entries.firstOrNull() ?: break
            log.v { "Evicting oldest call: ${oldest.key}" }
            bodyStore.removeAll(oldest.key)
            map.remove(oldest.key)
        }
    }

    /**
     * Clears all calls for a specific device/package.
     * MUST be called while holding the mutex lock.
     */
    private suspend fun clearCallsForDevicePackageUnsafe(deviceId: String, packageName: String) {
        val toRemove = calls.value.values
            .filter { it.entity.deviceId == deviceId && it.entity.packageName == packageName }
            .map { it.entity.callId }

        toRemove.forEach { callId ->
            bodyStore.removeAll(callId)
        }

        calls.update { current ->
            val updated = LinkedHashMap(current)
            toRemove.forEach { updated.remove(it) }
            updated
        }
    }

    private fun NetworkCallEntity.stripBodies(): NetworkCallEntity {
        return copy(
            request = request.copy(body = null),
            response = when (val r = response) {
                is NetworkResponse.Success -> r.copy(body = null)
                is NetworkResponse.Failure -> r
                null -> null
            }
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
            }
        )
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
