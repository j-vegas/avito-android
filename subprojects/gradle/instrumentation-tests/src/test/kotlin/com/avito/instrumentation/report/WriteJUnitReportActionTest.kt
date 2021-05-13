package com.avito.instrumentation.report

import com.avito.instrumentation.internal.finalizer.TestRunResult
import com.avito.instrumentation.internal.finalizer.action.LegacyWriteJUnitReportAction
import com.avito.instrumentation.internal.finalizer.verdict.HasFailedTestDeterminer
import com.avito.instrumentation.internal.finalizer.verdict.HasNotReportedTestsDeterminer
import com.avito.instrumentation.internal.finalizer.verdict.LegacyVerdict
import com.avito.report.NoOpReportLinkGenerator
import com.avito.report.NoOpTestSuiteNameProvider
import com.avito.report.model.SimpleRunTest
import com.avito.report.model.Status
import com.avito.report.model.createStubInstance
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class WriteJUnitReportActionTest {

    lateinit var file: File

    private val reportViewerUrl = "https://report/"

    @BeforeEach
    fun setup(@TempDir temp: Path) {
        file = File(temp.toFile(), "sample_report.xml")
    }


    fun `junit report - contains caseId data`() {
        mockData(
            TestRunResult(
                reportedTests = listOf(SimpleRunTest.createStubInstance(testCaseId = 8888)),
                failed = HasFailedTestDeterminer.Result.NoFailed,
                notReported = HasNotReportedTestsDeterminer.Result.AllTestsReported
            )
        )
        val rawFile = file.readText()
        assertThat(rawFile).contains("caseId=\"8888\"")
    }


    fun `junit report - contains skipped case`() {
        mockData(
            TestRunResult(
                reportedTests = listOf(SimpleRunTest.createStubInstance(status = Status.Skipped("reason"))),
                failed = HasFailedTestDeterminer.Result.NoFailed,
                notReported = HasNotReportedTestsDeterminer.Result.AllTestsReported
            )
        )
        val rawFile = file.readText()
        assertThat(rawFile).contains("<skipped/>")
    }


    fun `junit report - contains failure data with report viewer link`() {
        val failed = SimpleRunTest.createStubInstance(
            id = "id",
            status = Status.Failure(verdict = "Something went wrong", errorHash = "111")
        )

        mockData(
            TestRunResult(
                reportedTests = listOf(
                    failed
                ),
                failed = HasFailedTestDeterminer.Result.Failed(failed = listOf(failed)),
                notReported = HasNotReportedTestsDeterminer.Result.AllTestsReported
            )
        )
        val rawFile = file.readText()
        assertThat(rawFile).contains("<failure>\nSomething went wrong\n$reportViewerUrl\n</failure>")
    }


    fun `junit report - contains test class and method names`() {
        mockData(
            TestRunResult(
                reportedTests = listOf(
                    SimpleRunTest.createStubInstance(
                        name =
                        "com.avito.android.deep_linking.DeepLinkingActivityIntentFilterTest.resolve_advert_legacyFormat"
                    )
                ),
                failed = HasFailedTestDeterminer.Result.NoFailed,
                notReported = HasNotReportedTestsDeterminer.Result.AllTestsReported
            )
        )
        val rawFile = file.readText()
        assertThat(rawFile).contains("classname=\"com.avito.android.deep_linking.DeepLinkingActivityIntentFilterTest\"")
        assertThat(rawFile).contains("name=\"resolve_advert_legacyFormat\"")
    }

    private fun mockData(testRunResult: TestRunResult, verdict: LegacyVerdict = LegacyVerdict.Success("")) {
        LegacyWriteJUnitReportAction(
            destination = file,
            testSuiteNameProvider = NoOpTestSuiteNameProvider(),
            reportLinkGenerator = NoOpReportLinkGenerator(testLink = reportViewerUrl)
        ).action(
            testRunResult = testRunResult,
            verdict = verdict
        )
    }
}
