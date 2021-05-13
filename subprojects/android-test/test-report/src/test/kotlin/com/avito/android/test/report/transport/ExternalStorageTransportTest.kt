package com.avito.android.test.report.transport

import com.avito.android.test.report.ReportState.NotFinished.Initialized
import com.avito.android.test.report.createStubInstance
import com.avito.android.test.report.model.TestMetadata
import com.avito.android.test.report.model.createStubInstance
import com.avito.logger.StubLoggerFactory
import com.avito.report.TestArtifactsProvider
import com.avito.report.TestArtifactsProviderFactory
import com.avito.report.model.Entry
import com.avito.report.model.FileAddress
import com.avito.time.StubTimeProvider
import com.avito.truth.assertThat
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ExternalStorageTransportTest {

    private val timeProvider = StubTimeProvider()

    private val loggerFactory = StubLoggerFactory

    
    fun `sendReport - file written`(@TempDir tempDir: File) {
        val testMetadata = TestMetadata.createStubInstance(className = "com.Test", methodName = "test")

        val reportState = Initialized.Started.createStubInstance(testMetadata = testMetadata)

        val outputFileProvider = createOutputFileProvider(
            rootDir = tempDir,
            testMetadata = testMetadata
        )

        createTransport(outputFileProvider).sendReport(reportState)

        val reportFile = File(tempDir, "runner/com.Test#test/report.json")

        assertThat(reportFile.exists()).isTrue()
    }

    
    fun `sendContent plainText - file with content written`(@TempDir tempDir: File) {
        val testMetadata = TestMetadata.createStubInstance()

        val outputFileProvider = createOutputFileProvider(
            rootDir = tempDir,
            testMetadata = testMetadata
        )

        val result = createTransport(outputFileProvider).sendContent(
            testMetadata,
            content = "text",
            type = Entry.File.Type.plain_text,
            comment = "test"
        )

        assertThat<Entry.File>(result.get()) {
            assertThat<FileAddress.File>(fileAddress) {
                val contentFile = File(tempDir, "runner/com.Test#test/${this.fileName}")

                assertThat(contentFile.exists()).isTrue()

                val content = contentFile.readText()

                assertThat(content).isEqualTo("text")
            }
        }
    }

    private fun createTransport(testArtifactsProvider: TestArtifactsProvider): ExternalStorageTransport {
        return ExternalStorageTransport(
            timeProvider = timeProvider,
            loggerFactory = loggerFactory,
            testArtifactsProvider = testArtifactsProvider
        )
    }

    private fun createOutputFileProvider(
        rootDir: File,
        testMetadata: TestMetadata
    ): TestArtifactsProvider {
        return TestArtifactsProviderFactory.create(
            testReportRootDir = lazy { rootDir },
            className = testMetadata.className,
            methodName = testMetadata.methodName!!
        )
    }
}
