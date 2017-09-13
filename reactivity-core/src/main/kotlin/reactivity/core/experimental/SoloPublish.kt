package reactivity.core.experimental

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.sync.Mutex
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

/**
 * Creates cold reactive [Solo] that runs a given [block] in a coroutine.
 * Every time the returned publisher is subscribed, it starts a new coroutine in the specified [context].
 * Coroutine emits items with `send`. Unsubscribing cancels running coroutine.
 *
 * Invocations of `send` are suspended appropriately when subscribers apply back-pressure and to ensure that
 * `onNext` is not invoked concurrently.
 *
 * | **Coroutine action**                         | **Signal to subscriber**
 * | -------------------------------------------- | ------------------------
 * | `value in end of coroutine is not null`      | `onNext`
 * | Normal completion or `close` without cause   | `onComplete`
 * | Failure with exception or `close` with cause | `onError`
 */
fun <T> solo(
        scheduler: Scheduler,
        block: suspend ProducerCoroutineScope<T>.() -> Unit
): Solo<T> = SoloImpl(Publisher<T> { subscriber ->
    val newContext = newCoroutineContext(scheduler.context)
    val coroutine = SoloCoroutine(newContext, subscriber)
    coroutine.initParentJob(scheduler.context[Job])
    subscriber.onSubscribe(coroutine) // do it first (before starting coroutine), to avoid unnecessary suspensions
    block.startCoroutine(coroutine, coroutine)
})

interface Producer<in E> {
    /**
     * Returns `true` if this consumer was closed by invocation of [close] and thus
     * the [produce] attempt throws [ClosedSendChannelException]. If the consumer was closed because of the exception, it
     * is considered closed, too, but it is called a _failed_ consumer. Any suspending attempts to produce
     * the element to a failed consumer throw the original [close] cause exception.
     */
    val isClosedForProduce: Boolean

    /**
     * Adds [element] into this consumer, suspending the caller while this consumer awaits,
     * or throws [ClosedSendChannelException] if the consumer [isClosedForProduce] _normally_.
     * It throws the original [close] cause exception if the consumer has _failed_.
     *
     * Note, that closing a consumer _after_ this function had suspended does not cause this suspended produce invocation
     * to abort, because closing a consumer is conceptually like producing a special "close token" over this consumer.
     * The element that is sent to the consumer is delivered in one to one. The element that
     * is being sent will get delivered to receiver before a close token.
     *
     * This suspending function is cancellable. If the [Job] of the current coroutine is cancelled or completed while this
     * function is suspended, this function immediately resumes with [CancellationException].
     *
     * *Cancellation of suspended produce is atomic* -- when this function
     * throws [CancellationException] it means that the [element] was not sent to this consumer.
     * As a side-effect of atomic cancellation, a thread-bound coroutine (to some UI thread, for example) may
     * continue to execute even after it was cancelled from the same thread in the case when this produce operation
     * was already resumed and the continuation was posted for execution to the thread's queue.
     *
     * Note, that this function does not check for cancellation when it is not suspended.
     * Use [yield] or [CoroutineScope.isActive] to periodically check for cancellation in tight loops if needed.
     *
     * This function cannot be used in [select] invocation for now
     * Use [complete] to try sending to this consumer without waiting.
     */
    suspend fun produce(element: E)

    /**
     * Adds [element] into this consumer if it is possible to do so immediately without violating restrictions
     * and returns `true`. Otherwise, it returns `false` immediately
     * or throws [ClosedSendChannelException] if the consumer [isClosedForProduce] _normally_.
     * It throws the original [close] cause exception if the consumer has _failed_.
     */
    fun complete(element: E): Boolean

//    /**
//     * Registers [onSend][SelectBuilder.onSend] select clause.
//     * @suppress **This is unstable API and it is subject to change.**
//     */
//    public fun <R> registerSelectSend(select: SelectInstance<R>, element: E, block: suspend () -> R)

    /**
     * Closes this consumer with an optional exceptional [cause].
     * This is an idempotent operation -- repeated invocations of this function have no effect and return `false`.
     * Conceptually, its produce a special "close token" over this consumer. Immediately after invocation of this function
     * [isClosedForProduce] starts returning `true`.
     *
     * A consumer that was closed without a [cause], is considered to be _closed normally_.
     * A consumer that was closed with non-null [cause] is called a _failed consumer_. Attempts to produce or
     * await on a failed consumer throw this cause exception.
     */
    fun close(cause: Throwable? = null): Boolean
}

/**
 * Scope for [solo] coroutine builder.
 */
interface ProducerCoroutineScope<in E> : CoroutineScope, Producer<E> {
    /**
     * A reference to the producer that this coroutine [produce][produce] elements to.
     * It is provided for convenience, so that the code in the coroutine can refer
     * to the producer as `producer` as apposed to `this`.
     * All the [Producer] functions on this interface delegate to
     * the producer instance returned by this function.
     */
    val producer: Producer<E>
}

/**
 * Indicates attempt to [produce][Producer.produce] on [isClosedForProduce][Producer.isClosedForProduce] consumer
 * that was closed _normally_. A _failed_ consumer rethrows the original [close][Producer.close] cause
 * exception on send attempts.
 */
class ClosedProducerException(message: String?) : CancellationException(message)

/**
 * Indicates attempt to [receive][Consumer.await] on [isCompletedExceptionally][Consumer.isCompletedExceptionally]
 * channel that was closed _normally_. A _failed_ producer rethrows the original [close][Producer.close] cause
 * exception on receive attempts.
 */
class ClosedConsumerException(message: String?) : NoSuchElementException(message)

private const val CLOSED_MESSAGE = "This subscription had already closed (completed or failed)"
private const val CLOSED = -1L    // closed, but have not signalled onCompleted/onError yet
private const val SIGNALLED = -2L  // already signalled subscriber onCompleted/onError

