package reactivity.experimental

import kotlin.coroutines.experimental.CoroutineContext

// coroutine contexts
expect val DefaultDispatcher: CoroutineContext

// general
expect interface CoroutineScope {
    val context: CoroutineContext
}
//expect interface SendChannel<E> {
//    suspend fun send(element: E)
//    fun close(cause: Throwable?): Boolean
//    fun offer(element: E): Boolean
//}
expect interface ProducerScope<in E> {
    suspend fun send(element: E)
    val context: CoroutineContext
}
expect interface Job : CoroutineContext.Element
expect fun launch(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> Unit
): Job

// selects
expect interface SelectClause1<out Q>
expect interface SelectBuilder<in R> {
    operator fun <Q> SelectClause1<Q>.invoke(block: suspend (Q) -> R)
}
expect suspend fun whileSelect(builder: SelectBuilder<Boolean>.() -> Unit)

// channels
expect interface ChannelIterator<out E> {
    suspend operator fun hasNext(): Boolean
    suspend operator fun next(): E
}
expect interface ReceiveChannel<out E> {
    val onReceive: SelectClause1<E>
    operator fun iterator() : ChannelIterator<E>
}
expect open class LinkedListChannel<E> {
    // from superclass AbstractSendChannel
    protected open fun afterClose(cause: Throwable?)
    open fun close(cause: Throwable?): Boolean
//    suspend fun send(element: E)
    fun offer(element: E): Boolean
    // from superclass AbstractChannel
    fun iterator(): ChannelIterator<E>
    val onReceive: SelectClause1<E>
}
expect interface SubscriptionReceiveChannel<out T> : Closeable {
    operator fun iterator(): ChannelIterator<T>
    val onReceive: SelectClause1<T>
}
expect inline suspend fun <E> ReceiveChannel<E>.consumeEach(action: (E) -> Unit)
expect interface Channel<E> : ReceiveChannel<E> {
    suspend fun send(element: E)
    fun close(cause: Throwable?): Boolean
}
expect fun <E> Channel(capacity: Int): Channel<E>