package reactivity

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.internal.util.errorInOnSubscribe
import reactivity.internal.util.onErrorDropped
import reactivity.internal.util.onNextDropped
import reactivity.internal.util.onOperatorError
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater

internal class DelegatedSubscriber<T> internal constructor(val parent: DelegatedPublisher<T>,
                                                           val actual: Subscriber<in T>) : Subscription, Subscriber<T> {

    lateinit var subscription: Subscription
    @Volatile
    var done: Boolean = false
    @Volatile
    var once: Int = 0

    val ONCE: AtomicIntegerFieldUpdater<DelegatedSubscriber<*>> = AtomicIntegerFieldUpdater.newUpdater<DelegatedSubscriber<*>>(DelegatedSubscriber::class.java, "once")

    // Subscription functions
    override fun request(n: Long) {
        try {
            parent.onRequestBlock?.invoke(n)
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e))
            return
        }
        subscription.request(n)
    }

    override fun cancel() {
        try {
            parent.onCancelBlock?.invoke()
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e))
            return
        }
        subscription.cancel()

        if (parent.finallyBlock != null) runFinally()
    }

    // Subscriber functions
    override fun onSubscribe(s: Subscription) {
        try {
            parent.onSubscribeBlock?.invoke(s)
        } catch (e: Throwable) {
            errorInOnSubscribe(actual, onOperatorError(s, e))
            return
        }
        subscription = s
        actual.onSubscribe(this)
    }

    override fun onNext(t: T) {
        if (done) {
            onNextDropped(t)
            return
        }
        try {
            parent.onNextBlock?.invoke(t)
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e, t))
            return
        }
        actual.onNext(t)
    }

    override fun onError(t: Throwable) {
        if (done) {
            onErrorDropped(t)
            return
        }
        done = true
        var throwable = t
        if (parent.onErrorBlock != null) {
            Exceptions.throwIfFatal(t)
            try {
                parent.onErrorBlock?.invoke(t)
            } catch (e: Throwable) {
                //this performs a throwIfFatal or suppresses t in e
                throwable = onOperatorError(null, e, t)
            }
        }
        try {
            actual.onError(throwable)
        } catch (use: UnsupportedOperationException) {
            if (parent.onErrorBlock == null || !Exceptions.isErrorCallbackNotImplemented(use) && use.cause !== throwable) {
                throw use
            }
        }

        if (parent.finallyBlock != null) runFinally()
    }

    override fun onComplete() {
        if (done) {
            return
        }
        try {
            parent.onCompleteBlock?.invoke()
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e))
            return
        }
        done = true

        actual.onComplete()

        if (parent.finallyBlock != null) runFinally()
    }

    private fun runFinally() {
        if (ONCE.compareAndSet(this, 0, 1)) {
            try {
                parent.finallyBlock?.invoke()
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                onErrorDropped(e)
            }
        }
    }

}