package reactivity.core.experimental

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.experimental.AbstractCoroutine
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.handleCoroutineException
import kotlinx.coroutines.experimental.selects.SelectInstance
import kotlinx.coroutines.experimental.sync.Mutex
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Scope for [solo] coroutine builder.
 */
//interface SoloProducerScope<in E> : CoroutineScope, Producer<E> {
//    /**
//     * A reference to the producer that this coroutine [produce][produce] elements to.
//     * It is provided for convenience, so that the code in the coroutine can refer
//     * to the producer as `producer` as apposed to `this`.
//     * All the [Producer] functions on this interface delegate to
//     * the producer instance returned by this function.
//     */
//    val producer: Producer<E>
//}

/**
 * Indicates attempt to [produce][Producer.produce] on [isClosedForProduce][Producer.isClosedForProduce] consumer
 * that was closed _normally_. A _failed_ consumer rethrows the original [close][Producer.close] cause
 * exception on send attempts.
 */
class ClosedProducerException(message: String?) : CancellationException(message)

private const val CLOSED_MESSAGE = "This subscription had already closed (completed or failed)"
private const val CLOSED = -1L    // closed, but have not signalled onCompleted/onError yet
private const val SIGNALLED = -2L  // already signalled subscriber onCompleted/onError

internal class SoloCoroutine<T>(
        parentContext: CoroutineContext,
        private val subscriber: Subscriber<T>
) : AbstractCoroutine<Unit>(parentContext, true), ProducerScope<T>, Subscription {

    override val channel: SendChannel<T> get() = this

    // Mutex is locked when either nRequested == 0 or while subscriber.onXXX is being invoked
    private val mutex = Mutex(locked = true)

    private val _nRequested = atomic(0L) // < 0 when closed (CLOSED or SIGNALLED)

    // Never Full ! Just one item to send
    override val isFull: Boolean get() = false
    override val isClosedForSend: Boolean get() = isCompleted
    override fun close(cause: Throwable?): Boolean = cancel(cause)

    private fun sendException() =
            (state as? CompletedExceptionally)?.cause ?: ClosedProducerException(CLOSED_MESSAGE)

    override fun offer(element: T): Boolean {
        if (!mutex.tryLock()) return false
        doLockedUnique(element)
        return true
    }

    suspend override fun send(element: T) {
        // fast-path -- try produce without suspension
        if (offer(element)) return
        // slow-path does suspend
        return sendSuspend(element)
    }

    private suspend fun sendSuspend(element: T) {
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

        /*
           There is no sense to check for `isActive` before doing `unlock`, because cancellation/completion might
           happen after this check and before `unlock` (see `onCancellation` that does not do anything
           if it fails to acquire the lock that we are still holding).
           We have to recheck `isActive` after `unlock` anyway.
         */
        mutex.unlock()
        // recheck isActive
        if (!isActive && mutex.tryLock()) {
            println("call doLockedSignalCompleted from doLockedUnique : coroutine is inactive")
            doLockedSignalCompleted()
        }
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

    override fun <R> registerSelectSend(select: SelectInstance<R>, element: T, block: suspend () -> R) =
            mutex.registerSelectLock(select, null) {
                doLockedUnique(element)
                block()
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