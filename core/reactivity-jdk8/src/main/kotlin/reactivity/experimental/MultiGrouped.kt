package reactivity.experimental

import org.reactivestreams.Publisher
import reactivity.experimental.core.DefaultMultiGrouped
import reactivity.experimental.core.Scheduler

abstract class MultiGrouped<T, out R> internal constructor(): Multi<T>(), DefaultMultiGrouped<T, R>

internal class MultiGroupedImpl<T, out R> internal constructor(private val del: MultiImpl<T>, override val key: R)
    : MultiGrouped<T, R>(), Publisher<T> by del.delegate {
    override val delegate: Publisher<T>
        get() = del.delegate
    override val initialScheduler: Scheduler
        get() = del.initialScheduler
    }