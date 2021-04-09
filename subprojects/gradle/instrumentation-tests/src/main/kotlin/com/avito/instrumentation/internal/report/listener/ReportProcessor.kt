package com.avito.instrumentation.internal.report.listener

import com.avito.report.model.AndroidTest
import com.avito.runner.scheduler.listener.TestResult
import com.avito.runner.service.model.TestCase

internal interface ReportProcessor {

    fun createTestReport(
        result: TestResult,
        test: TestCase,
        executionNumber: Int,
        logcatBuffer: LogcatBuffer?
    ): AndroidTest
}
