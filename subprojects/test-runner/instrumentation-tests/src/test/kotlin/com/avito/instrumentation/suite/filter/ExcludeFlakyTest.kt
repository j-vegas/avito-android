package com.avito.instrumentation.suite.filter

import com.avito.instrumentation.configuration.InstrumentationFilter
import com.avito.instrumentation.createStub
import com.avito.instrumentation.internal.suite.filter.ExcludeByFlakyFilter
import com.avito.instrumentation.internal.suite.filter.TestsFilter
import com.avito.report.model.Flakiness
import com.avito.truth.isInstanceOf
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ExcludeFlakyTest {


    fun include_stable() {
        val result = ExcludeByFlakyFilter().filter(
            TestsFilter.Test.createStub(
                flakiness = Flakiness.Stable
            )
        )

        assertThat(result)
            .isEqualTo(TestsFilter.Result.Included)
    }


    fun exclude_flaky() {
        val result = ExcludeByFlakyFilter().filter(
            TestsFilter.Test.createStub(
                flakiness = Flakiness.Flaky("")
            )
        )

        assertThat(result).isInstanceOf<TestsFilter.Result.Excluded.HasFlakyAnnotation>()
    }


    fun exclude_flaky_in_integration_with_filter_factory() {
        val filterFactory = StubFilterFactory.create(
            filter = InstrumentationFilter.Data.createStub(
                excludeFlaky = true
            )
        )

        val result = filterFactory.createFilter().filter(
            TestsFilter.Test.createStub(
                flakiness = Flakiness.Flaky("")
            )
        )

        assertThat(result).isInstanceOf<TestsFilter.Result.Excluded.HasFlakyAnnotation>()
    }


    fun include_stable_in_integration_with_filter_factory() {
        val filterFactory = StubFilterFactory.create(
            filter = InstrumentationFilter.Data.createStub(
                excludeFlaky = true
            )
        )

        val result = filterFactory.createFilter().filter(
            TestsFilter.Test.createStub(
                flakiness = Flakiness.Stable
            )
        )

        assertThat(result)
            .isEqualTo(TestsFilter.Result.Included)
    }


    fun include_flaky_in_integration_with_filter_factory() {
        val filterFactory = StubFilterFactory.create(
            filter = InstrumentationFilter.Data.createStub(
                excludeFlaky = false
            )
        )
        val result = filterFactory.createFilter().filter(
            TestsFilter.Test.createStub(
                flakiness = Flakiness.Flaky("")
            )
        )

        assertThat(result)
            .isEqualTo(TestsFilter.Result.Included)
    }
}
