package reactivity.experimental

/**
 * Common functions for [Multi] and [Solo]
 */
interface CommonPublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T> {
    val initialScheduler: Scheduler
}