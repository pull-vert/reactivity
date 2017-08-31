package reactivity.experimental

import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import org.reactivestreams.Subscriber

abstract class AbstractSubscriptionChannel<T> : LinkedListChannel<T>(), SubscriptionReceiveChannel<T>, Subscriber<T> {

    // Subscription overrides
    override fun close() {
        close(cause = null)
    }
}