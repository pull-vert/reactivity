package reactivity.experimental

import reactivity.experimental.expect.Publisher
import reactivity.experimental.expect.Scheduler

/**
 * Common functions for [Multi] and [Solo]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>, Publisher<T> {
    val initialScheduler: Scheduler
    val delegate: Publisher<T>
}