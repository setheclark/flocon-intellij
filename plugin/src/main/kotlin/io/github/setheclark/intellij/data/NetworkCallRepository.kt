package io.github.setheclark.intellij.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing network call storage and retrieval.
 */
interface NetworkCallRepository {
    val networkCalls: StateFlow<List<NetworkCallEntry>>

    fun addCall(call: NetworkCallEntry)
    fun updateCall(callId: String, update: (NetworkCallEntry) -> NetworkCallEntry)
    fun getCall(callId: String): NetworkCallEntry?
    fun clear()
}

@Inject
@SingleIn(AppScope::class)
class NetworkCallRepositoryImpl : NetworkCallRepository {

    private val _networkCalls = MutableStateFlow<List<NetworkCallEntry>>(emptyList())
    override val networkCalls: StateFlow<List<NetworkCallEntry>> = _networkCalls.asStateFlow()

    override fun addCall(call: NetworkCallEntry) {
        _networkCalls.value = _networkCalls.value + call
    }

    override fun updateCall(callId: String, update: (NetworkCallEntry) -> NetworkCallEntry) {
        _networkCalls.value = _networkCalls.value.map { call ->
            if (call.id == callId) update(call) else call
        }
    }

    override fun getCall(callId: String): NetworkCallEntry? {
        return _networkCalls.value.find { it.id == callId }
    }

    override fun clear() {
        _networkCalls.value = emptyList()
    }
}
