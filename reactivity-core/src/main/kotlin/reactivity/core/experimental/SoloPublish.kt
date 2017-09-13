package reactivity.core.experimental

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.selects.SelectInstance
import kotlinx.coroutines.experimental.sync.Mutex
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
        block: suspend CoroutineScope.() -> T
) : Solo<T> = SoloImpl { subscriber ->
        val newContext = newCoroutineContext(scheduler.context)
    val coroutine = SoloCoroutine(newContext, subscriber)
    coroutine.initParentJob(scheduler.context[Job])
    subscriber.onSubscribe(coroutine) // do it first (before starting coroutine), to avoid unnecessary suspensions
    block.startCoroutine(coroutine, coroutine)
}

private class SoloCoroutine<T>(
        parentContext: CoroutineContext,
        private val subscriber: Subscriber<T>
) : AbstractCoroutine<T>(parentContext, true), Subscription {

    // Mutex is locked when either nRequested == 0 or while subscriber.onXXX is being invoked
    private val mutex = Mutex(locked = true)

    var disposed = false

    @Suppress("UNCHECKED_CAST")
    override fun afterCompletion(state: Any?, mode: Int) {
        when {
            disposed                        -> {}
            state is CompletedExceptionally -> subscriber.onError(state.exception)
            state != null                   -> {
                mutex.lock()
                subscriber.onNext(state as T)
                subscriber.onComplete()
            }
            else                            -> {
                subscriber.onNext(null)
                subscriber.onComplete()
            }
        }
    }

    override fun cancel() {
        disposed = true
        cancel(cause = null)
    }

    override fun request(n: Long) {
        if (n < 0) {
            cancel(IllegalArgumentException("Must request non-negative number, but $n requested"))
            return
        }
        // nothing to do, just one item to produce
    }
}

//private const val CLOSED_MESSAGE = "This subscription had already closed (completed or failed)"
//private const val CLOSED = -1L    // closed, but have not signalled onCompleted/onError yet
//private const val SIGNALLED = -2L  // already signalled subscriber onCompleted/onError
//
//private class SoloCoroutine<T>(
//        parentContext: CoroutineContext,
//        private val subscriber: Subscriber<T>
//) : AbstractCoroutine<T>(parentContext, true), CoroutineScope, Deferred<T?>, Subscription {
//
//    // Mutex is locked when either nRequested == 0 or while subscriber.onXXX is being invoked
//    private val mutex = Mutex(locked = true)
//
//    private val _nRequested = atomic(0L) // < 0 when closed (CLOSED or SIGNALLED)
//
//    override fun getCompleted(): T? {
//        if (!mutex.tryLock()) return null
//        val value = getCompletedInternal() as T
//        doLockedNext(value)
//        return value
//    }
//
//    suspend override fun await(): T {
//        // fast-path -- try send without suspension
//        return getCompleted() ?: awaitSuspend() // slow-path does suspend
//    }
//
//    private suspend fun awaitSuspend(): T {
//        val value = awaitInternal() as T
//        mutex.lock()
//        doLockedNext(value)
//        return value
//    }
//
//    override fun <R> registerSelectAwait(select: SelectInstance<R>, block: suspend (T?) -> R) =
//            mutex.registerSelectLock(select, null) {
//                val value = awaitInternal() as T
//                doLockedNext(value)
//                block(value)
//            }
//
//    private fun sendException() =
//            (state as? CompletedExceptionally)?.cause ?: CancellationException(CLOSED_MESSAGE)
//
//    // assert: mutex.isLocked()
//    private fun doLockedNext(elem: T) {
//        // check if already closed for send
//        if (!isActive) {
//            doLockedSignalCompleted()
//            throw sendException()
//        }
//        // notify subscriber
//        try {
//            subscriber.onNext(elem)
//        } catch (e: Throwable) {
//            try {
//                if (!cancel(e))
//                    handleCoroutineException(coroutineContext, e)
//            } finally {
//                doLockedSignalCompleted()
//            }
//            throw sendException()
//        }
//        // now update nRequested
//        while (true) { // lock-free loop on nRequested
//            val cur = _nRequested.value
//            if (cur < 0) break // closed from inside onNext => unlock
//            if (cur == Long.MAX_VALUE) break // no back-pressure => unlock
//            val upd = cur - 1
//            if (_nRequested.compareAndSet(cur, upd)) {
//                if (upd == 0L) return // return to keep locked due to back-pressure
//                break // unlock if upd > 0
//            }
//        }
//        /*
//           There is no sense to check for `isActive` before doing `unlock`, because cancellation/completion might
//           happen after this check and before `unlock` (see `onCancellation` that does not do anything
//           if it fails to acquire the lock that we are still holding).
//           We have to recheck `isActive` after `unlock` anyway.
//         */
//        mutex.unlock()
//        // recheck isActive
//        if (!isActive && mutex.tryLock())
//            doLockedSignalCompleted()
//    }
//
//    // assert: mutex.isLocked()
//    private fun doLockedSignalCompleted() {
//        try {
//            if (_nRequested.value >= CLOSED) {
//                _nRequested.value = SIGNALLED // we'll signal onError/onCompleted (that the final state -- no CAS needed)
//                val cause = getCompletionCause()
//                try {
//                    if (cause != null)
//                        subscriber.onError(cause)
//                    else
//                        subscriber.onComplete()
//                } catch (e: Throwable) {
//                    handleCoroutineException(coroutineContext, e)
//                }
//            }
//        } finally {
//            mutex.unlock()
//        }
//    }
//
//    override fun request(n: Long) {
//        if (n < 0) {
//            cancel(IllegalArgumentException("Must request non-negative number, but $n requested"))
//            return
//        }
//        while (true) { // lock-free loop for nRequested
//            val cur = _nRequested.value
//            if (cur < 0) return // already closed for send, ignore requests
//            var upd = cur + n
//            if (upd < 0 || n == Long.MAX_VALUE)
//                upd = Long.MAX_VALUE
//            if (cur == upd) return // nothing to do
//            if (_nRequested.compareAndSet(cur, upd)) {
//                // unlock the mutex when we don't have back-pressure anymore
//                if (cur == 0L) {
//                    mutex.unlock()
//                    // recheck isActive
//                    if (!isActive && mutex.tryLock())
//                        doLockedSignalCompleted()
//                }
//                return
//            }
//        }
//    }
//
//    override fun onCancellation() {
//        while (true) { // lock-free loop for nRequested
//            val cur = _nRequested.value
//            if (cur == SIGNALLED) return // some other thread holding lock already signalled cancellation/completion
//            check(cur >= 0) // no other thread could have marked it as CLOSED, because onCancellation is invoked once
//            if (!_nRequested.compareAndSet(cur, CLOSED)) continue // retry on failed CAS
//            // Ok -- marked as CLOSED, now can unlock the mutex if it was locked due to backpressure
//            if (cur == 0L) {
//                doLockedSignalCompleted()
//            } else {
//                // otherwise mutex was either not locked or locked in concurrent onNext... try lock it to signal completion
//                if (mutex.tryLock())
//                    doLockedSignalCompleted()
//                // Note: if failed `tryLock`, then `doLockedNext` will signal after performing `unlock`
//            }
//            return // done anyway
//        }
//    }
//
//    // Subscription impl
//    override fun cancel() {
//        cancel(cause = null)
//    }
//}