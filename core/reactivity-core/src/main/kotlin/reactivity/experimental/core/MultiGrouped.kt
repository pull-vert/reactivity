package reactivity.experimental.core

import org.reactivestreams.Publisher

interface MultiGrouped<T, R> : Multi<T> {
    val key : R
}

internal class MultiGroupedImpl<T, R> internal constructor(delegate: Publisher<T>, initialScheduler: Scheduler, override val key: R) : MultiImpl<T>(delegate, initialScheduler), MultiGrouped<T, R>