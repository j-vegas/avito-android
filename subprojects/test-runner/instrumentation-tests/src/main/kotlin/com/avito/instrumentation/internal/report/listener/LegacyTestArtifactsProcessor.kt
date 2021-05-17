package com.avito.instrumentation.internal.report.listener

import com.avito.android.Result
import com.avito.logger.LoggerFactory
import com.avito.logger.create
import com.avito.report.TestArtifactsProviderFactory
import com.avito.report.model.AndroidTest
import com.avito.report.model.TestStaticData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

internal class LegacyTestArtifactsProcessor(
    private val reportParser: ReportParser,
    private val logcatProcessor: LogcatProcessor,
    private val dispatcher: CoroutineDispatcher,
    private val loggerFactory: LoggerFactory
) : TestArtifactsProcessor {

    private val logger = loggerFactory.create<LegacyTestArtifactsProcessor>()

    override fun process(
        reportDir: File,
        testStaticData: TestStaticData,
        logcatBuffer: LogcatBuffer?
    ): Result<AndroidTest> {

        val scope = CoroutineScope(CoroutineName("test-artifacts-${testStaticData.name}") + dispatcher)

        val reportFileProvider = TestArtifactsProviderFactory.createForTempDir(reportDir)

        return reportFileProvider.provideReportFile()
            .flatMap { reportJson -> reportParser.parse(reportJson) }
            .map { testRuntimeData ->

                val isTestFailed = testRuntimeData.incident != null

                runBlocking {
                    withContext(scope.coroutineContext) {

                        val stdout = async {
                            logcatProcessor.process(logcatBuffer?.getStdout(), isUploadNeeded = isTestFailed)
                        }

                        val stderr = async {
                            logcatProcessor.process(logcatBuffer?.getStderr(), isUploadNeeded = isTestFailed)
                        }

                        AndroidTest.Completed.create(
                            testStaticData = testStaticData,
                            testRuntimeData = testRuntimeData,
                            stdout = stdout.await().also {
                                logger.debug("testy LegacyTestArtifactsProcessor $it")
                            },
                            stderr = stderr.await()
                        )
                    }
                }
            }
    }
}
