package reactivity.experimental

expect public interface ProducerScope<in T> {
    public suspend fun send(element: T)
}

expect public inline suspend fun <T> Publisher<T>.consumeEach(action: (T) -> Unit)

/**
 * Common functions for [Multi] and [Solo]
 */
interface CommonPublisher<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T> {
    val initialScheduler: Scheduler
}