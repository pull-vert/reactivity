package reactivity

import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.reactive.publish
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

fun <T> multi(
        context: CoroutineContext,
        block: suspend ProducerScope<T>.() -> Unit
): Multi<T> = MultiImpl(publish(context, block))

interface Multi<T> : Publisher<T>, WithCallbacks<T> {
    companion object {
        fun range(start: Int, count: Int, context: CoroutineContext = EmptyCoroutineContext): Multi<Int> = multi(context) {
            for (x in start until start + count) send(x)
        }
    }

    override fun doOnSubscribe(block: (Subscription) -> Unit): Multi<T>

    override fun doOnNext(block: (T) -> Unit): Multi<T>

    override fun doOnError(block: (Throwable) -> Unit): Multi<T>

    override fun doOnComplete(block: () -> Unit): Multi<T>

    override fun doOnCancel(block: () -> Unit): Multi<T>

    override fun doOnRequest(block: (Long) -> Unit): Multi<T>

    override fun doFinally(block: () -> Unit): Multi<T>
}

internal class MultiImpl<T> internal constructor(override val delegate: Publisher<T>) : PublisherDelegated<T>, Multi<T> {
    override fun doOnSubscribe(block: (Subscription) -> Unit): Multi<T> {
        if (delegate is PublisherCallbacks) {
            delegate.onSubscribeBlock = block
            return this
        }
        // otherwise this is not a PublisherCallbacks
        val publisherCallbacks = PublisherCallbacks(this)
        publisherCallbacks.onSubscribeBlock = block
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnNext(block: (T) -> Unit): Multi<T> {
        if (delegate is PublisherCallbacks) {
            delegate.onNextBlock = block
            return this
        }
        val publisherCallbacks = PublisherCallbacks(this)
        publisherCallbacks.onNextBlock = block
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnError(block: (Throwable) -> Unit): Multi<T> {
        if (delegate is PublisherCallbacks) {
            delegate.onErrorBlock = block
            return this
        }
        val publisherCallbacks = PublisherCallbacks(this)
        publisherCallbacks.onErrorBlock = block
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnComplete(block: () -> Unit): Multi<T> {
        if (delegate is PublisherCallbacks) {
            delegate.onCompleteBlock = block
            return this
        }
        val publisherCallbacks = PublisherCallbacks(this)
        publisherCallbacks.onCompleteBlock = block
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnCancel(block: () -> Unit): Multi<T> {
        if (delegate is PublisherCallbacks) {
            delegate.onCancelBlock = block
            return this
        }
        val publisherCallbacks = PublisherCallbacks(this)
        publisherCallbacks.onCancelBlock = block
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnRequest(block: (Long) -> Unit): Multi<T> {
        if (delegate is PublisherCallbacks) {
            delegate.onRequestBlock = block
            return this
        }
        val publisherCallbacks = PublisherCallbacks(this)
        publisherCallbacks.onRequestBlock = block
        return MultiImpl(publisherCallbacks)
    }

    override fun doFinally(block: () -> Unit): Multi<T> {
        if (delegate is PublisherCallbacks) {
            delegate.finallyBlock = block
            return this
        }
        val publisherCallbacks = PublisherCallbacks(this)
        publisherCallbacks.finallyBlock = block
        return MultiImpl(publisherCallbacks)
    }
}