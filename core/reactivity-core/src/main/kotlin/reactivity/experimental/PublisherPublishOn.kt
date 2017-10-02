package reactivity.experimental

import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.experimental.internal.coroutines.CompletableConsumerImpl
import reactivity.experimental.internal.util.validateSubscription

/**
 * This is the interface declaring the publishOn functions
 * for executing the subscriber in other context than the publisher
 * will be implemented in both [Multi] and [SoloPublisher]
 */
interface WithPublishOn {
    fun publishOn(delayError: Boolean): WithPublishOn
    fun publishOn(scheduler: Scheduler, delayError: Boolean): WithPublishOn
}

internal class MultiPublishOn<T> internal constructor(val delayError: Boolean, val prefetch: Int) :
        LinkedListChannel<T>(), SubscriptionReceiveChannel<T>, Subscriber<T> {

    @Volatile
    @JvmField
    var subscription: Subscription? = null

    override fun afterClose(cause: Throwable?) {
        println("MultiPublishOn afterClose")
        subscription?.cancel()
    }

    // Subscriber functions
    override fun onSubscribe(s: Subscription) {
        println("MultiPublishOn onSubscribe")
        if (validateSubscription(subscription, s)) {
            subscription = s
            initialRequest()
        }
    }

    private fun initialRequest() {
        println("MultiPublishOn initialRequest " + prefetch)
        // In this function we need that the subscription is not null, so use of !!
        if (prefetch == Integer.MAX_VALUE) {
            subscription!!.request(Long.MAX_VALUE)
        } else {
            subscription!!.request(prefetch.toLong())
        }
    }

    override fun onNext(t: T) {
        println("MultiPublishOn onNext " + t)
        offer(t)
    }

    override fun onError(t: Throwable) {
        println("MultiPublishOn onError" + t)
        close(cause = t)
    }

    override fun onComplete() {
        println("MultiPublishOn onComplete")
        close(cause = null)
    }

    // Subscription overrides
    override fun close() {
        close(cause = null)
    }
}

internal class SoloPublishOn<T> internal constructor(val delayError: Boolean) :
    CompletableConsumerImpl<T>(), DeferredCloseable<T>, Subscriber<T> {

    @Volatile
    @JvmField
    var subscription: Subscription? = null

    override fun afterClose(cause: Throwable?) {
        println("SoloPublishOn afterClose")
        subscription?.cancel()
    }

    // Subscriber functions
    override fun onSubscribe(s: Subscription) {
        println("SoloPublishOn onSubscribe")
        if (validateSubscription(subscription, s)) {
            subscription = s
            initialRequest()
        }
    }

    private fun initialRequest() {
        println("SoloPublishOn initialRequest ")
        subscription!!.request(Long.MAX_VALUE)
    }

    override fun onNext(t: T) {
        println("SoloPublishOn onNext " + t)
        complete(t)
    }

    override fun onError(t: Throwable) {
        println("SoloPublishOn onError" + t)
        close(cause = t)
    }

    override fun onComplete() {
        println("SoloPublishOn onComplete")
        close(cause = null)
    }

    // Subscription overrides
    override fun close() {
        close(cause = null)
    }
}