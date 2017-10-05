package reactivity.experimental.core

import org.reactivestreams.Publisher

/**
 * Common functions for [DefaultMulti] and [DefaultSolo]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>, Publisher<T> {
    val initialScheduler: Scheduler
}