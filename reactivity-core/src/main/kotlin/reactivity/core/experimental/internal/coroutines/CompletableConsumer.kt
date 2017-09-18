package reactivity.core.experimental.internal.coroutines

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.internal.LockFreeLinkedListNode
import kotlinx.coroutines.experimental.internal.Symbol
import kotlinx.coroutines.experimental.selects.SelectInstance
import reactivity.core.experimental.ClosedProducerException
import kotlin.coroutines.experimental.CoroutineContext

internal const val DEFAULT_CLOSE_MESSAGE = "Consumer was closed"

interface CompletableConsumer<T> : CompletableDeferred<T> {
    // overrides all the Job functions
    override val key: CoroutineContext.Key<*>
        get() = throw UnsupportedOperationException("Operators should not use this method!")
    override val isActive: Boolean
        get() = throw UnsupportedOperationException("Operators should not use this method!")
    override val isCancelled: Boolean
        get() = throw UnsupportedOperationException("Operators should not use this method!")
    override val isCompleted: Boolean
        get() = throw UnsupportedOperationException("Operators should not use this method!")

    override fun cancel(cause: Throwable?): Boolean {
        throw UnsupportedOperationException("Operators should not use this method!")
    }

    override fun getCompletionException(): Throwable {
        throw UnsupportedOperationException("Operators should not use this method!")
    }

    override fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle {
        throw UnsupportedOperationException("Operators should not use this method!")
    }

    override fun invokeOnCompletion(handler: CompletionHandler, onCancelling: Boolean): DisposableHandle {
        throw UnsupportedOperationException("Operators should not use this method!")
    }

    suspend override fun join() {
        throw UnsupportedOperationException("Operators should not use this method!")
    }

    override fun <R> registerSelectJoin(select: SelectInstance<R>, block: suspend () -> R) {
        throw UnsupportedOperationException("Operators should not use this method!")
    }

    override fun start(): Boolean {
        throw UnsupportedOperationException("Operators should not use this method!")
    }

    // new close function
    fun close(cause: Throwable?): Boolean
}

/**
 * Represents receiver waiter in the queue or closed token.
 * @suppress **This is unstable API and it is subject to change.**
 */
interface ConsumeOrClosed<in E> {
    val produceResult: Any // PRODUCE_SUCCESS | Closed
    fun tryResumeConsume(value: E, idempotent: Any?): Any?
    fun completeResumeConsume(token: Any)
}

private abstract class Consume<in E> : LockFreeLinkedListNode(), ConsumeOrClosed<E> {
    override val produceResult get() = PRODUCE_SUCCESS
    abstract fun resumeReceiveClosed(closed: Closed<*>)
}

/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val COMPLETE_SUCCESS: Any = Symbol("COMPLETE_SUCCESS")
/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val PRODUCE_SUCCESS: Any = Symbol("PRODUCE_SUCCESS")
/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val CLOSE_RESUMED: Any = Symbol("CLOSE_RESUMED")
/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val GET_COMPLETED_FAILED: Any = Symbol("GET_COMPLETED_FAILED")

/**
 * Represents closed consumer.
 * @suppress **This is unstable API and it is subject to change.**
 */
class Closed<in E>(
        @JvmField val closeCause: Throwable?
) : LockFreeLinkedListNode(), ConsumeOrClosed<E> {
    val produceException: Throwable get() = closeCause ?: ClosedProducerException(DEFAULT_CLOSE_MESSAGE)
    val awaitException: Throwable get() = closeCause ?: ClosedConsumerException(DEFAULT_CLOSE_MESSAGE)

    override val produceResult get() = this
    override fun tryResumeConsume(value: E, idempotent: Any?): Any? = CLOSE_RESUMED
    override fun completeResumeConsume(token: Any) { check(token === CLOSE_RESUMED) }
    override fun toString(): String = "Closed[$closeCause]"
}

internal open class CompletableConsumerImpl<T> : CompletableConsumer<T> {

    val _consumeElement: AtomicRef<ConsumeOrClosed<T>?> = atomic(null)
    // Can be null | Closed | or T
    val _bufferedElement: AtomicRef<Any?> = atomic(null)

    /**
     * Returns non-null closed token if it exists on the Atomic.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected val completedExceptionally: Closed<*>? get() = _consumeElement.value as Closed<*>

    /**
     * Returns non-null closed token if it is last in one of the Atomic.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected val isClosed = completedExceptionally != null

    /**
     * Invoked when [Closed] element was just added.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected open fun onClosed(closed: Closed<T>) {}

    /**
     * Invoked after successful [close].
     */
    protected open fun afterClose(cause: Throwable?) {}

    /**
     * Invoked when value consumer was successfully cancelled.
     */
    protected open fun onCancelledAwait() {}

