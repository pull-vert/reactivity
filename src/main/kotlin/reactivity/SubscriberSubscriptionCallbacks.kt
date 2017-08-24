package reactivity

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * This is the interface declaring the callback functions
 * related to each functions of [Subscriber] & [Subscription]
 * will be implemented in both [Multi] and [Solo]
 */
interface SubscriberSubscriptionCallbacks<T> {
    fun doOnSubscribe(block: (Subscription) -> Unit): SubscriberSubscriptionCallbacks<T>

    fun doOnNext(block: (T) -> Unit): SubscriberSubscriptionCallbacks<T>

    fun doOnError(block: (Throwable) -> Unit): SubscriberSubscriptionCallbacks<T>

    fun doOnComplete(block: () -> Unit): SubscriberSubscriptionCallbacks<T>

    fun doOnCancel(block: () -> Unit): SubscriberSubscriptionCallbacks<T>

    fun doOnRequest(block: (Long) -> Unit): SubscriberSubscriptionCallbacks<T>

    fun doFinally(block: () -> Unit): SubscriberSubscriptionCallbacks<T>
}