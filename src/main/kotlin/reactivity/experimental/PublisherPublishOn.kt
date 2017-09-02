package reactivity.experimental

import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactivity.experimental.internal.util.validateSubscription

/**
 * This is the interface declaring the publishOn functions
 * for executing the subscriber in other context than the publisher
 * will be implemented in both [Multi] and [Solo]
 */
interface WithPublishOn {
    fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): WithPublishOn
}

internal class SubscriberPublishOn<T> internal constructor(val delayError: Boolean, val prefetch: Int) :
        LinkedListChannel<T>(), SubscriptionReceiveChannel<T>, Subscriber<T> {

    @Volatile
    @JvmField
    var subscription: Subscription? = null

    override fun onCancelledReceive() {
        println("SubscriberPublishOn nCancelledReceive")
    }

    override fun afterClose(cause: Throwable?) {
        println("SubscriberPublishOn afterClose")
        subscription?.cancel()
    }

    // Subscriber functions
    override fun onSubscribe(s: Subscription) {
        println("SubscriberPublishOn onSubscribe")
        if (validateSubscription(subscription, s)) {
            subscription = s
            initialRequest()
        }
    }

    private fun initialRequest() {
        println("SubscriberPublishOn initialRequest " + prefetch)
        // In this function we need that the subscription is not null, so use of !!
        if (prefetch == Integer.MAX_VALUE) {
            subscription!!.request(java.lang.Long.MAX_VALUE)
        } else {
            subscription!!.request(prefetch.toLong())
        }
    }

    override fun onNext(t: T) {
        println("SubscriberPublishOn onNext " + t)
        offer(t)
    }

    override fun onError(t: Throwable) {
        println("SubscriberPublishOn onError" + t)
        close(cause = t)
    }

    override fun onComplete() {
        println("SubscriberPublishOn onComplete")
        close(cause = null)
    }

    // Subscription overrides
    override fun close() {
        close(cause = null)
    }
}