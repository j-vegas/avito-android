package com.avito.report

import com.avito.report.model.AndroidTest
import com.avito.report.model.DeviceName
import com.avito.report.model.ReportCoordinates
import com.avito.report.model.TestStaticDataPackage
import com.avito.report.model.createStubInstance
import com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(StubReportsExtension::class)
internal class EnvironmentTest {

    
    fun `deviceName sent as environment field`(reports: StubReportApi) {
        reports.addTest(
            reportCoordinates = ReportCoordinates.createStubInstance(),
            buildId = "1234",
            test = AndroidTest.Completed.createStubInstance(
                testStaticData = TestStaticDataPackage.createStubInstance(deviceName = DeviceName("API-22"))
            )
        )
            .singleRequestCaptured()
            .bodyMatches(hasJsonPath("$.params.environment", Matchers.equalTo("API-22")))
    }
}
