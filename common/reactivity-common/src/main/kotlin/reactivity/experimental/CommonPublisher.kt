package reactivity.experimental

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlin.coroutines.experimental.CoroutineContext

expect public interface ProducerScope<in T>: CoroutineScope {
    public suspend fun send(element: T)
}

expect public inline suspend fun <T> Publisher<T>.consumeEach(action: (T) -> Unit)

expect internal inline suspend fun <T> CommonPublisher<T>.commonConsumeEach(action: (T) -> Unit)

expect interface Closeable

expect public interface ChannelIterator<out E> {
    public abstract operator suspend fun hasNext(): kotlin.Boolean

    public abstract operator suspend fun next(): E
}

expect public interface ReceiveChannel<out E> {
    public operator fun iterator(): ChannelIterator<E>
}

expect public interface SubscriptionReceiveChannel<out T> : ReceiveChannel<T>, Closeable

expect public fun <T> CommonPublisher<T>.openSubscription(): SubscriptionReceiveChannel<T>

expect public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R

expect public interface SendChannel<in E> {
    public suspend fun send(element: E)
    @Suppress("EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER")
    public fun close(cause: Throwable? = null): Boolean
}

expect public interface Channel<E> : SendChannel<E>, ReceiveChannel<E>

expect public fun <E> Channel(capacity: Int): Channel<E>

@Suppress("EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER")
expect public fun <T> ReceiveChannel<T>.asPublisher(context: CoroutineContext = DefaultDispatcher): Publisher<T>

/**
 * Common functions for [Multi] and [Solo]
 */
expect interface CommonPublisher<T> : WithCallbacks<T>, WithPublishOn, WithLambdas<T> {
    val initialScheduler: Scheduler
}