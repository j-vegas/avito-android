package com.avito.instrumentation

import com.avito.instrumentation.configuration.InstrumentationPluginConfiguration.GradleInstrumentationPluginConfiguration.Data
import com.avito.report.model.RunId
import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.ciRun
import com.avito.test.gradle.file
import com.avito.test.gradle.git
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.plugin.plugins
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.ObjectInputStream
import java.nio.file.Path

internal class InstrumentationParamsBuildingTest {

    private lateinit var projectDir: File

    private val buildType: String = "buildType"
    private val targetBranch: String = "another"
    private lateinit var commit: String

    private val pluginInstrumentationParams by lazy {
        mapOf(
            "planSlug" to "AvitoAndroid",
            "jobSlug" to "FunctionalTests",
            "override" to "overrideInPlugin",
            "deviceName" to "local",
            "teamcityBuildId" to "0",
            "buildBranch" to "develop",
            "buildCommit" to commit,
            "runId" to RunId(
                prefix = "stub",
                commitHash = commit,
                buildTypeId = "teamcity-$buildType"
            ).toReportViewerFormat(),
            "reportApiUrl" to "http://stub", // from InstrumentationPluginConfiguration
            "reportViewerUrl" to "http://stub",
            "fileStorageUrl" to "http://stub",
            "sentryDsn" to "stub",
            "inHouse" to "true",
            "avito.report.enabled" to "false" // todo
        )
    }

    @BeforeEach
    fun setup(@TempDir temp: Path) {
        projectDir = temp.toFile()

        projectDir.apply {
            TestProjectGenerator(
                modules = listOf(
                    AndroidAppModule(
                        "app",
                        plugins = plugins {
                            id("com.avito.android.instrumentation-tests")
                        },
                        buildGradleExtra = """
                         import static com.avito.instrumentation.reservation.request.Device.LocalEmulator

                         android {
                            defaultConfig {
                                testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
                                testInstrumentationRunnerArguments([
                                    "planSlug" : "AvitoAndroid",
                                    "override": "createdInInstrumentationRunnerArguments"
                                ])
                            }
                         }

                         instrumentation {
                             output = project.file("outputs").path
                             sentryDsn = "stub"

                             instrumentationParams = [
                                 "jobSlug": "FunctionalTests",
                                 "override": "overrideInPlugin"
                             ]
                             
                             testReport {
                                reportViewer {
                                    reportApiUrl = "http://stub"
                                    reportViewerUrl = "http://stub"
                                    reportRunIdPrefix = "stub"
                                    fileStorageUrl = "http://stub"
                                }
                             }

                             configurations {

                                 functional {
                                    instrumentationParams = [
                                        "configuration": "functional",
                                        "override": "overrideInConfiguration"
                                    ]

                                    targets {
                                        api22 {
                                            instrumentationParams = [
                                                "deviceName": "invalid",
                                                "target": "yes",
                                                "override": "overrideInTarget"
                                            ]

                                            deviceName = "api22"

                                            scheduling {
                                                quota {
                                                    minimumSuccessCount = 1
                                                }

                                                staticDevicesReservation {
                                                    device = LocalEmulator.device(27)
                                                    count = 1
                                                }
                                            }
                                        }
                                    }
                                 }

                                 dynamic {
                                    targets {
                                        disabled {
                                            deviceName = "disabled"

                                            enabled = false

                                            scheduling {
                                                quota {
                                                    minimumSuccessCount = 1
                                                }

                                                staticDevicesReservation {
                                                    device = LocalEmulator.device(27)
                                                    count = 1
                                                }
                                            }
                                        }

                                        enabled {
                                            deviceName = "enabled"

                                            enabled = true

                                            scheduling {
                                                quota {
                                                    minimumSuccessCount = 1
                                                }

                                                staticDevicesReservation {
                                                    device = LocalEmulator.device(27)
                                                    count = 1
                                                }
                                            }
                                        }
                                    }
                                 }
                             }
                         }
                    """.trimIndent()
                    )
                )
            ).generateIn(this)
        }

        with(projectDir) {
            git("branch $targetBranch")
            commit = git("rev-parse HEAD").trim()
        }
    }

