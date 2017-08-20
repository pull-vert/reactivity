package io.khttp2.internal.common

import java.util.concurrent.Flow

interface SuspendingSubscriber<T> : Flow.Subscriber<T> {
    suspend fun suspendingOnComplete()

    suspend fun suspendingOnError(throwable: Throwable)

    override fun onComplete() {
        TODO("do not override or call onComplete !!! overide and call suspendingOnComplete")
    }

    override fun onError(throwable: Throwable?) {
        TODO("do not override or call onError !!! overide and call suspendingOnError")
    }
}