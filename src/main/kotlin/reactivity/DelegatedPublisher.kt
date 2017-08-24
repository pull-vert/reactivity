package reactivity

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * This is the abstract Super class for [Multi] and [Solo]
 */
internal abstract class DelegatedPublisher<T> : Publisher<T> {
    internal abstract val delegate: Publisher<T>

    override fun subscribe(s: Subscriber<in T>) {
        val hasCallback = onSubscribeBlock != null || onNextBlock != null
            || onErrorBlock != null || onCompleteBlock != null
            || onCancelBlock != null || onRequestBlock != null
            || finallyBlock != null
        if (!hasCallback) delegate.subscribe(s)
        else delegate.subscribe(DelegatedSubscriber(this, s))
    }

    // callbacks
    internal var onSubscribeBlock: ((Subscription) -> Unit)? = null
    internal var onNextBlock: ((T) -> Unit)? = null
    internal var onErrorBlock: ((Throwable) -> Unit)? = null
    internal var onCompleteBlock: (() -> Unit)? = null
    internal var onCancelBlock: (() -> Unit)? = null
    internal var onRequestBlock: ((Long) -> Unit)? = null
    internal var finallyBlock: (() -> Unit)? = null
}