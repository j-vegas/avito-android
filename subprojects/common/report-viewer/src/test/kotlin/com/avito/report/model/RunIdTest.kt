package com.avito.report.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class RunIdTest {


    fun `serialize - without prefix`() {
        val actual = RunId(commitHash = COMMIT_HASH, buildTypeId = BUILD_TYPE_ID).toReportViewerFormat()
        val expected = "$COMMIT_HASH${RunId.DELIMITER}$BUILD_TYPE_ID"
        assertThat(actual).isEqualTo(expected)
    }


    fun `serialize - with prefix`() {
        val actual = RunId(
            prefix = PREFIX,
            commitHash = COMMIT_HASH,
            buildTypeId = BUILD_TYPE_ID
        ).toReportViewerFormat()
        val expected = "$PREFIX${RunId.DELIMITER}$COMMIT_HASH${RunId.DELIMITER}$BUILD_TYPE_ID"
        assertThat(actual).isEqualTo(expected)
    }


    fun `serialize - blank prefix`() {
        val actual = RunId(prefix = "   ", commitHash = COMMIT_HASH, buildTypeId = BUILD_TYPE_ID).toReportViewerFormat()
        val expected = "$COMMIT_HASH${RunId.DELIMITER}$BUILD_TYPE_ID"
        assertThat(actual).isEqualTo(expected)
    }

    private companion object {
        const val PREFIX = "prefix"
        const val COMMIT_HASH = "8982d2d2"
        const val BUILD_TYPE_ID = "local"
    }
}
