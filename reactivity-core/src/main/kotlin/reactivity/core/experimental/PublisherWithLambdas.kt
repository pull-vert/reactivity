package reactivity.core.experimental

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.core.experimental.internal.util.cancelledSubscription
import reactivity.core.experimental.internal.util.onErrorDropped
import reactivity.core.experimental.internal.util.validateSubscription

interface SubscribeWith<T> : Publisher<T> {
    /**
     * Subscribe the given [Subscriber] to this [Publisher] and return said
     * [Subscriber] (eg. a [SubscriberLambda]).
     *
     * @param subscriber the [Subscriber] to subscribe with
     * @param E the reified type of the [Subscriber] for chaining
     *
     * @return the passed [Subscriber] after subscribing it to this [Publiser]
    </E> */
    fun <E : Subscriber<in T>> subscribeWith(subscriber: E): E {
        subscribe(subscriber)
        return subscriber
    }
}

/**
 * This is the interface declaring the callback functions
 * related to each functions of [Subscriber] & [Subscription]
 * will be implemented in both [Multi] and [Solo]
 */
interface WithLambdas<T> : SubscribeWith<T> {
    // Methods for Publisher with lambdas
    fun subscribe(): Disposable {
        return subscribeWith(SubscriberLambda())
    }

    fun subscribe(onNext: (T) -> Unit): Disposable {
        return subscribeWith(SubscriberLambda(onNext))
    }

    fun subscribe(onNext: ((T) -> Unit)?, onError: (Throwable) -> Unit): Disposable {
        return subscribeWith(SubscriberLambda(onNext, onError))
    }

    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?): Disposable {
        return subscribeWith(SubscriberLambda(onNext, onError, onComplete))
    }

    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): Disposable {
        return subscribeWith(SubscriberLambda(onNext, onError, onComplete, onSubscribe))
    }
}

private class SubscriberLambda<T>(private val onNext: ((T) -> Unit)? = null,
                                  private val onError: ((Throwable) -> Unit)? = null,
                                  private val onComplete: (() -> Unit)? = null,
                                  private val onSubscribe: ((Subscription) -> Unit)? = null)
    : Subscriber<T>, Disposable {

    val _subscription: AtomicRef<Subscription?> = atomic(null)

    override fun onSubscribe(s: Subscription) {
        if (validateSubscription(_subscription.value, s)) {
            _subscription.compareAndSet(expect = null, update = s)
            try {
                this.onSubscribe?.invoke(s) ?: s.request(Long.MAX_VALUE)
            } catch (t: Throwable) {
                Exceptions.throwIfFatal(t)
                s.cancel()
                onError(t)
            }
        }
    }

    override fun onComplete() {
        val s = _subscription.getAndSet(cancelledSubscription())
        if (s === cancelledSubscription()) {
            return
        }
        try {
            this.onComplete?.invoke()
        } catch (t: Throwable) {
            Exceptions.throwIfFatal(t)
            onError(t)
        }
    }

    override fun onError(t: Throwable) {
        val s = _subscription.getAndSet(cancelledSubscription())
        if (s === cancelledSubscription()) {
            onErrorDropped(t)
            return
        }
        this.onError?.invoke(t) ?: throw Exceptions.errorCallbackNotImplemented(t)
    }

    override fun onNext(item: T) {
        try {
            onNext?.invoke(item)
        } catch (t: Throwable) {
            Exceptions.throwIfFatal(t)
            _subscription.value?.cancel()
            onError(t)
        }
    }

    override fun isDisposed(): Boolean {
        return _subscription.value === cancelledSubscription()
    }

    override fun dispose() {
        val s = _subscription.getAndSet(cancelledSubscription())
        if (s != null && s !== cancelledSubscription()) {
            s.cancel()
        }
    }
}