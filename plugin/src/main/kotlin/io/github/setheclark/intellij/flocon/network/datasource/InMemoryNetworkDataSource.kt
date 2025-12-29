package io.github.setheclark.intellij.flocon.network.datasource

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Inject
@SingleIn(AppScope::class)
class InMemoryNetworkDataSource : NetworkDataSource {

    private val mutex = Mutex()
    private val calls = MutableStateFlow<Map<String, NetworkCallEntity>>(emptyMap())

    override suspend fun insert(entity: NetworkCallEntity) {
        mutex.withLock {
            calls.update { current -> current + (entity.callId to entity) }
        }
    }

    override suspend fun update(entity: NetworkCallEntity) {
        mutex.withLock {
            calls.update { current ->
                if (current.containsKey(entity.callId)) {
                    current + (entity.callId to entity)
                } else {
                    current
                }
            }
        }
    }

    override suspend fun getByCallId(callId: String): NetworkCallEntity? {
        return calls.value[callId]
    }

    override fun observeByCallId(callId: String): Flow<NetworkCallEntity?> {
        return calls.map { it[callId] }
    }

    override suspend fun getByDeviceAndPackage(
        deviceId: String,
        packageName: String
    ): List<NetworkCallEntity> {
        return calls.value.values.filter {
            it.deviceId == deviceId && it.packageName == packageName
        }
    }

    override fun observeByDeviceAndPackage(
        deviceId: String,
        packageName: String
    ): Flow<List<NetworkCallEntity>> {
        return calls.map { map ->
            map.values.filter {
                it.deviceId == deviceId && it.packageName == packageName
            }
        }
    }

    override suspend fun deleteByCallId(callId: String) {
        mutex.withLock {
            calls.update { current -> current - callId }
        }
    }

    override suspend fun deleteByDeviceAndPackage(deviceId: String, packageName: String) {
        mutex.withLock {
            calls.update { current ->
                current.filterValues {
                    it.deviceId != deviceId || it.packageName != packageName
                }
            }
        }
    }

    override suspend fun deleteAll() {
        mutex.withLock {
            calls.value = emptyMap()
        }
    }
}
