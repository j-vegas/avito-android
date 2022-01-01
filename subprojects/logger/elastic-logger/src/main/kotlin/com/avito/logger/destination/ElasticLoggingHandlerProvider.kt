package com.avito.logger.destination

import com.avito.android.elastic.ElasticClient
import com.avito.logger.LogLevel
import com.avito.logger.handler.LoggingHandler
import com.avito.logger.handler.LoggingHandlerProvider
import com.avito.logger.metadata.LoggerMetadata

public class ElasticLoggingHandlerProvider(
    private val acceptedLogLevel: LogLevel,
    private val elasticClient: ElasticClient
) : LoggingHandlerProvider {

    override fun provide(metadata: LoggerMetadata): LoggingHandler {
        return ElasticLoggingHandler(acceptedLogLevel, elasticClient, metadata.asMap())
    }
}
