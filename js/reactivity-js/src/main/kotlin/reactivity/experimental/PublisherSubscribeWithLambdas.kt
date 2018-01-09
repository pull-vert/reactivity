package reactivity.experimental

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

    var _subscription: Subscription? = null

    override fun onSubscribe(s: Subscription) {
        if (validateSubscription(_subscription, s)) {
            _subscription = s
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
        if (_subscription === cancelledSubscription()) return
        _subscription = cancelledSubscription()
        try {
            this.onComplete?.invoke()
        } catch (t: Throwable) {
            Exceptions.throwIfFatal(t)
            onError(t)
        }
    }

    override fun onError(t: Throwable) {
        if (_subscription === cancelledSubscription()) {
            onErrorDropped(t)
            return
        }
        _subscription = cancelledSubscription()
        this.onError?.invoke(t) ?: throw Exceptions.errorCallbackNotImplemented(t)
    }

    override fun onNext(t: T) {
        try {
            onNext?.invoke(t)
        } catch (th: Throwable) {
            Exceptions.throwIfFatal(th)
            _subscription?.cancel()
            onError(th)
        }
    }

    override fun dispose() {
        if (_subscription != null && _subscription !== cancelledSubscription()) {
            _subscription?.cancel()
        }
        _subscription = cancelledSubscription()
    }
}