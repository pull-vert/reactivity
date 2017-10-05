package reactivity.experimental.core

import org.reactivestreams.Publisher


/**
 * The default scheduler used for instantiation of Multi and Solo
 */
val DEFAULT_SCHEDULER = Schedulers.defaultDispatcher()

/**
 * Common functions for [MultiPublisher] and [SoloPublisher]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>, Publisher<T> {
    val initialScheduler: Scheduler
}