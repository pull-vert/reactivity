package io.khttp2.internal.common

import java.util.concurrent.Flow

interface SuspendingSubscriber<T> : Flow.Subscriber<T> {
    suspend fun onCompleteSuspend()

    suspend fun onErrorSuspend(throwable: Throwable)

    override fun onComplete() {
        TODO("do not override or call onComplete !!! overide and call onCompleteSuspend")
    }

    override fun onError(throwable: Throwable?) {
        TODO("do not override or call onError !!! overide and call onErrorSuspend")
    }
}