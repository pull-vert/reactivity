package reactivity.experimental

import org.reactivestreams.Publisher

interface MultiGrouped<T, R> : Multi<T> {
    val key : R
}

internal class MultiGroupedImpl<T, R> internal constructor(delegated: Publisher<T>, override val initialScheduler: Scheduler, override val key: R) : MultiImpl<T>(delegated, initialScheduler), MultiGrouped<T, R>

