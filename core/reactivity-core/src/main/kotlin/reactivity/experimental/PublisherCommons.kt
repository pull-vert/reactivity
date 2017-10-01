package reactivity.experimental


/**
 * The default scheduler used for instantiation of [Multi] and [Solo]
 */
internal val DEFAULT_SCHEDULER = Schedulers.defaultDispatcher()

/**
 * Common functions for [Multi] and [Solo]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T> {
    val initialScheduler: Scheduler
}