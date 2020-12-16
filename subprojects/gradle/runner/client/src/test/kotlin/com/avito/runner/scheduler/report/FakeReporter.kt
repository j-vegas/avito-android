package com.avito.runner.scheduler.report

import com.avito.runner.scheduler.report.model.SummaryReport

class FakeReporter : Reporter {

    var reported: SummaryReport? = null

    override fun report(report: SummaryReport) {
        reported = report
    }
}
