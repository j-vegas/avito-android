package com.avito.emcee.worker.internal

import com.avito.emcee.queue.Bucket
import kotlinx.coroutines.flow.Flow

internal interface TestJobProducer {

    data class Job(
        val workerId: String,
        val payloadSignature: String,
        val bucket: Bucket
    )

    fun getJobs(): Flow<Job>
}
