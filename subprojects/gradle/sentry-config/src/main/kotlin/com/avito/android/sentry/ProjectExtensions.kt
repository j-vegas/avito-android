@file:Suppress("UnstableApiUsage")

package com.avito.android.sentry

import com.avito.kotlin.dsl.ProjectProperty
import com.avito.kotlin.dsl.PropertyScope.ROOT_PROJECT
import com.avito.kotlin.dsl.getBooleanProperty
import com.avito.kotlin.dsl.getMandatoryStringProperty
import com.avito.kotlin.dsl.getOptionalStringProperty
import com.avito.utils.BuildMetadata
import com.avito.utils.gradle.buildEnvironment
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.gradle.api.Project
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Provider

val Project.sentryConfig: Provider<SentryConfig> by ProjectProperty.lazy(scope = ROOT_PROJECT) { project ->
    Providers.of(from(project))
}

private fun from(project: Project): SentryConfig {
    return if (project.getBooleanProperty("avito.sentry.enabled")) {
        val buildEnv = project.buildEnvironment
        val info = project.environmentInfo().get()
        val tags = mutableMapOf<String, String>()

        val buildId = info.teamcityBuildId()

        val buildIdTag = "build_id"

        if (!buildId.isNullOrBlank()) {
            tags[buildIdTag] = buildId
        }

        val infraVersion = BuildMetadata.kotlinLibraryVersion(SentryConfig::class.java)

        val config = SentryConfig.Enabled(
            dsn = project.getMandatoryStringProperty("avito.sentry.dsn"),
            environment = buildEnv::class.java.simpleName,
            serverName = info.node ?: "unknown",
            release = infraVersion,
            tags = tags
        )

        val projectUrl = project.getOptionalStringProperty("avito.sentry.projectUrl")

        if (!projectUrl.isNullOrBlank() && !buildId.isNullOrBlank()) {
            project.gradle.buildFinished {

                val url = projectUrl.toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.addQueryParameter("query", "$buildIdTag:$buildId")
                    ?.build()
                    ?.toString()

                project.logger.lifecycle("Build errors: $url")
            }
        }

        config
    } else {
        SentryConfig.Disabled
    }
}
