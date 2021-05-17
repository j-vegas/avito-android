package com.avito.report

import com.avito.http.HttpClientProvider
import com.avito.http.createStubInstance
import com.avito.logger.StubLoggerFactory
import com.avito.report.model.ReportCoordinates
import com.avito.test.http.MockWebServerFactory
import com.avito.truth.ResultSubject.Companion.assertThat
import com.avito.utils.fileFromJarResources
import com.github.salomonbrys.kotson.jsonObject
import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class ReportsApiTest {

    private val mockWebServer = MockWebServerFactory.create()

    private val loggerFactory = StubLoggerFactory


    fun `getReport - returns NotFound - when throws exception with no data`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody(
                    "{\"jsonrpc\":\"2.0\"," +
                        "\"error\":{\"code\":-32603,\"message\":\"Internal error\",\"data\":\"not found\"}," +
                        "\"id\":1}"
                )
        )

        val result = createNoRetriesReportsApi().getReport(
            ReportCoordinates("AvitoAndroid", "FunctionalTests", "12345")
        )

        assertThat(result).isFailure()
    }


    fun `getReport - returns Error - when throws exception with no data`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = createNoRetriesReportsApi().getReport(
            ReportCoordinates("AvitoAndroid", "FunctionalTests", "12345")
        )

        assertThat(result).isFailure()
    }


    fun `getReport - returns Report`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody(fileFromJarResources<ReportsApiTest>("getReport.json").readText())
        )

        val result = createNoRetriesReportsApi().getReport(ReportCoordinates("AvitoAndroid", "FunctionalTests", ""))

        assertThat(result).isSuccess().withValue {
            assertThat(it.id).isEqualTo("5c8032d5ccdf780001c49576")
        }
    }


    fun `getTestsForRunId - returns ok`() {
        mockWebServer.enqueue(
            MockResponse().setBody(fileFromJarResources<ReportsApiTest>("getReport.json").readText())
        )
        mockWebServer.enqueue(
            MockResponse().setBody(fileFromJarResources<ReportsApiTest>("getTestsForRunId.json").readText())
        )

        val result = createNoRetriesReportsApi().getTestsForRunId(
            ReportCoordinates("AvitoAndroid", "FunctionalTests", "")
        )

        assertThat(result).isSuccess().withValue {
            assertThat(it.first().name).isEqualTo("ru.domofond.features.RemoteToggleMonitorTest.check_remote_toggle")
        }
    }


    fun `pushPreparedData - returns ok`() {
        mockWebServer.enqueue(
            MockResponse().setBody(fileFromJarResources<ReportsApiTest>("pushPreparedData.json").readText())
        )

        val result = createNoRetriesReportsApi().pushPreparedData("any", "any", jsonObject("any" to "any"))

        assertThat(result).isSuccess()
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    private fun createNoRetriesReportsApi(): ReportsApi = ReportsApiFactory.create(
        host = mockWebServer.url("/").toString(),
        loggerFactory = loggerFactory,
        httpClientProvider = HttpClientProvider.createStubInstance(),
        retryRequests = false
    )
}
