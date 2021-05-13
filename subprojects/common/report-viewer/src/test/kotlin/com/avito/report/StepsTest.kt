package com.avito.report

import com.avito.report.model.AndroidTest
import com.avito.report.model.Entry
import com.avito.report.model.ReportCoordinates
import com.avito.report.model.Step
import com.avito.report.model.TestRuntimeDataPackage
import com.avito.report.model.createStubInstance
import com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(StubReportsExtension::class)
internal class StepsTest {

    private fun testRequest(reports: StubReportApi) = reports.addTest(
        reportCoordinates = ReportCoordinates.createStubInstance(),
        buildId = "1234",
        test = AndroidTest.Completed.createStubInstance(
            testRuntimeData = TestRuntimeDataPackage.createStubInstance(
                steps = listOf(
                    Step(
                        timestamp = 1570044898,
                        number = 1,
                        title = "Hello",
                        entryList = listOf(
                            Entry.Comment(title = "Hello from comment", timeInSeconds = 1570044922)
                        )
                    )
                )
            )
        )
    )


    fun `step contains title`(reports: StubReportApi) {
        testRequest(reports)
            .singleRequestCaptured()
            .bodyMatches(
                hasJsonPath(
                    "$.params.report.test_case_step_list[0].title",
                    Matchers.equalTo("Hello")
                )
            )
    }


    fun `step contains number`(reports: StubReportApi) {
        testRequest(reports)
            .singleRequestCaptured()
            .bodyMatches(
                hasJsonPath(
                    "$.params.report.test_case_step_list[0].number",
                    Matchers.equalTo(1)
                )
            )
    }


    fun `step contains timestamp`(reports: StubReportApi) {
        testRequest(reports)
            .singleRequestCaptured()
            .bodyMatches(
                hasJsonPath(
                    "$.params.report.test_case_step_list[0].timestamp",
                    Matchers.equalTo(1570044898)
                )
            )
    }


    fun `step contains entry with type`(reports: StubReportApi) {
        testRequest(reports)
            .singleRequestCaptured()
            .bodyMatches(
                hasJsonPath(
                    "$.params.report.test_case_step_list[0].entry_list[0].type",
                    Matchers.equalTo("comment")
                )
            )
    }


    fun `step contains entry with timestamp`(reports: StubReportApi) {
        testRequest(reports)
            .singleRequestCaptured()
            .bodyMatches(
                hasJsonPath(
                    "$.params.report.test_case_step_list[0].entry_list[0].timestamp",
                    Matchers.equalTo(1570044922)
                )
            )
    }


    fun `step contains comment entry with title`(reports: StubReportApi) {
        testRequest(reports)
            .singleRequestCaptured()
            .bodyMatches(
                hasJsonPath(
                    "$.params.report.test_case_step_list[0].entry_list[0].title",
                    Matchers.equalTo("Hello from comment")
                )
            )
    }
}
