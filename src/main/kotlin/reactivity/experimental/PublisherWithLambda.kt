package reactivity.experimental

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.experimental.internal.util.cancelledSubscription
import reactivity.experimental.internal.util.onErrorDropped
import reactivity.experimental.internal.util.validateSubscription
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * This is the interface declaring the lambda functions
 * related to each functions of [Subscriber]
 * will be implemented in both [Multi] and [Solo]
 */
interface WithLambda<T> {
    fun subscribe(onNext: (T) -> Unit): Disposable

    fun subscribe(onNext: ((T) -> Unit)?, onError: (Throwable) -> Unit): Disposable

    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?): Disposable

    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?,
                  onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): Disposable
}

internal class SubscriberLambda<T>(private val onNext: ((T) -> Unit)? = null, private val onError: ((Throwable) -> Unit)? = null,
                                   private val onComplete: (() -> Unit)? = null, private val onSubscribe: ((Subscription) -> Unit)? = null)
    : Subscriber<T>, Disposable {

    @Volatile
    @JvmField
    var subscription: Subscription? = null
    val S: AtomicReferenceFieldUpdater<SubscriberLambda<*>, Subscription> = AtomicReferenceFieldUpdater.newUpdater(SubscriberLambda::class.java,
            Subscription::class.java,
            "subscription")

    override fun onSubscribe(s: Subscription) {
        if (validateSubscription(subscription, s)) {
            this.subscription = s
            try {
                this.onSubscribe?.invoke(s) ?: s.request(java.lang.Long.MAX_VALUE)
            } catch (t: Throwable) {
                Exceptions.throwIfFatal(t)
                s.cancel()
                onError(t)
            }
        }
    }

    override fun onComplete() {
        val s = S.getAndSet(this, cancelledSubscription())
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
        val s = S.getAndSet(this, cancelledSubscription())
        if (s === cancelledSubscription()) {
            onErrorDropped(t)
            return
        }
        this.onError?.invoke(t) ?: throw Exceptions.errorCallbackNotImplemented(t)
    }

    override fun onNext(item: T) {
        try {
            this.onNext?.invoke(item)
        } catch (t: Throwable) {
            Exceptions.throwIfFatal(t)
            this.subscription?.cancel()
            onError(t)
        }
    }

    override fun isDisposed(): Boolean {
        return subscription === cancelledSubscription()
    }

    override fun dispose() {
        val s = S.getAndSet(this, cancelledSubscription())
        if (s != null && s !== cancelledSubscription()) {
            s.cancel()
        }
    }
}