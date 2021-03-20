package com.avito.git

import com.avito.kotlin.dsl.getMandatoryStringProperty
import com.avito.kotlin.dsl.getOptionalStringProperty
import com.avito.logger.LoggerFactory
import org.gradle.api.Project
import java.io.File

class GitStateFromEnvironment(
    rootDir: File,
    loggerFactory: LoggerFactory,
    gitBranch: String,
    targetBranch: String?,
    originalCommitHash: String?
) : GitState {

    override val defaultBranch = "develop"

    override val originalBranch: Branch

    override val currentBranch: Branch

    override val targetBranch: Branch?

    init {
        @Suppress("NAME_SHADOWING")
        val gitBranch: String = gitBranch.asBranchWithoutOrigin()

        @Suppress("NAME_SHADOWING")
        val targetBranch: String? = targetBranch?.asBranchWithoutOrigin()

        val git = Git.Impl(
            rootDir = rootDir,
            loggerFactory = loggerFactory
        )

        val gitCommit = git.tryParseRev("HEAD").getOrThrow()

        this.currentBranch = Branch(
            name = gitBranch,
            commit = gitCommit
        )

        this.originalBranch = Branch(
            name = gitBranch,
            commit = originalCommitHash ?: gitCommit
        )

        var target: Branch? = null

        if (!targetBranch.isNullOrBlank()) {
            val remoteTargetBranch: String = targetBranch.asOriginBranch()

            // Сначала ищем ветку из remote если ее нет, ищем локальную ветку
            git.tryParseRev(remoteTargetBranch)
                .rescue { git.tryParseRev(targetBranch) }
                .onSuccess { targetCommit ->
                    target = Branch(
                        name = targetBranch,
                        commit = targetCommit
                    )
                }
        }

        this.targetBranch = target
    }

    companion object {

        fun from(project: Project, loggerFactory: LoggerFactory): GitState {
            val gitBranch: String = project.getMandatoryStringProperty("gitBranch")
            val targetBranch: String? = project.getOptionalStringProperty("targetBranch")
            val originalCommitHash: String? = project.getOptionalStringProperty("originalCommitHash")
            return GitStateFromEnvironment(
                rootDir = project.rootDir,
                loggerFactory = loggerFactory,
                gitBranch = gitBranch,
                targetBranch = targetBranch,
                originalCommitHash = originalCommitHash
            )
        }
    }
}
