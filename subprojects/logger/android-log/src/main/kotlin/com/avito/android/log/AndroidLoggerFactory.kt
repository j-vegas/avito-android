package com.avito.android.log

import com.avito.android.elastic.ElasticConfig
import com.avito.android.log.destination.AndroidLogDestination
import com.avito.logger.DefaultLogger
import com.avito.logger.Logger
import com.avito.logger.LoggerFactory
import com.avito.logger.handler.CombinedHandler
import com.avito.logger.handler.DefaultLoggingHandler
import com.avito.logger.handler.LoggingHandler

class AndroidLoggerFactory(
    private val elasticConfig: ElasticConfig,
    private val testName: String?
) : LoggerFactory {

    fun newFactory(
        newElasticConfig: ElasticConfig? = null,
        newTestName: String? = null
    ): AndroidLoggerFactory {
        return AndroidLoggerFactory(
            elasticConfig = newElasticConfig ?: elasticConfig,
            testName = newTestName ?: testName
        )
    }

    override fun create(tag: String): Logger {

        val metadata = AndroidTestMetadata(
            tag = tag,
            testName = testName
        )

        val defaultHandler = defaultHandler(metadata)

        val errorHandler = errorHandler(defaultHandler)

        return DefaultLogger(
            debugHandler = defaultHandler,
            infoHandler = defaultHandler,
            warningHandler = errorHandler,
            criticalHandler = errorHandler
        )
    }

    private fun defaultHandler(metadata: AndroidTestMetadata): LoggingHandler {
        return if (elasticConfig is ElasticConfig.Enabled) {
            CombinedHandler(
                listOf(
                    androidLogHandler(metadata),
                    elasticHandler(metadata)
                )
            )
        } else {
            androidLogHandler(metadata)
        }
    }

    private fun androidLogHandler(metadata: AndroidTestMetadata): LoggingHandler {
        return DefaultLoggingHandler(destination = AndroidLogDestination(metadata))
    }

    private fun elasticHandler(metadata: AndroidTestMetadata): LoggingHandler {
        return DefaultLoggingHandler(destination = ElasticDestinationFactory.create(elasticConfig, metadata))
    }

    private fun errorHandler(defaultHandler: LoggingHandler): LoggingHandler =
        defaultHandler
}
