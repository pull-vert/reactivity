package reactivity.experimental.core

import org.reactivestreams.Publisher


/**
 * Common functions for [MultiPublisher] and [SoloPublisher]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>, Publisher<T> {
    val initialScheduler: Scheduler
}