private class SoloCoroutine<T>(
        parentContext: CoroutineContext,
        private val subscriber: Subscriber<T>
) : AbstractCoroutine<Unit>(parentContext, true), ProducerCoroutineScope<T>, Subscription {

    override val producer: Producer<T> get() = this

    // Mutex is locked when either nRequested == 0 or while subscriber.onXXX is being invoked
    private val mutex = Mutex(locked = true)

    private val _nRequested = atomic(0L) // < 0 when closed (CLOSED or SIGNALLED)

    override val isClosedForProduce: Boolean get() = isCompleted
    override fun close(cause: Throwable?): Boolean = cancel(cause)

    private fun sendException() =
            (state as? CompletedExceptionally)?.cause ?: ClosedProducerException(CLOSED_MESSAGE)

    override fun complete(element: T): Boolean {
        if (!mutex.tryLock()) return false
        doLockedUnique(element)
        return true
    }

    suspend override fun produce(element: T) {
        // fast-path -- try produce without suspension
        if (complete(element)) return
        // slow-path does suspend
        return produceSuspend(element)
    }

    private suspend fun produceSuspend(element: T) {
        mutex.lock()
        doLockedUnique(element)
    }

    // assert: mutex.isLocked()
    private fun doLockedUnique(elem: T) {
        // check if already closed for send
        if (!isActive) {
            doLockedSignalCompleted()
            throw sendException()
        }
        // notify subscriber
        try {
            subscriber.onNext(elem)
        } catch (e: Throwable) {
            try {
                if (!cancel(e))
                    handleCoroutineException(coroutineContext, e)
            } finally {
                doLockedSignalCompleted()
            }
            throw sendException()
        }
        // now update nRequested
        while (true) { // lock-free loop on nRequested
            val cur = _nRequested.value
            if (cur < 0) break // closed from inside onNext => unlock
            if (cur == Long.MAX_VALUE) break // no back-pressure => unlock
            val upd = cur - 1
            if (_nRequested.compareAndSet(cur, upd)) {
                if (upd == 0L) {
                    println("return without calling doLockedSignalCompleted and keep mutex locked due to back-pressure")
                    return // return to keep locked due to back-pressure
                }
                break // unlock if upd > 0
            }
        }

        // Call doLockedSignalCompleted because there is only one Unique item in Publisher
        println("Call doLockedSignalCompleted in doLockedUnique because there is only one Unique item in Publisher")
        doLockedSignalCompleted()

//        /*
//           There is no sense to check for `isActive` before doing `unlock`, because cancellation/completion might
//           happen after this check and before `unlock` (see `onCancellation` that does not do anything
//           if it fails to acquire the lock that we are still holding).
//           We have to recheck `isActive` after `unlock` anyway.
//         */
//        mutex.unlock()
//        // recheck isActive
//        if (!isActive && mutex.tryLock()) {
//            println("call doLockedSignalCompleted from doLockedUnique : coroutine is inactive")
//            doLockedSignalCompleted()
//        }
    }

    // assert: mutex.isLocked()
    private fun doLockedSignalCompleted() {
        try {
            if (_nRequested.value >= CLOSED) {
                _nRequested.value = SIGNALLED // we'll signal onError/onCompleted (that the final state -- no CAS needed)
                val cause = getCompletionCause()
                try {
                    if (cause != null)
                        subscriber.onError(cause)
                    else
                        subscriber.onComplete()
                } catch (e: Throwable) {
                    handleCoroutineException(coroutineContext, e)
                }
            }
        } finally {
            mutex.unlock()
        }
    }

    override fun request(n: Long) {
        if (n < 0) {
            cancel(IllegalArgumentException("Must request non-negative number, but $n requested"))
            return
        }
        while (true) { // lock-free loop for nRequested
            val cur = _nRequested.value
            if (cur < 0) return // already closed for send, ignore requests
            var upd = cur + n
            if (upd < 0 || n == Long.MAX_VALUE)
                upd = Long.MAX_VALUE
            if (cur == upd) return // nothing to do
            if (_nRequested.compareAndSet(cur, upd)) {
                // unlock the mutex when we don't have back-pressure anymore
                if (cur == 0L) {
                    mutex.unlock()
                    // recheck isActive
                    if (!isActive && mutex.tryLock()) {
                        println("call doLockedSignalCompleted from request($n), we don't have back-pressure anymore")
                        doLockedSignalCompleted()
                    }
                }
                return
            }
        }
    }

    override fun onCancellation() {
        while (true) { // lock-free loop for nRequested
            val cur = _nRequested.value
            if (cur == SIGNALLED) return // some other thread holding lock already signalled cancellation/completion
            check(cur >= 0) // no other thread could have marked it as CLOSED, because onCancellation is invoked once
            if (!_nRequested.compareAndSet(cur, CLOSED)) continue // retry on failed CAS
            // Ok -- marked as CLOSED, now can unlock the mutex if it was locked due to backpressure
            if (cur == 0L) {
                println("call doLockedSignalCompleted from onCancellation, marked as CLOSED, now can unlock the mutex if it was locked due to backpressure")
                doLockedSignalCompleted()
            } else {
                // otherwise mutex was either not locked or locked in concurrent onNext... try lock it to signal completion
                if (mutex.tryLock()) {
                    println("call doLockedSignalCompleted from onCancellation, mutex was either not locked or locked in concurrent onNext... try lock it to signal completion")
                    doLockedSignalCompleted()
                }
                // Note: if failed `tryLock`, then `doLockedNext` will signal after performing `unlock`
            }
            return // done anyway
        }
    }

    // Subscription impl
    override fun cancel() {
        cancel(cause = null)
    }
}