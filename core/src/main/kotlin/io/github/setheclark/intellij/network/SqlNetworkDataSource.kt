package io.github.setheclark.intellij.network

import app.cash.sqldelight.coroutines.asFlow
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.network.db.NetworkHistoryDatabase
import io.github.setheclark.intellij.network.db.Network_call
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * [NetworkDataSource] backed by the SQLite history database.
 *
 * Bodies are stored as plain text in their own table, in full, and are deleted together with their call.
 * Lists of calls never carry bodies; [getByCallId] and [observeByCallId] load them.
 */
class SqlNetworkDataSource internal constructor(
    database: NetworkHistoryDatabase,
    private val json: Json,
    private val dispatcher: CoroutineDispatcher,
) : NetworkDataSource {

    private val queries = database.networkCallQueries

    // SQLite allows one writer at a time. Serializing them here avoids waiting on the busy timeout.
    private val writeMutex = Mutex()

    override fun observeCurrentAppInstance(deviceId: String, packageName: String): Flow<String?> =
        queries.selectCurrentAppInstance(deviceId, packageName)
            .asFlow()
            .conflate()
            .map { query -> withContext(dispatcher) { query.executeAsOneOrNull() } }
            .distinctUntilChanged()

    override suspend fun insert(entity: NetworkCallEntity) {
        withContext(dispatcher) {
            val requestBody = entity.request.body.takeUnless { it.isNullOrEmpty() }
            val responseBody = (entity.response as? NetworkResponse.Success)?.body.takeUnless { it.isNullOrEmpty() }

            writeMutex.withLock {
                queries.transaction {
                    entity.insertRow()
                    requestBody?.let { queries.insertBody(entity.callId, BODY_REQUEST, it) }
                    responseBody?.let { queries.insertBody(entity.callId, BODY_RESPONSE, it) }
                }
            }
        }
    }

    override suspend fun update(entity: NetworkCallEntity) {
        withContext(dispatcher) {
            val responseBody = (entity.response as? NetworkResponse.Success)?.body.takeUnless { it.isNullOrEmpty() }

            writeMutex.withLock {
                queries.transaction {
                    if (!queries.callExists(entity.callId).executeAsOne()) return@transaction
                    entity.updateResponseRow()
                    queries.deleteBody(entity.callId, BODY_RESPONSE)
                    responseBody?.let { queries.insertBody(entity.callId, BODY_RESPONSE, it) }
                }
            }
        }
    }

    override suspend fun getByCallId(callId: String): NetworkCallEntity? = withContext(dispatcher) {
        queries.selectCall(callId).executeAsOneOrNull()?.toEntity(withBodies = true)
    }

    override fun observeByCallId(callId: String): Flow<NetworkCallEntity?> =
        queries.selectCall(callId)
            .asFlow()
            .conflate()
            .map { query -> withContext(dispatcher) { query.executeAsOneOrNull()?.toEntity(withBodies = true) } }

    override fun observeCurrentSessionCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>> =
        observeCurrentAppInstance(deviceId, packageName).flatMapLatest { appInstance ->
            if (appInstance == null) {
                flowOf(emptyList())
            } else {
                queries.selectCallsForSession(deviceId, packageName, appInstance)
                    .asFlow()
                    .conflate()
                    .map { query ->
                        withContext(dispatcher) { query.executeAsList().map { it.toEntity(withBodies = false) } }
                    }
            }
        }

    override suspend fun searchBodies(deviceId: String, packageName: String, text: String): Set<String> =
        withContext(dispatcher) {
            val appInstance = queries.selectCurrentAppInstance(deviceId, packageName).executeAsOneOrNull()
                ?: return@withContext emptySet()
            queries.searchBodies(deviceId, packageName, appInstance, text).executeAsList().toSet()
        }

    override suspend fun deleteByCallId(callId: String) {
        withContext(dispatcher) {
            writeMutex.withLock { queries.deleteCall(callId) }
        }
    }

    override suspend fun deleteByDeviceAndPackage(deviceId: String, packageName: String) {
        withContext(dispatcher) {
            writeMutex.withLock { queries.deleteCallsForDevicePackage(deviceId, packageName) }
        }
    }

    override suspend fun deleteAll() {
        withContext(dispatcher) {
            writeMutex.withLock { queries.deleteAllCalls() }
        }
    }

    // region mapping

    private fun NetworkCallEntity.insertRow() {
        val graphQl = request.type as? NetworkRequest.Type.GraphQl
        queries.insertCall(
            call_id = callId,
            device_id = deviceId,
            package_name = packageName,
            app_instance = appInstance,
            start_time = startTime,
            name = name,
            request_url = request.url,
            request_method = request.method,
            request_headers = encodeHeaders(request.headers),
            request_size = request.size,
            request_type = when (request.type) {
                NetworkRequest.Type.Http -> TYPE_HTTP
                is NetworkRequest.Type.GraphQl -> TYPE_GRAPHQL
                NetworkRequest.Type.Grpc -> TYPE_GRPC
            },
            graphql_persisted = graphQl?.persisted?.let { if (it) 1L else 0L },
            graphql_query = graphQl?.query,
            graphql_operation_name = graphQl?.operationName,
            graphql_operation_type = graphQl?.operationType,
            response_kind = response.kind(),
            response_duration_ms = response?.durationMs,
            response_headers = (response as? NetworkResponse.Success)?.let { encodeHeaders(it.headers) },
            response_size = (response as? NetworkResponse.Success)?.size,
            response_content_type = (response as? NetworkResponse.Success)?.contentType,
            response_status_code = (response as? NetworkResponse.Success)?.statusCode?.toLong(),
            response_issue = (response as? NetworkResponse.Failure)?.issue,
        )
    }

    private fun NetworkCallEntity.updateResponseRow() {
        val success = response as? NetworkResponse.Success
        queries.updateResponse(
            response_kind = response.kind(),
            response_duration_ms = response?.durationMs,
            response_headers = success?.let { encodeHeaders(it.headers) },
            response_size = success?.size,
            response_content_type = success?.contentType,
            response_status_code = success?.statusCode?.toLong(),
            response_issue = (response as? NetworkResponse.Failure)?.issue,
            call_id = callId,
        )
    }

    private fun NetworkResponse?.kind(): Long = when (this) {
        null -> RESPONSE_NONE
        is NetworkResponse.Success -> RESPONSE_SUCCESS
        is NetworkResponse.Failure -> RESPONSE_FAILURE
    }

    private fun Network_call.toEntity(withBodies: Boolean): NetworkCallEntity {
        val requestBody = if (withBodies) readBody(call_id, BODY_REQUEST) else null
        val responseBody = if (withBodies) readBody(call_id, BODY_RESPONSE) else null

        return NetworkCallEntity(
            callId = call_id,
            deviceId = device_id,
            packageName = package_name,
            appInstance = app_instance,
            startTime = start_time,
            name = name,
            request = NetworkRequest(
                url = request_url,
                method = request_method,
                headers = decodeHeaders(request_headers),
                body = requestBody,
                type = when (request_type) {
                    TYPE_GRAPHQL -> NetworkRequest.Type.GraphQl(
                        persisted = graphql_persisted == 1L,
                        query = graphql_query,
                        operationName = graphql_operation_name,
                        operationType = graphql_operation_type,
                    )

                    TYPE_GRPC -> NetworkRequest.Type.Grpc

                    else -> NetworkRequest.Type.Http
                },
                size = request_size,
            ),
            response = when (response_kind) {
                RESPONSE_SUCCESS -> NetworkResponse.Success(
                    durationMs = response_duration_ms ?: 0.0,
                    body = responseBody,
                    headers = response_headers?.let(::decodeHeaders).orEmpty(),
                    size = response_size,
                    contentType = response_content_type,
                    statusCode = response_status_code?.toInt(),
                )

                RESPONSE_FAILURE -> NetworkResponse.Failure(
                    durationMs = response_duration_ms ?: 0.0,
                    issue = response_issue.orEmpty(),
                )

                else -> null
            },
        )
    }

    private fun readBody(callId: String, kind: Long): String? =
        queries.selectBody(callId, kind).executeAsOneOrNull()

    private fun encodeHeaders(headers: Map<String, String>): String = json.encodeToString(headersSerializer, headers)

    private fun decodeHeaders(encoded: String): Map<String, String> = json.decodeFromString(headersSerializer, encoded)

    // endregion

    private companion object {
        const val BODY_REQUEST = 0L
        const val BODY_RESPONSE = 1L

        const val RESPONSE_NONE = 0L
        const val RESPONSE_SUCCESS = 1L
        const val RESPONSE_FAILURE = 2L

        const val TYPE_HTTP = "http"
        const val TYPE_GRAPHQL = "graphql"
        const val TYPE_GRPC = "grpc"

        val headersSerializer = MapSerializer(String.serializer(), String.serializer())
    }
}
