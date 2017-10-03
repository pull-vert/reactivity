package reactivity.experimental.core


/**
 * The default scheduler used for instantiation of Multi and Solo
 */
val DEFAULT_SCHEDULER = Schedulers.defaultDispatcher()

/**
 * Common functions for [Multi] and [SoloPublisher]
 */
interface PublisherCommons<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T>