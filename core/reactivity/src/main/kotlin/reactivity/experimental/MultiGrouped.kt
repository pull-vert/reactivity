package reactivity.experimental

import org.reactivestreams.Publisher

interface MultiGrouped<T, out R> : Multi<T> {
    val key: R
}

internal class MultiGroupedImpl<T, out R> internal constructor(
        override val delegate: Publisher<T>,
        override val initialScheduler: Scheduler,
        override val key: R)
    : MultiImpl<T>(delegate, initialScheduler), MultiGrouped<T, R>