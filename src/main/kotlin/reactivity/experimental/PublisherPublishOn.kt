package reactivity.experimental

import kotlinx.coroutines.experimental.launch
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * This is the interface declaring the publishOn functions
 * for executing the subscriber in other context than the publisher
 * will be implemented in both [Multi] and [Solo]
 */
interface WithPublishOn {
    fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): WithPublishOn
}

internal class PublisherWithPublishOn<T> internal constructor(override val delegate: Publisher<T>, val scheduler: Scheduler, val delayError: Boolean, val prefetch: Int) : PublisherDelegated<T> {

    override fun subscribe(s: Subscriber<in T>) {
        delegate.subscribe(SubscriberPublishOn(s, scheduler, delayError, prefetch))
    }
}

private class SubscriberPublishOn<T> internal constructor(val actual: Subscriber<in T>, val scheduler: Scheduler, val delayError: Boolean, val prefetch: Int) : Subscriber<T> {

    // Subscriber functions
    override fun onSubscribe(s: Subscription) {
        launch(scheduler.context) {
            actual.onSubscribe(s)
            s.request(prefetch.toLong())
        }
    }

    override fun onNext(t: T) {
        actual.onNext(t)
    }

    override fun onError(t: Throwable) {
        actual.onError(t)
    }

    override fun onComplete() {
        actual.onComplete()
    }
}