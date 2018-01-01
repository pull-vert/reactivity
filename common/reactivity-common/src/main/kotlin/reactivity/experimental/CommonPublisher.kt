package reactivity.experimental

expect interface ProducerScope<in T> {
    public suspend fun send(element: T)
}

/**
 * Common functions for [Multi] and [Solo]
 */
interface CommonPublisher<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T> {
    val initialScheduler: Scheduler
}