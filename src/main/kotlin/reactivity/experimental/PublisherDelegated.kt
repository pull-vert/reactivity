package reactivity.experimental

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

/**
 * This is the base interface for [MultiImpl] and [AbstractSolo]
 */
internal interface PublisherDelegated<T> : Publisher<T> {
    val delegate: Publisher<T>

    override fun subscribe(s: Subscriber<in T>) {
        delegate.subscribe(s)
    }
}