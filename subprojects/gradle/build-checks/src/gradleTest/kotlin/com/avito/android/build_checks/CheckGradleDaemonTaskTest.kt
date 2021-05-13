package com.avito.android.build_checks

import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.plugin.plugins
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CheckGradleDaemonTaskTest {

    
    fun `checkGradleDaemon - passes - when no buildSrc in project`(@TempDir projectDir: File) {
        TestProjectGenerator(
            plugins = plugins {
                id("com.avito.android.build-checks")
            },
            buildGradleExtra = """
                buildChecks {
                    enableByDefault = false
                    gradleDaemon {}
                }
            """.trimIndent()
        )
            .generateIn(projectDir)

        gradlew(projectDir, ":checkGradleDaemon")
    }
}
