package com.avito.instrumentation.suite.filter

import com.avito.instrumentation.createStub
import com.avito.instrumentation.internal.suite.filter.IncludeByTestSignaturesFilter
import com.avito.instrumentation.internal.suite.filter.TestsFilter
import com.avito.report.model.DeviceName
import com.avito.truth.isInstanceOf
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class IncludeByTestSignaturesFilterTest {


    fun `when signature matches testName and testDevice then test is included`() {
        val filter = createIncludeTestSignatureFilter(
            signatures = setOf(TestsFilter.Signatures.TestSignature("testName", "deviceName"))
        )
        val test = TestsFilter.Test.createStub(
            name = "testName",
            deviceName = DeviceName("deviceName")
        )

        val actual = filter.filter(test)

        assertThat(actual).isInstanceOf<TestsFilter.Result.Included>()
    }


    fun `when testName startWith signatureName and testDevice equals signatureDevice then test is included`() {
        val filter = createIncludeTestSignatureFilter(
            signatures = setOf(TestsFilter.Signatures.TestSignature("testName", "deviceName"))
        )
        val test = TestsFilter.Test.createStub(
            name = "testName1",
            deviceName = DeviceName("deviceName")
        )

        val actual = filter.filter(test)

        assertThat(actual).isInstanceOf<TestsFilter.Result.Included>()
    }


    fun `when signature matches testName and signature device is null then test is included`() {
        val filter = createIncludeTestSignatureFilter(
            signatures = setOf(TestsFilter.Signatures.TestSignature("testName", null))
        )
        val test = TestsFilter.Test.createStub(
            name = "testName",
            deviceName = DeviceName("deviceName")
        )

        val actual = filter.filter(test)

        assertThat(actual).isInstanceOf<TestsFilter.Result.Included>()
    }


    fun `when signature matches name and signature device doesn't match testDevice then test is excluded`() {
        val filter = createIncludeTestSignatureFilter(
            signatures = setOf(TestsFilter.Signatures.TestSignature("testName", "deviceName1"))
        )
        val test = TestsFilter.Test.createStub(
            name = "testName",
            deviceName = DeviceName("deviceName")
        )

        val actual = filter.filter(test)

        assertThat(actual).isInstanceOf<TestsFilter.Result.Excluded.DoesNotMatchIncludeSignature>()
    }


    fun `when testName doesn't startWith signatureName then test is excluded`() {
        val filter = createIncludeTestSignatureFilter(
            signatures = setOf(TestsFilter.Signatures.TestSignature("differentTestName", "deviceName"))
        )
        val test = TestsFilter.Test.createStub(
            name = "testName",
            deviceName = DeviceName("deviceName")
        )

        val actual = filter.filter(test)

        assertThat(actual).isInstanceOf<TestsFilter.Result.Excluded.DoesNotMatchIncludeSignature>()
    }

    private fun createIncludeTestSignatureFilter(
        signatures: Set<TestsFilter.Signatures.TestSignature>
    ): IncludeByTestSignaturesFilter {
        return IncludeByTestSignaturesFilter(
            source = TestsFilter.Signatures.Source.Code, // source type doesn't affect logic
            signatures = signatures
        )
    }
}
