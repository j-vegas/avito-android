package com.avito.emcee.worker

import com.avito.emcee.worker.WorkerQueueApi.Companion.createWorkerQueueApi
import com.avito.emcee.worker.internal.TestJobProducerImpl
import com.avito.emcee.worker.internal.consumer.FakeTestJobConsumer
import com.avito.emcee.worker.internal.networking.SocketAddressResolver
import com.avito.emcee.worker.internal.rest.HttpServer
import com.avito.emcee.worker.internal.rest.handler.ProcessingBucketsRequestHandler
import com.avito.emcee.worker.internal.storage.ProcessingBucketsStorage
import com.avito.emcee.worker.internal.storage.SingleElementProcessingBucketsStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalCli
internal class StartWorkerCommand(
    name: String,
    description: String
) : Subcommand(name, description) {

    private val configPath: String by option(
        type = ArgType.String,
        fullName = "config",
        shortName = "c",
        description = "Absolute path to worker config"
    ).required()

    private val debugMode: Boolean by option(
        type = ArgType.Boolean,
        fullName = "debug",
        shortName = "d",
        description = "Enables verbose logging",
    ).default(false)

    override fun execute() {
        val moshi = Moshi.Builder().build()
        val okHttpClient = OkHttpClient.Builder().apply {
            if (debugMode) {
                addInterceptor(HttpLoggingInterceptor { message ->
                    println(message)
                }.apply { level = HttpLoggingInterceptor.Level.BODY })
            }
        }.build()
        val socketAddressResolver = SocketAddressResolver()
        val configAdapter = moshi.adapter<Config>()
        val config: Config = requireNotNull(
            configAdapter.fromJson(File(configPath).readText())
        )
        val bucketsStorage: ProcessingBucketsStorage = SingleElementProcessingBucketsStorage()
        val api = Retrofit.Builder().createWorkerQueueApi(okHttpClient, config.queueUrl)

        HttpServer.Builder()
            .addHandler(ProcessingBucketsRequestHandler(bucketsStorage))
            .debug(debugMode)
            .build()
            .also {
                Executors.newSingleThreadExecutor().execute {
                    it.start(config.workerPort)
                }
            }

        val producer = TestJobProducerImpl(
            api = api,
            workerId = config.workerId,
            workerAddress = socketAddressResolver.resolve(config.workerPort)
        )
        val consumer = FakeTestJobConsumer(
            api = api,
            bucketsStorage = bucketsStorage,
        )

        runBlocking {
            consumer.consume(
                producer.getJobs()
            ).collect { result ->
                println(result)
            }
        }
    }
}
