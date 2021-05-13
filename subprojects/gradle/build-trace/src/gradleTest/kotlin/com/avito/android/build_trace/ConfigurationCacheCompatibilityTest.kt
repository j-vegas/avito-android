package com.avito.android.build_trace

import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.TestResult
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.plugin.plugins
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ConfigurationCacheCompatibilityTest {

    /**
     * TODO blocked by agp 4.2
     */
    @Disabled

    fun `configuration with applied plugin - ok`(@TempDir projectDir: File) {
        TestProjectGenerator(
            name = "rootapp",
            plugins = plugins {
                id("com.avito.android.build-trace")
            },
            buildGradleExtra = """
                buildTrace {
                    enabled.set(true)
                }
                """.trimIndent(),
            modules = listOf(
                AndroidAppModule(name = "app")
            )
        ).generateIn(projectDir)

        runTask(projectDir).assertThat().buildSuccessful()

        runTask(projectDir).assertThat().buildSuccessful().configurationCachedReused()
    }

    private fun runTask(projectDir: File): TestResult {
        return gradlew(
            projectDir,
            "help",
            dryRun = true,
            configurationCache = true
        )
    }
}
