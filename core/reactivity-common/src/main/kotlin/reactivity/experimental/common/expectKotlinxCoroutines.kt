package reactivity.experimental.common

import kotlin.coroutines.experimental.CoroutineContext

// coroutine contexts
expect abstract class CoroutineDispatcher: CoroutineContext
expect val DefaultDispatcher: CoroutineDispatcher

// general
expect interface ProducerScope<in E> : CoroutineScope, SendChannel<E>
expect interface CoroutineScope {
    val coroutineContext: CoroutineContext
}
expect interface Job : CoroutineContext.Element
expect fun launch(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit
): Job

// channels
expect open class LinkedListChannel<E>(): SendChannel<E>, ReceiveChannel<E> {
    // from superclass AbstractSendChannel
    protected open fun afterClose(cause: Throwable?)
    override fun close(cause: Throwable?): Boolean
    final override suspend fun send(element: E)
    final override fun offer(element: E): Boolean
    // from superclass AbstractChannel
    final override fun iterator(): ChannelIterator<E>
    final override val onReceive: SelectClause1<E>
}
expect interface ChannelIterator<out E> {
    suspend operator fun hasNext(): Boolean
    suspend operator fun next(): E
}
expect interface ReceiveChannel<out E> {
    operator fun iterator(): ChannelIterator<E>
    val onReceive: SelectClause1<E>
}
expect interface SubscriptionReceiveChannel<out T> : ReceiveChannel<T>, Closeable {
    fun close()
}
expect inline suspend fun <E> ReceiveChannel<E>.consumeEach(action: (E) -> Unit)
expect interface SendChannel<in E> {
    suspend fun send(element: E)
    fun offer(element: E): Boolean
    fun close(cause: Throwable?): Boolean
}
expect interface Channel<E> : ReceiveChannel<E>, SendChannel<E>
expect fun <E> Channel(capacity: Int): Channel<E>

// selects
expect interface SelectClause1<out Q>
expect interface SelectBuilder<in R> {
    operator fun <Q> SelectClause1<Q>.invoke(block: suspend (Q) -> R)
}
expect suspend fun whileSelect(builder: SelectBuilder<Boolean>.() -> Unit)