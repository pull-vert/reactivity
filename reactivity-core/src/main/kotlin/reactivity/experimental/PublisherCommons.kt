package reactivity.experimental


/**
 * The default scheduler used for Builder and Operators for [Multi] and [Solo]
 */
internal val DEFAULT_SCHEDULER = Schedulers.emptyThreadContext()

/**
 * Common functions for [Multi] and [Solo]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>