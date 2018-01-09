package reactivity.experimental

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.experimental.DisposableHandle

interface SubscribeWith<T> : Publisher<T> {
    /**
     * Subscribe the given [Subscriber] to this [Publisher] and return said
     * [Subscriber] (eg. a [SubscriberLambda]).
     *
     * @param subscriber the [Subscriber] to subscribe with
     * @param E the reified type of the [Subscriber] for chaining
     *
     * @return the passed [Subscriber] after subscribing it to this [Publisher]
    </E> */
    fun <E : Subscriber<in T>> subscribeWith(subscriber: E): E {
        subscribe(subscriber)
        return subscriber
    }
}

actual interface WithLambdas<T>  {
    actual fun subscribe(): DisposableHandle
    actual fun subscribe(onNext: (T) -> Unit): DisposableHandle
    actual fun subscribe(onNext: ((T) -> Unit)?, onError: (Throwable) -> Unit): DisposableHandle
    actual fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?): DisposableHandle
}

interface WithLambdaImpl<T> : WithLambdas<T>, SubscribeWith<T> {
    override fun subscribe(): DisposableHandle {
        return subscribeWith(SubscriberLambda())
    }
    override fun subscribe(onNext: (T) -> Unit): DisposableHandle {
        return subscribeWith(SubscriberLambda(onNext))
    }
    override fun subscribe(onNext: ((T) -> Unit)?, onError: (Throwable) -> Unit): DisposableHandle {
        return subscribeWith(SubscriberLambda(onNext, onError))
    }
    override fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?): DisposableHandle {
        return subscribeWith(SubscriberLambda(onNext, onError, onComplete))
    }
}

internal class SubscriberLambda<T>(private val onNext: ((T) -> Unit)? = null,
                                  private val onError: ((Throwable) -> Unit)? = null,
                                  private val onComplete: (() -> Unit)? = null,
                                  private val onSubscribe: ((Subscription) -> Unit)? = null)
    : Subscriber<T>, DisposableHandle {

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
        if (s === cancelledSubscription()) return
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

    override fun dispose() {
        val s = _subscription.getAndSet(cancelledSubscription())
        if (s != null && s !== cancelledSubscription()) s.cancel()
    }
}