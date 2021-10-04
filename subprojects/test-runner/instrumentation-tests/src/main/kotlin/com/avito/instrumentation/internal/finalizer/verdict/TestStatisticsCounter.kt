package com.avito.instrumentation.internal.finalizer.verdict

internal interface TestStatisticsCounter {

    fun overallDurationSec(): Float

    fun overallCount(): Int

    fun successCount(): Int

    fun skippedCount(): Int

    fun failureCount(): Int

    fun notReportedCount(): Int
}
