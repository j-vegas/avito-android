package com.avito.emcee.worker.internal.storage

import com.avito.emcee.queue.Bucket

internal class SingleElementProcessingBucketsStorage : ProcessingBucketsStorage {

    private var bucket: Bucket? = null
    private val mutex = Any()

    override fun add(bucket: Bucket) = synchronized(mutex) {
        require(this.bucket == null) { "There is a bucket with id=${bucket.bucketId} already" }
        this.bucket = bucket
    }

    override fun remove(bucket: Bucket) = synchronized(mutex) {
        require(this.bucket == bucket) { "There is no bucket with id=${bucket.bucketId}" }
        this.bucket = null
    }

    override fun getAll(): Set<Bucket> = synchronized(mutex) {
        return if (bucket == null) emptySet() else setOf(requireNotNull(bucket))
    }
}
