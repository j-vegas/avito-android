package com.avito.android.runner.devices.internal.kubernetes

import com.avito.android.Result
import com.avito.android.runner.devices.internal.FakeAndroidDebugBridge
import com.avito.android.runner.devices.internal.FakeRemoteDevice
import com.avito.android.runner.devices.internal.StubEmulatorsLogsReporter
import com.avito.android.runner.devices.model.ReservationData
import com.avito.android.runner.devices.model.stub
import com.avito.k8s.StubKubernetesApi
import com.avito.k8s.model.KubePod
import com.avito.k8s.model.createStubInstance
import com.avito.logger.PrintlnLoggerFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import java.util.LinkedList
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
internal class KubernetesReservationClientTest {

    private val kubernetesApi = StubKubernetesApi()
    private val androidDebugBridge = FakeAndroidDebugBridge()
    private val dispatcher = TestCoroutineDispatcher()
    private val podsQueryInterval = 1L
    private fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) {
        dispatcher.runBlockingTest(block)
    }

    private fun client(
        dispatcher: CoroutineDispatcher = this.dispatcher,
    ): KubernetesReservationClient {
        return KubernetesReservationClientFactory(
            kubernetesApi = kubernetesApi,
            reservationDeploymentFactory = FakeReservationDeploymentFactory(),
            emulatorsLogsReporter = StubEmulatorsLogsReporter,
            deviceProvider = RemoteDeviceProviderImpl(
                kubernetesApi,
                StubEmulatorsLogsReporter,
                androidDebugBridge,
                PrintlnLoggerFactory
            ),
            listener = StubKubernetesReservationListener,
            loggerFactory = PrintlnLoggerFactory,
            podsQueryIntervalMs = podsQueryInterval,
            dispatcher = dispatcher,
            deviceSignals = deviceSignals
        ).create()
    }

    @Test
    fun `empty reservations - throws exception`() {
        val client = client()
        val exception = assertThrows<IllegalArgumentException> {
            runBlockingTest {
                client.claim(emptyList())
            }
        }
        assertThat(exception.message)
            .isEqualTo("Must have at least one reservation but empty")
    }

    @Test
    fun `creating deployment fail - throws exception`() {
        val client = client()
        val message = "Failed to create deployment"
        kubernetesApi.createDeployment = { throw RuntimeException(message) }
        val exception = assertThrows<RuntimeException>(message = message) {
            runBlockingTest {
                client.claim(listOf(ReservationData.stub()))
            }
        }
        assertThat(exception.message)
            .isEqualTo(message)
    }

    @Test
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
        kubernetesApi.getPods = { Result.Success(listOf(KubePod.createStubInstance())) }
        val exception = assertThrows<RuntimeException>(message = message) {
            runBlocking {
                client.claim(
                    listOf(
                        ReservationData.stub(),
                        ReservationData.stub()
                    )
                )
            }
        }
        assertThat(exception.message)
            .isEqualTo(message)
    }

    @Test
    fun `claim twice - throws exception`() {
        val client = client()
        val exception = assertThrows<IllegalStateException> {
            runBlockingTest {
                // first
                client.claim(listOf(ReservationData.stub()))
                // second
                client.claim(listOf(ReservationData.stub()))
            }
        }
        assertThat(exception.message)
            .isEqualTo("Unable claim reservation. State is already started")
    }

    @Test
    fun `get one pod then fail - success`() {
        val client = client()
        val results = LinkedList(
            listOf(
                Result.Success(listOf(KubePod.createStubInstance())),
                Result.Failure(RuntimeException())
            ),
        )

        kubernetesApi.getPods = {
            results.poll()
        }
        runBlockingTest {
            client.claim(listOf(ReservationData.stub()))
        }
    }

    @Test
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun `devices channel cancel - success`() {
        val client = client(dispatcher = Dispatchers.Default)
        kubernetesApi.getPods = {
            Result.Success(listOf(KubePod.createStubInstance()))
        }
        runBlocking {
            val result = client.claim(listOf(ReservationData.stub()))
            result.deviceCoordinates.cancel()
        }
    }

    @Disabled("Flaky")
    @Test
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun `claim then release - success`() {
        val client = client(dispatcher = Dispatchers.Default)
        kubernetesApi.getPods = {
            Result.Success(listOf(KubePod.createStubInstance()))
        }
        runBlocking {
            client.claim(listOf(ReservationData.stub()))
            delay(100) // wait inner parts of `claim` fun
            client.release()
        }
    }

    @Test
    fun `can't boot device and then delete device fail - throws exception`() {
        androidDebugBridge.remoteDeviceProvider = { serial ->
            FakeRemoteDevice(serial).also {
                it.waitForBoot = { Result.Failure(RuntimeException("Wait for boot failed")) }
            }
        }
        kubernetesApi.getPods = { Result.Success(listOf(KubePod.createStubInstance())) }
        val expectedMessage = "Can't delete pod"
        kubernetesApi.deletePod = {
            throw RuntimeException(expectedMessage)
        }
        val client = client(dispatcher = Dispatchers.Default)
        val exception = assertThrows<RuntimeException> {
            runBlocking {
                client.claim(listOf(ReservationData.stub()))
            }
        }
        assertThat(exception.message)
            .isEqualTo(expectedMessage)
    }

    @Test
    fun `first pod hasn't ip - success`() {
        val client = client()
        val results = LinkedList(
            listOf(
                Result.Success(listOf(KubePod.createStubInstance(name = "stub-1", ip = null))),
                Result.Success(listOf(KubePod.createStubInstance(name = "stub-2"))),
            ),
        )

        kubernetesApi.getPods = {
            results.poll()
        }
        runBlockingTest {
            val claim = client.claim(listOf(ReservationData.stub()))
            claim.deviceCoordinates.cancel() // stop client
        }
    }
}
