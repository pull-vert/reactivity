package reactivity.core.experimental

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.internal.LockFreeLinkedListNode
import kotlinx.coroutines.experimental.internal.Symbol
import kotlinx.coroutines.experimental.selects.SelectInstance
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.io.Closeable
import kotlin.coroutines.experimental.CoroutineContext

internal const val DEFAULT_CLOSE_MESSAGE = "Consumer was closed"

/**
 * Subscribes to this [Publisher] and returns a channel to receive elements emitted by it.
 * The resulting channel shall be [closed][SubscriptionReceiveChannel.close] to unsubscribe from this publisher.
 */
fun <T> Solo<T>.openDeferred(): ConsumerCloseable<T> {
    val producerDeferred = ProducerConsumerCloseable<T>()
    subscribe(producerDeferred)
    return producerDeferred
}

interface Consumer<out T> : Deferred<T> {
    // override all the Job functions
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
}

/**
 * Return type for [Solo.openDeferred] that can be used to [await] elements from the
 * open producer and to [close] it to unsubscribe.
 */
interface ConsumerCloseable<out T> : Consumer<T>, Closeable {
    /**
     * Closes this deferred.
     */
    override fun close()
}

/**
 * Represents sending waiter in the queue.
 * @suppress **This is unstable API and it is subject to change.**
 */
interface Produce {
    val consumeResult: Any? // E | Closed
    fun tryResumeProduce(idempotent: Any?): Any?
    fun completeResumeProduce(token: Any)
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

/**
 * Represents closed channel.
 * @suppress **This is unstable API and it is subject to change.**
 */
@Suppress("UNCHECKED_CAST")
class ProduceElement(
        override val consumeResult: Any?,
        @JvmField val cont: CancellableContinuation<Unit>
) : LockFreeLinkedListNode(), Produce {

    override fun tryResumeProduce(idempotent: Any?): Any? = cont.tryResume(Unit, idempotent)
    override fun completeResumeProduce(token: Any) = cont.completeResume(token)
    override fun toString(): String = "ProduceElement($consumeResult)[$cont]"

}

/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val COMPLETE_SUCCESS: Any = Symbol("COMPLETE_SUCCESS")
/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val COMPLETE_FAILED: Any = Symbol("COMPLETE_FAILED")
/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val PRODUCE_SUCCESS: Any = Symbol("PRODUCE_SUCCESS")
/** @suppress **This is unstable API and it is subject to change.** */
@JvmField val CLOSE_RESUMED: Any = Symbol("CLOSE_RESUMED")

/**
 * Represents closed consumer.
 * @suppress **This is unstable API and it is subject to change.**
 */
class Closed<in E>(
        @JvmField val closeCause: Throwable?
) : LockFreeLinkedListNode(), Produce, ConsumeOrClosed<E> {
    val produceException: Throwable get() = closeCause ?: ClosedProducerException(DEFAULT_CLOSE_MESSAGE)
    val consumeException: Throwable get() = closeCause ?: ClosedConsumerException(DEFAULT_CLOSE_MESSAGE)

    override val produceResult get() = this
    override val consumeResult get() = this
    override fun tryResumeProduce(idempotent: Any?): Any? = CLOSE_RESUMED
    override fun completeResumeProduce(token: Any) { check(token === CLOSE_RESUMED) }
    override fun tryResumeConsume(value: E, idempotent: Any?): Any? = CLOSE_RESUMED
    override fun completeResumeConsume(token: Any) { check(token === CLOSE_RESUMED) }
    override fun toString(): String = "Closed[$closeCause]"
}

private open class ProducerConsumer<T> : Producer<T>, Consumer<T> {

    val _produceElement: AtomicRef<Produce?> = atomic(null)
    val _consumeElement: AtomicRef<Consume<T>?> = atomic(null)

    /**
     * Returns non-null closed token if it is last in one of the Atomic.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected val completedExceptionally: Closed<*>? get() = _consumeElement.value as? Closed<*>

    /**
     * Invoked when [Closed] element was just added.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected open fun onClosed(closed: Closed<T>) {}

    /**
     * Invoked after successful [close].
     */
    protected open fun afterClose(cause: Throwable?) {}

    // Producer functions
    override val isClosedForProduce: Boolean get() = false

