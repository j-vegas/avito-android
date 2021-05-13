package com.avito.instrumentation

import com.avito.instrumentation.internal.InstrumentationTestsActionFactory
import com.avito.instrumentation.internal.finalizer.verdict.InstrumentationTestsTaskVerdict
import com.github.salomonbrys.kotson.fromJson
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

public class InstrumentationTestsTaskVerdictSerializationTest {

    private val gson = InstrumentationTestsActionFactory.gson


    public fun `serialize verdict`() {
        val expected = InstrumentationTestsTaskVerdict(
            title = "Stub title",
            reportUrl = "https://stub-url",
            problemTests = setOf(
                InstrumentationTestsTaskVerdict.Test(
                    testUrl = "https://stub-url",
                    title = "stub test title"
                )
            )
        )

        val actual = gson.fromJson<InstrumentationTestsTaskVerdict>(gson.toJson(expected))

        assertThat(expected)
            .isEqualTo(actual)
    }
}
