@file:Suppress("UnstableApiUsage")

package com.avito.logger

import com.avito.android.elastic.ElasticClientFactory
import com.avito.android.sentry.sentryClient
import com.avito.logger.destination.ElasticLoggingHandlerProvider
import com.avito.logger.destination.SentryLoggingHandlerProvider
import com.avito.logger.handler.FileLoggingHandlerProvider
import com.avito.logger.handler.LoggingHandlerProvider
import com.avito.logger.handler.PrintlnLoggingHandlerProvider
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

public abstract class LoggerService : BuildService<LoggerService.Params> {

    public interface Params : BuildServiceParameters {
        public val fileHandler: Property<LogLevel>
        public val fileHandlerRootDir: DirectoryProperty
        public val printlnHandler: Property<GradleLoggerExtension.PrintlnMode>
        public val sentryHandler: Property<GradleLoggerExtension.Sentry>
        public val elasticHandler: Property<GradleLoggerExtension.Elastic>
        public val finalized: Property<Boolean>
    }

    init {
        require(parameters.finalized.getOrElse(false)) {
            "gradleLogger extension must be finalized. You trying to use it too early"
        }
    }

    private val handlerProviders: List<LoggingHandlerProvider> by lazy {
        val providers = mutableListOf<LoggingHandlerProvider>()
        with(parameters) {
            if (fileHandler.isPresent) {
                providers.add(
                    FileLoggingHandlerProvider(
                        fileHandler.get(),
                        fileHandlerRootDir.get().asFile
                    )
                )
            }
            if (printlnHandler.isPresent) {
                val config = printlnHandler.get()
                providers.add(PrintlnLoggingHandlerProvider(config.level, config.printStackTrace))
            } else {
                providers.add(PrintlnLoggingHandlerProvider(LogLevel.INFO, false))
            }
            if (sentryHandler.isPresent) {
                val config = sentryHandler.get()
                providers.add(SentryLoggingHandlerProvider(config.level, sentryClient(config.config)))
            }
            if (elasticHandler.isPresent) {
                val config = elasticHandler.get()
                providers.add(ElasticLoggingHandlerProvider(config.level, ElasticClientFactory.provide(config.config)))
            }
        }
        providers.toList()
    }

    public fun createLoggerFactory(coordinates: GradleLoggerCoordinates): LoggerFactory {
        val builder = LoggerFactoryBuilder()
        with(builder) {
            metadataProvider(GradleMetadataProvider(coordinates))
            handlerProviders.forEach { addLoggingHandlerProvider(it) }
        }
        return builder.build()
    }
}
