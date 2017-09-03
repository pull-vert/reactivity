package reactivity.core.experimental

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

/**
 * Common functions for [Multi] and [Solo]
 */
interface PublisherCommons<T> : Publisher<T> {
    /**
     * Subscribe the given [Subscriber] to this [Publiser] and return said
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