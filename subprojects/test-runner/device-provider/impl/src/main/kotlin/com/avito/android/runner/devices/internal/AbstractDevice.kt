package com.avito.android.runner.devices.internal

import com.avito.android.Result
import com.avito.android.waiter.waitForCondition
import com.avito.logger.LoggerFactory
import com.avito.logger.create
import com.avito.runner.service.worker.device.adb.Adb
import com.avito.utils.ProcessRunner
import kotlinx.coroutines.delay
import java.io.File
import java.time.Duration

internal abstract class AbstractDevice(
    protected val loggerFactory: LoggerFactory,
    protected val processRunner: ProcessRunner,
) : Device {

    private val logger = loggerFactory.create<AbstractDevice>()

    protected abstract val adb: Adb

    override fun redirectLogcatToFile(
        file: File,
        tags: Collection<String>
    ) {
        val tagsString = if (tags.isEmpty()) {
            ""
        } else {
            " ${tags.joinToString(" ")}"
        }

        // TODO stop endless process
        processRunner.spawn(
            command = "$adb -s $serial logcat$tagsString",
            outputTo = file
        )
    }

    protected suspend fun isBootCompleted(): Result<String> =
        waitForCommand(
            command = {
                processRunner.run(
                    command = "$adb -s $serial $CHECK_BOOT_COMPLETED_COMMAND",
                    timeout = Duration.ofSeconds(60)
                ).flatMap { bootResult ->
                    if (bootResult == "1") {
                        Result.Success(bootResult)
                    } else {
                        Result.Failure(RuntimeException("Failed to connect to $serial"))
                    }
                }
            },
            timeoutSec = 600,
            attempts = 10,
            frequencySec = 10
        )

    protected suspend fun <T> waitForCommand(
        command: suspend () -> Result<T>,
        attempts: Int = 12,
        frequencySec: Long = 5,
        timeoutSec: Long = 60
    ) = waitForCondition(
        conditionName = "Wait device with serial: $serial",
        maxAttempts = attempts,
        onSuccess = { conditionName: String, durationMs: Long, attempt: Int ->
            logger.debug("$conditionName succeed in $durationMs at attempt=$attempt")
        },
        sleepAction = { frequencyMs: Long -> delay(frequencyMs) },
        frequencySeconds = frequencySec,
        timeoutSec = timeoutSec,
        condition = command
    )
}

private const val CHECK_BOOT_COMPLETED_COMMAND = "shell getprop sys.boot_completed"
