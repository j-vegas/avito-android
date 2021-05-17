package com.avito.android

import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.dependencies.GradleDependency.Safe.Companion.project
import com.avito.test.gradle.git
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.module.AndroidLibModule
import com.avito.test.gradle.plugin.plugins
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ModuleTypesPluginTest {

    private lateinit var projectDir: File

    @BeforeEach
    fun setup(@TempDir tempPath: Path) {
        projectDir = tempPath.toFile()
    }

    @Suppress("MaxLineLength")

    fun `android application - having library dependencies in implementation configuration in library module - has checkProjectDependenciesTypeTask scheduled`() {
        TestProjectGenerator(
            plugins = plugins {
                id("com.avito.android.impact")
            },
            modules = listOf(
                AndroidAppModule(
                    "app",
                    plugins = plugins {
                        id("com.avito.android.module-types")
                    }
                ),
                AndroidLibModule(
                    "feature",
                    plugins = plugins {
                        id("com.avito.android.module-types")
                    },
                    dependencies = setOf(project(":dependent_test_module"))
                ),
                AndroidLibModule(
                    "dependent_test_module",
                    plugins = plugins {
                        id("com.avito.android.module-types")
                    }
                )
            )
        ).generateIn(projectDir)

        with(projectDir) {
            git("checkout -b develop")
        }

        val result = gradlew(
            projectDir,
            "assemble",
            "-Pavito.moduleTypeValidationEnabled=true",
            "-PgitBranch=xxx", // todo need for impact plugin
            dryRun = true
        )
        result.assertThat()
            .tasksShouldBeTriggered(
                ":dependent_test_module:checkProjectDependenciesType",
                ":feature:checkProjectDependenciesType"
            )
    }
}
