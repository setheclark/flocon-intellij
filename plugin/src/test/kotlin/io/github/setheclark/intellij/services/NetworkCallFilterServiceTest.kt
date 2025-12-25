package io.github.setheclark.intellij.services

import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.domain.models.StatusFilter
import io.github.setheclark.intellij.domain.models.StatusFilter.ALL
import io.github.setheclark.intellij.domain.models.StatusFilter.CLIENT_ERROR
import io.github.setheclark.intellij.domain.models.StatusFilter.ERROR
import io.github.setheclark.intellij.domain.models.StatusFilter.REDIRECT
import io.github.setheclark.intellij.domain.models.StatusFilter.SERVER_ERROR
import io.github.setheclark.intellij.domain.models.StatusFilter.SUCCESS
import io.github.setheclark.intellij.fixtures.NetworkCallFactory
import io.github.setheclark.intellij.ui.list.applyFilter
import io.github.setheclark.intellij.ui.list.matchesSearchText
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NetworkCallFilterServiceTest {

    @Nested
    inner class ApplyFilter {

        @Test
        fun `empty filter returns all calls`() {
            val calls = NetworkCallFactory.createEntries(5)
            val filter = NetworkFilter()

            val result = calls.applyFilter(filter)

            result shouldHaveSize 5
        }

        @Test
        fun `empty list returns empty result`() {
            val filter = NetworkFilter()

            val result = emptyList<NetworkCallEntry>().applyFilter(filter)

            result.shouldBeEmpty()
        }

        @Nested
        inner class SearchTextFilter {

            @Test
            fun `filters by URL`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.mysite.com/users",
                        packageName = "com.app"
                    ),
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.mysite.com/orders",
                        packageName = "com.app"
                    ),
                    NetworkCallFactory.createCompletedEntry(url = "https://other.com/data", packageName = "com.app")
                )
                val filter = NetworkFilter(searchText = "mysite")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 2
            }

            @Test
            fun `search is case insensitive`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://API.MYSITE.COM/users",
                        packageName = "com.app"
                    ),
                    NetworkCallFactory.createCompletedEntry(url = "https://other.com/data", packageName = "com.app")
                )
                val filter = NetworkFilter(searchText = "mysite")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
            }

            @Test
            fun `filters by method`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(method = "GET"),
                    NetworkCallFactory.createCompletedEntry(method = "POST"),
                    NetworkCallFactory.createCompletedEntry(method = "DELETE")
                )
                val filter = NetworkFilter(searchText = "post")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
                result[0].request.method shouldBe "POST"
            }

            @Test
            fun `filters by request body`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(requestBody = """{"username": "testuser"}"""),
                    NetworkCallFactory.createCompletedEntry(requestBody = """{"email": "test@test.com"}"""),
                    NetworkCallFactory.createCompletedEntry(requestBody = null)
                )
                val filter = NetworkFilter(searchText = "testuser")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
            }

            @Test
            fun `filters by response body`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(responseBody = """{"data": "important"}"""),
                    NetworkCallFactory.createCompletedEntry(responseBody = """{"error": "not found"}""")
                )
                val filter = NetworkFilter(searchText = "important")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
            }

            @Test
            fun `filters by status code`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(statusCode = 200),
                    NetworkCallFactory.createCompletedEntry(statusCode = 404),
                    NetworkCallFactory.createCompletedEntry(statusCode = 500)
                )
                val filter = NetworkFilter(searchText = "404")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
                result[0].response?.statusCode shouldBe 404
            }

            @Test
            fun `filters by device ID`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(deviceId = "device-abc"),
                    NetworkCallFactory.createCompletedEntry(deviceId = "device-xyz")
                )
                val filter = NetworkFilter(searchText = "abc")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
                result[0].deviceId shouldBe "device-abc"
            }

            @Test
            fun `filters by package name`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.mysite.com/test",
                        packageName = "com.mypackage.app"
                    ),
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.mysite.com/test",
                        packageName = "com.other.app"
                    )
                )
                val filter = NetworkFilter(searchText = "mypackage")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
                result[0].packageName shouldBe "com.mypackage.app"
            }
        }

        @Nested
        inner class MethodFilter {

            @Test
            fun `filters by exact method match`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(method = "GET"),
                    NetworkCallFactory.createCompletedEntry(method = "POST"),
                    NetworkCallFactory.createCompletedEntry(method = "GET")
                )
                val filter = NetworkFilter(methodFilter = "GET")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 2
                result.all { it.request.method == "GET" } shouldBe true
            }

            @Test
            fun `method filter is case insensitive`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(method = "GET"),
                    NetworkCallFactory.createCompletedEntry(method = "post")
                )
                val filter = NetworkFilter(methodFilter = "get")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
            }

            @Test
            fun `null method filter returns all`() {
                val calls = NetworkCallFactory.createEntries(5)
                val filter = NetworkFilter(methodFilter = null)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 5
            }
        }

        @Nested
        inner class StatusFiltering {

            @Test
            fun `ALL filter returns all calls`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(statusCode = 200),
                    NetworkCallFactory.createCompletedEntry(statusCode = 301),
                    NetworkCallFactory.createCompletedEntry(statusCode = 404),
                    NetworkCallFactory.createCompletedEntry(statusCode = 500)
                )
                val filter = NetworkFilter(statusFilter = ALL)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 4
            }

            @Test
            fun `SUCCESS filter returns only 2xx status codes`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(statusCode = 200),
                    NetworkCallFactory.createCompletedEntry(statusCode = 201),
                    NetworkCallFactory.createCompletedEntry(statusCode = 299),
                    NetworkCallFactory.createCompletedEntry(statusCode = 301),
                    NetworkCallFactory.createCompletedEntry(statusCode = 404)
                )
                val filter = NetworkFilter(statusFilter = SUCCESS)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 3
                result.all { it.response?.statusCode in 200..299 } shouldBe true
            }

            @Test
            fun `REDIRECT filter returns only 3xx status codes`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(statusCode = 200),
                    NetworkCallFactory.createCompletedEntry(statusCode = 301),
                    NetworkCallFactory.createCompletedEntry(statusCode = 302),
                    NetworkCallFactory.createCompletedEntry(statusCode = 404)
                )
                val filter = NetworkFilter(statusFilter = REDIRECT)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 2
                result.all { it.response?.statusCode in 300..399 } shouldBe true
            }

            @Test
            fun `CLIENT_ERROR filter returns only 4xx status codes`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(statusCode = 200),
                    NetworkCallFactory.createCompletedEntry(statusCode = 400),
                    NetworkCallFactory.createCompletedEntry(statusCode = 404),
                    NetworkCallFactory.createCompletedEntry(statusCode = 500)
                )
                val filter = NetworkFilter(statusFilter = CLIENT_ERROR)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 2
                result.all { it.response?.statusCode in 400..499 } shouldBe true
            }

            @Test
            fun `SERVER_ERROR filter returns only 5xx status codes`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(statusCode = 200),
                    NetworkCallFactory.createCompletedEntry(statusCode = 404),
                    NetworkCallFactory.createCompletedEntry(statusCode = 500),
                    NetworkCallFactory.createCompletedEntry(statusCode = 503)
                )
                val filter = NetworkFilter(statusFilter = SERVER_ERROR)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 2
                result.all { it.response?.statusCode in 500..599 } shouldBe true
            }

            @Test
            fun `ERROR filter returns only calls with errors`() {
                val errorResponse = NetworkCallFactory.createResponse(error = "Connection refused")
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(statusCode = 200),
                    NetworkCallFactory.createEntry(
                        response = errorResponse
                    )
                )
                val filter = NetworkFilter(statusFilter = ERROR)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
                result[0].response?.error shouldBe "Connection refused"
            }
        }

        @Nested
        inner class DeviceFilter {

            @Test
            fun `filters by device ID`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(deviceId = "device-1"),
                    NetworkCallFactory.createCompletedEntry(deviceId = "device-2"),
                    NetworkCallFactory.createCompletedEntry(deviceId = "device-1")
                )
                val filter = NetworkFilter(deviceFilter = "device-1")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 2
                result.all { it.deviceId == "device-1" } shouldBe true
            }

            @Test
            fun `null device filter returns all`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(deviceId = "device-1"),
                    NetworkCallFactory.createCompletedEntry(deviceId = "device-2")
                )
                val filter = NetworkFilter(deviceFilter = null)

                val result = calls.applyFilter(filter)

                result shouldHaveSize 2
            }
        }

        @Nested
        inner class CombinedFilters {

            @Test
            fun `combines search text and method filter`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.mysite.com/users",
                        method = "GET",
                        packageName = "com.app"
                    ),
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.mysite.com/users",
                        method = "POST",
                        packageName = "com.app"
                    ),
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://other.com/users",
                        method = "GET",
                        packageName = "com.app"
                    )
                )
                val filter = NetworkFilter(searchText = "mysite", methodFilter = "GET")

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
                result[0].request.url shouldBe "https://api.mysite.com/users"
                result[0].request.method shouldBe "GET"
            }

            @Test
            fun `combines all filters`() {
                val calls = listOf(
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.example.com/users",
                        method = "GET",
                        statusCode = 200,
                        deviceId = "device-1"
                    ),
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.example.com/users",
                        method = "GET",
                        statusCode = 200,
                        deviceId = "device-2"
                    ),
                    NetworkCallFactory.createCompletedEntry(
                        url = "https://api.example.com/orders",
                        method = "POST",
                        statusCode = 200,
                        deviceId = "device-1"
                    )
                )
                val filter = NetworkFilter(
                    searchText = "users",
                    methodFilter = "GET",
                    statusFilter = SUCCESS,
                    deviceFilter = "device-1"
                )

                val result = calls.applyFilter(filter)

                result shouldHaveSize 1
                result[0].request.url shouldBe "https://api.example.com/users"
                result[0].deviceId shouldBe "device-1"
            }
        }
    }

    @Nested
    inner class MatchesSearchText {

        @Test
        fun `matches URL`() {
            val call = NetworkCallFactory.createCompletedEntry(url = "https://api.example.com/users")

            call.matchesSearchText("example") shouldBe true
            call.matchesSearchText("other") shouldBe false
        }

        @Test
        fun `matches request headers`() {
            val request = NetworkCallFactory.createRequest(
                headers = mapOf("Authorization" to "Bearer token123")
            )
            val call = NetworkCallFactory.createEntry(request = request)

            call.matchesSearchText("authorization") shouldBe true
            call.matchesSearchText("token123") shouldBe true
            call.matchesSearchText("notfound") shouldBe false
        }

        @Test
        fun `matches response headers`() {
            val response = NetworkCallFactory.createResponse(
                headers = mapOf("X-Custom-Header" to "custom-value")
            )
            val call = NetworkCallFactory.createEntry(response = response)

            call.matchesSearchText("x-custom") shouldBe true
            call.matchesSearchText("custom-value") shouldBe true
        }

        @Test
        fun `matches content type`() {
            val request = NetworkCallFactory.createRequest(contentType = "application/json")
            val call = NetworkCallFactory.createEntry(request = request)

            call.matchesSearchText("json") shouldBe true
        }

        @Test
        fun `handles null response gracefully`() {
            val request = NetworkCallFactory.createRequest(url = "https://api.mysite.com/endpoint")
            val call = NetworkCallFactory.createEntry(
                request = request,
                response = null,
                packageName = "com.app"
            )

            call.matchesSearchText("notfound") shouldBe false
        }
    }
}
