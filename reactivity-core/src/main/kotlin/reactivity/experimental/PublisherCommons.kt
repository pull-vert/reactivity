package reactivity.experimental

/**
 * Common functions for [Multi] and [Solo]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>