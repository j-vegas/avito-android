package com.avito.instrumentation.internal.finalizer.action

import com.avito.instrumentation.internal.finalizer.verdict.TestStatisticsCounterFactory
import com.avito.instrumentation.internal.finalizer.verdict.Verdict
import com.avito.report.ReportLinkGenerator
import com.avito.report.TestSuiteNameProvider
import com.avito.report.model.AndroidTest
import com.avito.report.model.TestRuntimeData
import com.avito.report.model.duration
import org.apache.commons.text.StringEscapeUtils
import java.io.File

internal class WriteJUnitReportAction(
    private val testSuiteNameProvider: TestSuiteNameProvider,
    private val reportLinkGenerator: ReportLinkGenerator,
    private val destination: File
) : FinalizeAction {

    private val estimatedTestRecordSize = 150

    override fun action(verdict: Verdict) {

        val testStatisticsCounter = TestStatisticsCounterFactory.create(verdict)

        val probablyLostTests = verdict.testResults.filterIsInstance<AndroidTest.Lost>()
        val completedTests = verdict.testResults.filterIsInstance<AndroidTest.Completed>()

        val completedTestsWithLostTries = probablyLostTests.filter { test ->
            completedTests.firstOrNull {
                it.name == test.name
            } != null
        }

        val testCountOverall = testStatisticsCounter.overallCount()
        val testCountFailures = testStatisticsCounter.failureCount()
        val testCountErrors =
            (testStatisticsCounter.notReportedCount() - completedTestsWithLostTries.size).coerceAtLeast(0)
        val testCountSkipped = testStatisticsCounter.skippedCount()

        val xml = buildString(testCountOverall * estimatedTestRecordSize) {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")

            append("<testsuite ")
            append("""name="${testSuiteNameProvider.getName()}" """)
            append("""tests="$testCountOverall" """)
            append("""failures="$testCountFailures" """)
            append("""errors="$testCountErrors" """)
            append("""skipped="$testCountSkipped" """)
            append("""time="${testStatisticsCounter.overallDurationSec()}" """)
            appendLine(">")

            appendLine("<properties/>")

            verdict.testResults.forEach { test ->
                appendTest(test, completedTestsWithLostTries = completedTestsWithLostTries)
            }

            appendLine("</testsuite>")
        }

        destination.writeText(xml)
    }

    private fun Collection<AndroidTest>.isSuccess() =
        firstOrNull {
            it is AndroidTest.Completed && it.incident == null
        } ?: false

    private fun StringBuilder.appendTest(test: AndroidTest, completedTestsWithLostTries: Collection<AndroidTest>) {
        if (test !is AndroidTest.Lost || test.hasNoSuccessRun(completedTestsWithLostTries)) {
            append("<testcase ")
            append("""classname="${test.name.className}" """)
            append("""name="${test.name.methodName}" """)
            append("""caseId="${test.testCaseId}" """)

            if (test is TestRuntimeData) {
                append("""time="${test.duration.toFloat()}"""")
            } else {
                append("""time="-1.0"""")
            }

            appendLine(">")

            when (test) {
                is AndroidTest.Skipped -> {
                    appendLine("<skipped/>")
                    appendLine("<system-out>")
                    appendEscapedLine("Тест не запускался: ${test.skipReason}")
                    appendLine("</system-out>")
                }
                is AndroidTest.Completed -> {
                    val incident = test.incident
                    if (incident != null) {
                        appendLine("<failure>")
                        appendEscapedLine(incident.errorMessage)
                        appendLine(reportLinkGenerator.generateTestLink(test.name))
                        appendLine("</failure>")
                    }
                }
                is AndroidTest.Lost -> {
                    appendLine("<error>")
                    appendLine("LOST (no info in report)")
                    appendLine(reportLinkGenerator.generateTestLink(test.name))
                    appendLine("</error>")
                }
            }

            appendLine("</testcase>")
        }
    }

    private fun AndroidTest.Lost.hasNoSuccessRun(tests: Collection<AndroidTest>) =
        tests.firstOrNull { test ->
            this.name == test.name && test is AndroidTest.Completed && test.incident == null
        } == null

    private fun StringBuilder.appendEscapedLine(line: String) {
        appendLine(StringEscapeUtils.escapeXml10(line))
    }
}
