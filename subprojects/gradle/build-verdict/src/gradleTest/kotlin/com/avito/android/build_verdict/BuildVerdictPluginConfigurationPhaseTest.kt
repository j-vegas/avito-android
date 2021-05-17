package com.avito.android.build_verdict

import com.avito.android.build_verdict.internal.BuildVerdict
import com.avito.android.build_verdict.internal.Error.Multi
import com.avito.android.build_verdict.internal.Error.Single
import com.avito.test.gradle.dependencies.GradleDependency.Safe.Companion.project
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.module.KotlinModule
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BuildVerdictPluginConfigurationPhaseTest : BaseBuildVerdictTest() {

    private val htmlVerdicts by lazy {
        HtmlVerdictCases.Configuration(temp)
    }

    private val plainTextVerdicts by lazy {
        PlainTextVerdictCases.Configuration(temp)
    }

    
    fun `configuration success`() {
        generateProject()

        val result = gradlew(
            temp,
            "help",
            dryRun = true,
            expectFailure = false
        )

        result.assertThat().buildSuccessful()
        assertBuildVerdictFileExist(false)
    }

    
    fun `configuration fails - illegal method`() {
        generateProject(
            AndroidAppModule(
                name = appName,
                buildGradleExtra = """illegal gradle text"""
            )
        )

        val result = gradlew(
            temp,
            "help",
            dryRun = true,
            expectFailure = true
        )
        result.assertThat().buildFailed()
        assertBuildVerdictFiles(
            expectedPlainTextVerdict = plainTextVerdicts.illegalMethodFails(),
            expectedHtmlVerdict = htmlVerdicts.illegalMethodFails()
        )
        val actualBuildVerdict = gson.fromJson(jsonBuildVerdict.readText(), BuildVerdict.Configuration::class.java)

        assertThat(actualBuildVerdict.error.message).isEqualTo("Build completed with 2 failures.")

        val errors = (actualBuildVerdict.error as Multi).errors

        assertThat(errors).hasSize(2)

        @Suppress("MaxLineLength")
        errors[0].assertSingleError(
            expectedMessageLines = listOf(
                "$temp/app/build.gradle' line: 7",
                "A problem occurred evaluating project ':app'."
            ),
            expectedCauseMessages = listOf(
                "A problem occurred evaluating project ':app'.",
                "Could not find method illegal()"
            )
        )

        errors[1].assertSingleError(
            expectedMessageLines = listOf(
                "A problem occurred configuring project ':app'."
            ),
            expectedCauseMessages = listOf(
                "A problem occurred configuring project ':app'.",
                "compileSdkVersion is not specified"
            )
        )
    }

    
    fun `configuration fails - wrong project dependency`() {
        generateProject(
            KotlinModule(
                name = appName,
                dependencies = setOf(
                    project(":not-existed")
                )
            )
        )

        val result = gradlew(
            temp,
            "help",
            dryRun = true,
            expectFailure = true
        )
        result.assertThat().buildFailed()
        assertBuildVerdictFiles(
            expectedPlainTextVerdict = plainTextVerdicts.wrongProjectDependencyFails(),
            expectedHtmlVerdict = htmlVerdicts.wrongProjectDependencyFails()
        )

        val actualBuildVerdict = gson.fromJson(jsonBuildVerdict.readText(), BuildVerdict.Configuration::class.java)
        val error = actualBuildVerdict.error as Single

        error.assertSingleError(
            expectedMessageLines = listOf(
                "$temp/app/build.gradle' line: 8",
                "A problem occurred evaluating project ':app'."
            ),
            expectedCauseMessages = listOf(
                "A problem occurred evaluating project ':app'.",
                "Project with path ':not-existed' could not be found"
            )
        )
    }

    private fun assertBuildVerdictFiles(
        expectedPlainTextVerdict: String,
        expectedHtmlVerdict: String
    ) {
        assertBuildVerdictFileExist(true)
        assertThat(plainTextBuildVerdict.readText()).isEqualTo(expectedPlainTextVerdict)
        assertThat(htmlBuildVerdict.readText()).isEqualTo(expectedHtmlVerdict)
    }

    private fun Single.assertSingleError(
        expectedMessageLines: List<String>,
        expectedCauseMessages: List<String>
    ) {
        val messageLines = message.lines()
        assertThat(messageLines).hasSize(expectedMessageLines.size)
        messageLines.forEachIndexed { index, line ->
            assertThat(line).contains(expectedMessageLines[index])
        }
        assertThat(causes).hasSize(expectedCauseMessages.size)
        causes.forEachIndexed { index, cause ->
            assertThat(cause.message).contains(expectedCauseMessages[index])
        }
    }
}
