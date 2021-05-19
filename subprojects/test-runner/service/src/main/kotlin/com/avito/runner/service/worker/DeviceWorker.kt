package com.avito.runner.service.worker

import com.android.ddmlib.Log
import com.avito.android.Result
import com.avito.coroutines.extensions.Dispatchers
import com.avito.report.TestArtifactsProviderFactory
import com.avito.runner.service.IntentionsRouter
import com.avito.runner.service.listener.TestListener
import com.avito.runner.service.model.DeviceTestCaseRun
import com.avito.runner.service.model.TestCaseRun
import com.avito.runner.service.model.TestCaseRun.Result.Failed
import com.avito.runner.service.model.intention.InstrumentationTestRunAction
import com.avito.runner.service.model.intention.Intention
import com.avito.runner.service.model.intention.State
import com.avito.runner.service.worker.device.Device
import com.avito.runner.service.worker.device.Device.DeviceStatus.Alive
import com.avito.runner.service.worker.device.Device.DeviceStatus.Freeze
import com.avito.runner.service.worker.device.model.getData
import com.avito.runner.service.worker.listener.DeviceListener
import com.avito.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

internal class DeviceWorker(
    private val intentionsRouter: IntentionsRouter,
    private val device: Device,
    private val outputDirectory: File,
    private val testListener: TestListener,
    private val deviceListener: DeviceListener,
    private val timeProvider: TimeProvider,
    dispatchers: Dispatchers
) {

    private val dispatcher: CoroutineDispatcher = dispatchers.dispatcher()

    private val stateWorker: DeviceStateWorker = DeviceStateWorker(device)

    fun run(scope: CoroutineScope) = scope.launch(dispatcher + CoroutineName("device-worker")) {

        var state: State = when (val status = device.deviceStatus()) {

            is Freeze -> {
                deviceListener.onDeviceDied(
                    device = device,
                    message = "DeviceWorker died. Device status is `Freeze`",
                    reason = status.reason
                )
                return@launch
            }

            is Alive -> device.state()
        }

        deviceListener.onDeviceCreated(device, state)

        for (intention in intentionsRouter.observeIntentions(state)) {

            deviceListener.onIntentionReceived(device, intention)

            when (val status = device.deviceStatus()) {

                is Freeze -> {
                    onDeviceDieOnIntention(intention, status.reason)
                    return@launch
                }

                is Alive -> {

                    val (newState, preparingError) = prepareDeviceState(
                        currentState = state,
                        intendedState = intention.state
                    )

                    when {

                        preparingError != null -> {
                            onDeviceDieOnIntention(intention, preparingError)
                            return@launch
                        }

                        newState != null -> {
                            state = newState

                            deviceListener.onStatePrepared(device, newState)

                            deviceListener.onTestStarted(device, intention)

                            val result = executeAction(action = intention.action)

                            deviceListener.onTestCompleted(
                                device = device,
                                intention = intention,
                                result = result
                            )
                        }
                    }
                }
            }
        }

        deviceListener.onFinished(device)
    }

    private suspend fun prepareDeviceState(
        currentState: State,
        intendedState: State
    ): Result<State> = if (intendedState.digest != currentState.digest) {

        device.logger.debug(
            "Current state=${currentState.digest}, " +
                "intended=${intendedState.digest}. Preparing new state..."
        )

        stateWorker.installApplications(
            state = intendedState,
            onAllSucceeded = { installations ->
                installations.forEach { installation ->
                    deviceListener.onApplicationInstalled(
                        device = device,
                        installation = installation
                    )
                }
            }
        )
    } else {
        device.logger.debug(
            "Current state=${currentState.digest}, " +
                "intended=${intendedState.digest}. Clearing packages..."
        )

        stateWorker.clearPackages(currentState)
    }.map { intendedState }

    private suspend fun executeAction(action: InstrumentationTestRunAction): DeviceTestCaseRun {
        val deviceTestCaseRun = try {

            testListener.started(
                device = device,
                targetPackage = action.targetPackage,
                test = action.test,
                executionNumber = action.executionNumber
            )

            device.runIsolatedTest(
                action = action,
                outputDir = outputDirectory
            )
        } catch (t: Throwable) {
            val now = timeProvider.nowInMillis()

            DeviceTestCaseRun(
                testCaseRun = TestCaseRun(
                    test = action.test,
                    result = Failed.InfrastructureError.Unexpected(
                        error = RuntimeException("Unexpected infrastructure error", t),
                    ),
                    timestampStartedMilliseconds = now,
                    timestampCompletedMilliseconds = now
                ),
                device = device.getData()
            )
        }

        val reportFileProvider = TestArtifactsProviderFactory.createForAdbAccess(
            appUnderTestPackage = action.targetPackage,
            className = action.test.className,
            methodName = action.test.methodName
        )

        val testArtifactsDir = reportFileProvider.provideReportDir()

        Log.i("DeviceWorker", "materkey: before finished ${action.test.methodName}")
        testListener.finished(
            device = device,
            test = action.test,
            targetPackage = action.targetPackage,
            result = deviceTestCaseRun.testCaseRun.result,
            durationMilliseconds = deviceTestCaseRun.testCaseRun.durationMilliseconds,
            executionNumber = action.executionNumber,
            testArtifactsDir = testArtifactsDir
        )

        testArtifactsDir.map { dir ->
            val (_, clearError) = device.clearDirectory(remotePath = dir.toPath())
            if (clearError != null) {
                deviceListener.onDeviceDied(device, "Can't clear test metadata dir", clearError)
            }
        }

        return deviceTestCaseRun
    }

    private suspend fun onDeviceDieOnIntention(
        intention: Intention,
        reason: Throwable
    ) {
        deviceListener.onDeviceDied(
            device = device,
            message = "DeviceWorker died. Can't process intention: $intention",
            reason = reason
        )
        deviceListener.onIntentionFail(
            device = device,
            intention = intention,
            reason = reason
        )
    }

    /**
     * layers order matters
     */
    private fun Device.state(): State =
        State(
            layers = listOf(
                State.Layer.ApiLevel(api = api),
                State.Layer.Model(model = model)
            )
        )
}
