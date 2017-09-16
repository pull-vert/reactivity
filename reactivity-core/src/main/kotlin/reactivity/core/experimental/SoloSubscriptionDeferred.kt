package reactivity.core.experimental

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop
import kotlinx.coroutines.experimental.Deferred
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.core.experimental.coroutines.CompletableConsumerImpl
import java.io.Closeable

/**
 * Subscribes to this [Solo] and returns a deferred to receive one element emitted by it.
 * The resulting channel shall be [closed][DeferredCloseable.close] to unsubscribe from this publisher.
 */
fun <T> Solo<T>.openDeferred(): DeferredCloseable<T> {
    val producerDeferred = CompletableConsumerImplCloseable<T>()
    subscribe(producerDeferred)
    return producerDeferred
}

/**
 * Subscribes to this [Solo] and performs the specified action for the unique received element.
 */
inline suspend fun <T> Solo<T>.consumeUnique(action: (T) -> Unit) {
    openDeferred().use { deferred ->
        action(deferred.await())
    }
}

/**
 * Return type for [Solo.openDeferred] that can be used to [await] elements from the
 * open producer and to [close] it to unsubscribe.
 */
interface DeferredCloseable<out T> : Deferred<T>, Closeable {
    /**
     * Closes this deferred.
     */
    override fun close()
}

private class CompletableConsumerImplCloseable<T> : CompletableConsumerImpl<T>(), DeferredCloseable<T>, Subscriber<T> {
    @Volatile
    @JvmField
    var subscription: Subscription? = null

    // request balance from cancelled receivers, balance is negative if we have receivers, but no subscription yet
    val _balance = atomic(0)

    // AbstractChannel overrides
    override fun onAwait() {
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

    override fun onCancelledAwait() {
        _balance.incrementAndGet()
    }

    override fun afterClose(cause: Throwable?) {
        subscription?.cancel()
    }

    // Closeable overrides
    override fun close() {
        close(cause = null)
    }

    // Subscriber overrides
    override fun onSubscribe(s: Subscription) {
        subscription = s
        while (true) { // lock-free loop on balance
            if (isClosed) {
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
        complete(t)
    }

    override fun onComplete() {
        close(cause = null)
    }

    override fun onError(e: Throwable) {
        completeExceptionally(e)
    }
}
