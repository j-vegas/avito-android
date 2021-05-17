package com.avito.instrumentation.suite

import com.avito.android.TestInApk
import com.avito.android.createStubInstance
import com.avito.android.runner.report.Report
import com.avito.android.runner.report.StubReport
import com.avito.instrumentation.configuration.target.TargetConfiguration
import com.avito.instrumentation.createStubInstance
import com.avito.instrumentation.internal.suite.TestSuiteProvider
import com.avito.instrumentation.internal.suite.filter.FilterFactory
import com.avito.instrumentation.internal.suite.filter.TestsFilter
import com.avito.instrumentation.stub.suite.filter.StubFilterFactory
import com.avito.instrumentation.stub.suite.filter.excludedFilter
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class TestSuiteProviderTest {

    private val simpleTestInApk = TestInApk.createStubInstance(
        className = "com.MyTestClass",
        methodName = "test",
        annotations = emptyList()
    )


    fun `test suite - dont skip tests`() {
        val testSuiteProvider = createTestSuiteProvider()

        val result = testSuiteProvider.getTestSuite(
            tests = listOf(simpleTestInApk)
        )

        assertThat(result.testsToRun.map { it.test.name }).containsExactly(simpleTestInApk.testName)
    }


    fun `test suite - skip test - if rerun enabled and test passed in previous run`() {
        val report = StubReport()
        val testSuiteProvider = createTestSuiteProvider(
            report = report,
            reportSkippedTests = true,
            filterFactory = StubFilterFactory(
                filter = excludedFilter(
                    TestsFilter.Result.Excluded.MatchesExcludeSignature(
                        name = "",
                        source = TestsFilter.Signatures.Source.PreviousRun
                    )
                )
            )
        )

        testSuiteProvider.getTestSuite(
            tests = listOf(simpleTestInApk)
        )

        val result = report.reportedSkippedTests?.map { it.first.name }

        assertThat(result).isEmpty()
    }

    private fun createTestSuiteProvider(
        report: Report = StubReport(),
        targets: List<TargetConfiguration.Data> = listOf(TargetConfiguration.Data.createStubInstance()),
        reportSkippedTests: Boolean = false,
        filterFactory: FilterFactory = StubFilterFactory()
    ): TestSuiteProvider {
        return TestSuiteProvider.Impl(
            report = report,
            targets = targets,
            reportSkippedTests = reportSkippedTests,
            filterFactory = filterFactory
        )
    }
}
