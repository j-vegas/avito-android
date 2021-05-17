package com.avito.android.runner.devices.internal.kubernetes

import com.avito.android.Result
import com.avito.android.runner.devices.internal.FakeAndroidDebugBridge
import com.avito.android.runner.devices.internal.StubEmulatorsLogsReporter
import com.avito.android.runner.devices.model.ReservationData
import com.avito.android.runner.devices.model.stub
import com.avito.logger.StubLoggerFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import java.util.LinkedList
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
internal class KubernetesReservationClientTest {

    private val kubernetesApi = FakeKubernetesApi()
    private val dispatcher = TestCoroutineDispatcher()
    private fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) {
        dispatcher.runBlockingTest(block)
    }

    private fun client(dispatcher: CoroutineDispatcher = this.dispatcher): KubernetesReservationClient {
        return KubernetesReservationClient(
            androidDebugBridge = FakeAndroidDebugBridge(),
            kubernetesApi = kubernetesApi,
            emulatorsLogsReporter = StubEmulatorsLogsReporter,
            loggerFactory = StubLoggerFactory,
            reservationDeploymentFactory = FakeReservationDeploymentFactory(),
            dispatcher = dispatcher,
            podsQueryIntervalMs = 1L
        )
    }

    
    fun `empty reservations - throws exception`() {
        val client = client()
        val exception = assertThrows<IllegalArgumentException> {
            runBlockingTest {
                client.claim(emptyList(), this)
            }
        }
        assertThat(exception.message)
            .isEqualTo("Must have at least one reservation but empty")
    }

    
    fun `creating deployment fail - throws exception`() {
        val client = client()
        val message = "Failed to create deployment"
        kubernetesApi.createDeployment = { throw RuntimeException(message) }
        val exception = assertThrows<RuntimeException>(message = message) {
            runBlockingTest {
                client.claim(listOf(ReservationData.stub()), this)
            }
        }
        assertThat(exception.message)
            .isEqualTo(message)
    }

    
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun `creating second deployment fail - throws exception`() {
        val client = client(dispatcher = Dispatchers.Default)
        val message = "Failed to create deployment"
        val results = LinkedList(
            listOf(
                { /*success*/ },
                { throw RuntimeException(message) }
            ),
        )
        kubernetesApi.createDeployment = { results.poll().invoke() }
        kubernetesApi.getPods = { Result.Success(listOf(StubPod())) }
        val exception = assertThrows<RuntimeException>(message = message) {
            runBlocking {
                client.claim(
                    listOf(
                        ReservationData.stub(),
                        ReservationData.stub()
                    ),
                    this
                )
            }
        }
        assertThat(exception.message)
            .isEqualTo(message)
    }

    
    fun `claim twice - throws exception`() {
        val client = client()
        val exception = assertThrows<CancellationException> {
            runBlockingTest {
                // first
                client.claim(listOf(ReservationData.stub()), this)
                // second
                client.claim(listOf(ReservationData.stub()), this)
            }
        }
        assertThat(exception.message)
            .isEqualTo("Unable claim reservation. State is already started")
    }

    
    fun `get one pod then fail - success`() {
        val client = client()
        val results = LinkedList(
            listOf(
                Result.Success(listOf(StubPod())),
                Result.Failure(RuntimeException())
            ),
        )

        kubernetesApi.getPods = {
            results.poll()
        }
        runBlockingTest {
            client.claim(listOf(ReservationData.stub()), this)
        }
    }

    
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun `devices channel cancel - success`() {
        val client = client(dispatcher = Dispatchers.Default)
        kubernetesApi.getPods = {
            Result.Success(listOf(StubPod()))
        }
        runBlocking {
            val result = client.claim(listOf(ReservationData.stub()), this)
            result.deviceCoordinates.cancel()
        }
    }

    
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun `claim then release - success`() {
        val client = client(dispatcher = Dispatchers.Default)
        kubernetesApi.getPods = {
            Result.Success(listOf(StubPod()))
        }
        runBlocking {
            client.claim(listOf(ReservationData.stub()), this)
            delay(100) // wait inner parts of `claim` fun
            client.release()
        }
    }
}