    @Suppress("MaxLineLength")
    
    fun `plugin instrumentation params has entries from android plugin configuration and environment override by root plugin configuration`() {
        val result = runGradle(
            "app:instrumentationDumpConfiguration"
        )
        result.assertThat().buildSuccessful()

        val data: Data = ObjectInputStream(
            projectDir
                .file(instrumentationDumpPath)
                .inputStream()
        )
            .use {
                it.readObject() as Data
            }

        val pluginConfigurationInstrumentationParameters = data.checkPluginLevelInstrumentationParameters()

        assertThat(pluginConfigurationInstrumentationParameters)
            .containsExactlyEntriesIn(
                pluginInstrumentationParams
            )
        assertThat(data.reportViewer)
            .isEqualTo(
                Data.ReportViewer(
                    reportApiUrl = "http://stub",
                    reportViewerUrl = "http://stub",
                    reportRunIdPrefix = "stub",
                    fileStorageUrl = "http://stub"
                )
            )
    }

    @Suppress("MaxLineLength")
    
    fun `functional configuration instrumentation params has entries from plugin configuration override by functional configuration`() {
        val result = runGradle(
            "app:instrumentationDumpConfiguration"
        )
        result.assertThat().buildSuccessful()

        val data: Data = ObjectInputStream(
            projectDir
                .file(instrumentationDumpPath)
                .inputStream()
        )
            .use {
                it.readObject() as Data
            }

        val functionalConfigurationInstrumentationParameters = data.configurations
            .find { it.name == "functional" }!!
            .instrumentationParams

        assertThat(functionalConfigurationInstrumentationParameters)
            .containsExactlyEntriesIn(
                pluginInstrumentationParams.plus(
                    mapOf(
                        "override" to "overrideInConfiguration",
                        "configuration" to "functional"
                    )
                )
            )
    }

    @Suppress("MaxLineLength")
    
    fun `target configuration instrumentation params has entries from instrumentation configuration override by target configuration`() {
        val result = runGradle(
            "app:instrumentationDumpConfiguration"
        )
        result.assertThat().buildSuccessful()

        val data: Data = ObjectInputStream(
            projectDir
                .file(instrumentationDumpPath)
                .inputStream()
        )
            .use {
                it.readObject() as Data
            }

        val api22TargetInstrumentationParameters = data.configurations
            .find { it.name == "functional" }!!
            .targets
            .find { it.name == "api22" }!!
            .instrumentationParams

        assertThat(api22TargetInstrumentationParameters)
            .containsExactlyEntriesIn(
                pluginInstrumentationParams.plus(
                    mapOf(
                        "target" to "yes",
                        "deviceName" to "api22",
                        "override" to "overrideInTarget",
                        "configuration" to "functional"
                    )
                )
            )
    }

    
    fun `target with enabled false hasn not passed through`() {
        val result = runGradle(
            "app:instrumentationDumpConfiguration"
        )
        result.assertThat().buildSuccessful()

        val data: Data = ObjectInputStream(
            projectDir
                .file(instrumentationDumpPath)
                .inputStream()
        )
            .use {
                it.readObject() as Data
            }

        val dynamicInstrumentationConfiguration = data.configurations
            .find { it.name == "dynamic" }!!

        assertThat(dynamicInstrumentationConfiguration.targets.find { it.name == "disabled" })
            .isNull()
        assertThat(dynamicInstrumentationConfiguration.targets.find { it.name == "enabled" })
            .isNotNull()
    }

    private fun runGradle(vararg args: String) =
        ciRun(
            projectDir,
            *args,
            "-PteamcityBuildId=0",
            buildType = buildType,
            targetBranch = targetBranch
        )
}
