package io.github.setheclark.intellij.flocon.network

import androidx.paging.PagingData
import app.cash.sqldelight.Query
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.network.datasource.NetworkLocalDataSource
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.network.models.FloconNetworkCallDomainModel
import io.github.openflocon.domain.network.models.NetworkFilterDomainModel
import io.github.openflocon.domain.network.models.NetworkSortDomainModel
import io.github.setheclark.intellij.NetworkCallEntity
import io.github.setheclark.intellij.NetworkCallEntityQueries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Inject
class NetworkLocalDataSourceImpl(
    private val dispatcherProvider: DispatcherProvider,
    private val networkCallEntityQueries: NetworkCallEntityQueries
) : NetworkLocalDataSource {

    private val log = Logger.withTag(">>> NetworkLocalDataSource")

    override suspend fun getRequests(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel
    ): List<FloconNetworkCallDomainModel> {
        return networkCallEntityQueries
            .selectAll(
                deviceIdAndPackageName = deviceIdAndPackageName,
                sortedBy = sortedBy,
                filter = filter,
            )
            .executeAsList()
            .map { it.toModel() }
    }

    override fun observeRequests(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel
    ): Flow<PagingData<FloconNetworkCallDomainModel>> {
        log.w { "no-op:observeRequests: $deviceIdAndPackageName" }
        return flowOf(PagingData.empty())
    }

    override suspend fun getCalls(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        ids: List<String>
    ): List<FloconNetworkCallDomainModel> {
        log.w { "no-op:getCalls: $deviceIdAndPackageName|$ids" }
        return emptyList()
    }

    override suspend fun getCall(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ): FloconNetworkCallDomainModel? {
        log.w { "no-op:getCall: $deviceIdAndPackageName|$callId" }
        return null
    }

    override fun observeCall(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ): Flow<FloconNetworkCallDomainModel?> {
        log.w { "no-op:observeCall: $deviceIdAndPackageName|$callId" }
        return flowOf(null)
    }

    override suspend fun save(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        call: FloconNetworkCallDomainModel
    ) {
        log.w { "no-op:save: $deviceIdAndPackageName|$call" }
    }

    override suspend fun save(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        calls: List<FloconNetworkCallDomainModel>
    ) {
        log.w { "no-op:save: $deviceIdAndPackageName|$calls" }
    }

    override suspend fun clearDeviceCalls(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        log.w { "no-op:clearDeviceCalls: $deviceIdAndPackageName" }
    }

    override suspend fun deleteRequest(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ) {
        log.w { "no-op:deleteRequest: $deviceIdAndPackageName|$callId" }
    }

    override suspend fun deleteRequestsBefore(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ) {
        log.w { "no-op:deleteRequestsBefore: $deviceIdAndPackageName|$callId" }
    }

    override suspend fun deleteRequestOnDifferentSession(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        log.w { "no-op:deleteRequestOnDifferentSession: $deviceIdAndPackageName" }
    }

    override suspend fun clear() {
        log.w { "no-op:clear" }
    }

    private fun NetworkCallEntity.toModel(): FloconNetworkCallDomainModel {
        return FloconNetworkCallDomainModel(
            callId = callId,
            appInstance = appInstance,
            isReplayed = isReplayed,
            request = toRequestModel(),
            response = toResponseModel(),
        )
    }

    private fun NetworkCallEntityQueries.selectAll(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel
    ): Query<NetworkCallEntity> {
        return selectAll(
            filterText = filter.filterOnAllColumns?.let { "%it%" },
            sort = if (sortedBy?.asc == true) "START_ASC" else "START_DESC",
            appInstance = deviceIdAndPackageName.appInstance.takeUnless { filter.displayOldSessions }
        )
    }

    private fun NetworkCallEntity.toRequestModel(): FloconNetworkCallDomainModel.Request {
        return FloconNetworkCallDomainModel.Request(
            url = request_url,
            method = request_method,
            startTime = request_startTime,
            headers = request_headers,
            body = request_body,
            byteSize = request_byteSize,
            isMocked = request_isMocked,
            startTimeFormatted = request_startTimeFormatted,
            byteSizeFormatted = request_byteSizeFormatted,
            domainFormatted = request_domainFormatted,
            queryFormatted = request_queryFormatted,
            methodFormatted = request_methodFormatted,
            specificInfos = when (type) {
                "GRAPHQL" -> FloconNetworkCallDomainModel.Request.SpecificInfos.GraphQl(
                    query = request_graphql_query.orEmpty(),
                    operationType = request_graphql_operationType.orEmpty(),
                )

                "GRPC" -> FloconNetworkCallDomainModel.Request.SpecificInfos.Grpc
                "WEBSOCKET" -> FloconNetworkCallDomainModel.Request.SpecificInfos.WebSocket(
                    event = request_websocket_event ?: "unknown",
                )

                "HTTP" -> FloconNetworkCallDomainModel.Request.SpecificInfos.Http
                // TODO Log?
                else -> FloconNetworkCallDomainModel.Request.SpecificInfos.Http
            }
        )
    }

    private fun NetworkCallEntity.toResponseModel(): FloconNetworkCallDomainModel.Response? {
        // Assuming durationMs will be non-null for any non-null response...
        return response_durationMs?.let {
            if (response_error != null) {
                FloconNetworkCallDomainModel.Response.Failure(
                    durationMs = it,
                    durationFormatted = response_durationFormatted.orEmpty(),
                    issue = response_error,
                    statusFormatted = response_statusFormatted.orEmpty(),
                )
            } else {
                FloconNetworkCallDomainModel.Response.Success(
                    contentType = response_contentType.orEmpty(),
                    body = response_body.orEmpty(),
                    headers = response_headers.orEmpty(),
                    byteSize = response_byteSize ?: 0L,
                    durationMs = it,
                    durationFormatted = response_durationFormatted.orEmpty(),
                    byteSizeFormatted = response_byteSizeFormatted.orEmpty(),
                    isImage = response_isImage == true,
                    statusFormatted = response_statusFormatted.orEmpty(),
                    specificInfos = when {
                        response_graphql_isSuccess != null -> {
                            FloconNetworkCallDomainModel.Response.Success.SpecificInfos.GraphQl(
                                httpCode = response_graphql_httpCode?.toInt() ?: -1,
                                isSuccess = response_graphql_isSuccess,
                            )
                        }

                        response_grpc_status != null -> {
                            FloconNetworkCallDomainModel.Response.Success.SpecificInfos.Grpc(
                                grpcStatus = response_grpc_status,
                            )
                        }

                        response_http_httpCode != null -> {
                            FloconNetworkCallDomainModel.Response.Success.SpecificInfos.Http(
                                httpCode = response_http_httpCode.toInt()
                            )
                        }

                        else -> return null
                    }
                )
            }

        }
    }
}