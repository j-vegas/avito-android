package com.avito.runner.scheduler.suite.filter

import com.avito.logger.LoggerFactory
import com.avito.report.Report
import com.avito.runner.config.InstrumentationFilterData

internal interface FilterFactory {

    fun createFilter(): TestsFilter

    companion object {

        internal const val JUNIT_IGNORE_ANNOTATION = "org.junit.Ignore"

        fun create(
            filterData: InstrumentationFilterData,
            impactAnalysisResult: ImpactAnalysisResult,
            loggerFactory: LoggerFactory,
            report: Report,
            defaultDevice: String?,
        ): FilterFactory {
            return FilterFactoryImpl(
                filterData = filterData,
                impactAnalysisResult = impactAnalysisResult,
                loggerFactory = loggerFactory,
                report = report,
                defaultDevice = defaultDevice,
            )
        }
    }
}