    /**
     * Invoked when enqueued receiver was successfully cancelled.
     */
    protected open fun onAwait() {}

    /**
     * Tries to add element to buffer
     * Return type is `PRODUCE_SUCCESS | Closed`.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected open fun completeInternal(element: T): Any {
        while (true) {
            val consume = _consumeElement.value
            if (null == consume) {
                if (_bufferedElement.compareAndSet(null, element))
                    return PRODUCE_SUCCESS
            } else {
                val token = consume.tryResumeConsume(element, idempotent = null)
                if (token != null) {
                    consume.completeResumeConsume(token)
                    return consume.produceResult
                }
            }
        }
    }

    override fun complete(element: T): Boolean {
        val result = completeInternal(element)
        return when {
            result === COMPLETE_SUCCESS -> true
            result is Closed<*> -> throw result.produceException
            else -> error("offerInternal returned $result")
        }
    }

    override fun completeExceptionally(exception: Throwable) = close(exception)

    override fun close(cause: Throwable?): Boolean {
        val closed = Closed<T>(cause)
        while (true) {
            val consume = _consumeElement.value
            if (consume == null) {
                if (_consumeElement.compareAndSet(null, closed)) {
                    onClosed(closed)
                    afterClose(cause)
                    return true
                } else
                    continue // retry on failure
            }
            if (consume is Closed<*>) return false // already marked as closed -- nothing to do
            (consume as Consume).resumeReceiveClosed(closed)
        }
    }

    // Consumer functions
    override val isCompletedExceptionally: Boolean get() = completedExceptionally != null

    suspend override fun await(): T {
        // fast path -- try completed non-blocking
        val result = getCompletedInternal()
        if (result !== GET_COMPLETED_FAILED) return awaitResult(result)
        // slow-path does suspend
        return awaitSuspend()
    }

    @Suppress("UNCHECKED_CAST")
    private fun awaitResult(result: Any?): T {
        if (result is Closed<*>) throw result.awaitException
        return result as T
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun awaitSuspend(): T = suspendAtomicCancellableCoroutine(holdCancellability = true) sc@ { cont ->
        val consume = ConsumeElement(cont as CancellableContinuation<T?>, nullOnClose = false)
        while (true) { // lock-free loop on Atomic
            if (_consumeElement.compareAndSet(null, consume)) {
                onAwait()
                cont.initCancellability() // make it properly cancellable
                removeAwaitOnCancel(cont, consume)
                return@sc
            }
            // hm... something is not right. try to getCompleted
            val result = getCompletedInternal()
            if (result is Closed<*>) {
                cont.resumeWithException(result.awaitException)
                return@sc
            }
            if (result !== GET_COMPLETED_FAILED) {
                cont.resume(result as T)
                return@sc
            }
        }
    }

    private fun removeAwaitOnCancel(cont: CancellableContinuation<*>, consume: Consume<*>) {
        cont.invokeOnCompletion {
            if (cont.isCancelled && consume.remove())
                onCancelledAwait()
        }
    }

    override fun getCompleted(): T {
        val result = getCompletedInternal()
        if (result === GET_COMPLETED_FAILED)
            throw IllegalStateException("result = GET_COMPLETED_FAILED")
        else
            return awaitResultOrThrowIllegalStateException(result)
    }

    @Suppress("UNCHECKED_CAST")
    private fun awaitResultOrThrowIllegalStateException(result: Any?): T {
        if (result is Closed<*>) {
            if (result.closeCause != null) throw result.closeCause
            throw IllegalStateException("result is Closed without closeCause")
        }
        return result as T
    }

    /**
     * Tries to get the unique element from the AtomicRef
     * Return type is `T | GET_COMPLETED_FAILED | Closed`
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected open fun getCompletedInternal(): Any? {
        while (true) {
            return _bufferedElement.value ?: return GET_COMPLETED_FAILED
        }
    }

    override fun <R> registerSelectAwait(select: SelectInstance<R>, block: suspend (T) -> R) {
        // TODO : implement when needed
    }

    private class ConsumeElement<in E>(
            @JvmField val cont: CancellableContinuation<E?>,
            @JvmField val nullOnClose: Boolean
    ) : Consume<E>() {
        override fun tryResumeConsume(value: E, idempotent: Any?): Any? = cont.tryResume(value, idempotent)
        override fun completeResumeConsume(token: Any) = cont.completeResume(token)
        override fun resumeReceiveClosed(closed: Closed<*>) {
            if (closed.closeCause == null && nullOnClose)
                cont.resume(null)
            else
                cont.resumeWithException(closed.awaitException)
        }
        override fun toString(): String = "ConsumeElement[$cont,nullOnClose=$nullOnClose]"
    }
}