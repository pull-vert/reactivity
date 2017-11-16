package reactivity.experimental.common

import kotlin.coroutines.experimental.CoroutineContext

// coroutine contexts
expect abstract class CoroutineDispatcher: CoroutineContext
expect val DefaultDispatcher: CoroutineDispatcher

// channels
expect open class LinkedListChannel<E>() {
    // from superclass AbstractSendChannel
    protected open fun afterClose(cause: Throwable?)
    // from SendChannel
    fun offer(element: E): Boolean
    fun close(cause: Throwable?): Boolean
}
expect interface SubscriptionReceiveChannel<out T> {
    fun close()
}