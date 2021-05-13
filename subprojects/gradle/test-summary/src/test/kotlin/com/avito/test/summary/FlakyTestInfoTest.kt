package com.avito.test.summary

import com.avito.android.Result
import com.avito.report.model.SimpleRunTest
import com.avito.report.model.Stability
import com.avito.report.model.createStubInstance
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class FlakyTestInfoTest {

    private val flakyTestInfo = FlakyTestInfo()


    fun `test info summarized`() {
        flakyTestInfo.addReport(
            report = Result.Success(
                listOf(
                    SimpleRunTest.createStubInstance(
                        name = "com.avito.Test.test",
                        stability = Stability.Flaky(attemptsCount = 3, successCount = 1),
                        lastAttemptDurationInSeconds = 22
                    )
                )
            )
        )

        flakyTestInfo.addReport(
            report = Result.Success(
                listOf(
                    SimpleRunTest.createStubInstance(
                        name = "com.avito.Test.test",
                        stability = Stability.Flaky(attemptsCount = 2, successCount = 1),
                        lastAttemptDurationInSeconds = 10
                    )
                )
            )
        )

        val info = flakyTestInfo.getInfo().single()
        assertThat(info.attempts).isEqualTo(5)
        assertThat(info.wastedTimeEstimateInSec).isEqualTo(86)
    }
}