    /**
     * Tries to add element to buffer or to queued receiver.
     * Return type is `OFFER_SUCCESS | OFFER_FAILED | Closed`.
     * @suppress **This is unstable API and it is subject to change.**
     */
    protected open fun completeInternal(element: T): Any {
        while (true) {
            val consume = _consumeElement.value ?: return COMPLETE_FAILED
            val token = consume.tryResumeConsume(element, idempotent = null)
            if (token != null) {
                consume.completeResumeConsume(token)
                return consume.produceResult
            }
        }
    }

    suspend override fun produce(element: T) {
        // fast path -- try offer non-blocking
        if (complete(element)) return
        // slow-path does suspend
        return produceSuspend(element)
    }

    override fun complete(element: T): Boolean {
        val result = completeInternal(element)
        return when {
            result === COMPLETE_SUCCESS -> true
            result === COMPLETE_FAILED -> false
            result is Closed<*> -> throw result.produceException
            else -> error("offerInternal returned $result")
        }
    }

    private suspend fun produceSuspend(element: T): Unit = suspendAtomicCancellableCoroutine(holdCancellability = true) sc@ { cont ->
        val produce = ProduceElement(element, cont)
        loop@ while (true) { // lock-free loop on element
            val enqueueResult = _produceElement.compareAndSet(null, produce)
            if (enqueueResult) {// enqueued successfully
                cont.initCancellability() // make it properly cancellable
                cont.removeOnCancel(produce)
                return@sc
            } else {
                cont.resumeWithException(ClosedConsumerException(DEFAULT_CLOSE_MESSAGE))
                return@sc
            }
        }
    }

    override fun close(cause: Throwable?): Boolean {
        val closed = Closed<T>(cause)
        while (true) {
            val consume = _consumeElement.value
            if (consume == null) {
                // queue empty or has only senders -- try add last "Closed" item to the queue
                val produce = _produceElement.value
                if (produce == null) {
                    _produceElement.compareAndSet(null, closed)
                    onClosed(closed)
                    afterClose(cause)
                    return true
                } else if (produce is Closed<*>) return false // already closed
                continue // retry on failure
            }
            if (consume is Closed<*>) return false // already marked as closed -- nothing to do
//            consume as Consume<T> // type assertion
            consume.resumeReceiveClosed(closed)
        }
    }

    // Consumer functions
    override val isCompletedExceptionally: Boolean get() = completedExceptionally != null

    suspend override fun await(): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCompleted(): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R> registerSelectAwait(select: SelectInstance<R>, block: suspend (T) -> R) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                cont.resumeWithException(closed.consumeException)
        }
        override fun toString(): String = "ReceiveElement[$cont,nullOnClose=$nullOnClose]"
    }
}

private class ProducerConsumerCloseable<T> : ProducerConsumer<T>(), ConsumerCloseable<T>, Subscriber<T> {
    @Volatile
    @JvmField
    var subscription: Subscription? = null

    // request balance from cancelled receivers, balance is negative if we have receivers, but no subscription yet
    val _balance = atomic(0)

    // AbstractChannel overrides
    override fun onEnqueuedReceive() {
        _balance.loop { balance ->
            val subscription = this.subscription
            if (subscription != null) {
                if (balance < 0) { // receivers came before we had subscription
                    // try to fixup by making request
                    if (!_balance.compareAndSet(balance, 0)) return@loop // continue looping
                    subscription.request(-balance.toLong())
                    return
                }
                if (balance == 0) { // normal story
                    subscription.request(1)
                    return
                }
            }
            if (_balance.compareAndSet(balance, balance - 1)) return
        }
    }

    override fun onCancelledReceive() {
        _balance.incrementAndGet()
    }

    override fun afterClose(cause: Throwable?) {
        subscription?.cancel()
    }

    // Subscription overrides
    override fun close() {
        close(cause = null)
    }

    // Subscriber overrides
    override fun onSubscribe(s: Subscription) {
        subscription = s
        while (true) { // lock-free loop on balance
            if (isClosedForProduce) {
                s.cancel()
                return
            }
            val balance = _balance.value
            if (balance >= 0) return // ok -- normal story
            // otherwise, receivers came before we had subscription
            // try to fixup by making request
            if (!_balance.compareAndSet(balance, 0)) continue
            s.request(-balance.toLong())
            return
        }
    }

    override fun onNext(t: T) {
        offer(t)
    }

    override fun onComplete() {
        close(cause = null)
    }

    override fun onError(e: Throwable) {
        close(cause = e)
    }
}
