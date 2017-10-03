package reactivity.experimental

import org.reactivestreams.Publisher
import reactivity.experimental.core.MultiPublisherGrouped
import reactivity.experimental.core.MultiPublisherImpl
import reactivity.experimental.core.Scheduler

interface MultiGrouped<T, out R> : MultiPublisherGrouped<T, R>, Multi<T>

internal class MultiGroupedImpl<T, out R> internal constructor(delegate: Publisher<T>, initialScheduler: Scheduler, override val key: R) : MultiImpl<T>(MultiPublisherImpl<T>(delegate, initialScheduler)), MultiGrouped<T, R>