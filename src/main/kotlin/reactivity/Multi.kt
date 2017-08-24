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
): Multi<T> = object : AbstractMulti<T>(publish(context, block)) {}

interface Multi<T> : Publisher<T>, SubscriberSubscriptionCallbacks<T> {
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

internal abstract class AbstractMulti<T> internal constructor(override val delegate: Publisher<T>) : DelegatedPublisher<T>(), Multi<T> {
    override fun doOnSubscribe(block: (Subscription) -> Unit): Multi<T> {
        this.onSubscribeBlock = block
        return this
    }

    override fun doOnNext(block: (T) -> Unit): Multi<T> {
        this.onNextBlock = block
        return this
    }

    override fun doOnError(block: (Throwable) -> Unit): Multi<T> {
        this.onErrorBlock = block
        return this
    }

    override fun doOnComplete(block: () -> Unit): Multi<T> {
        this.onCompleteBlock = block
        return this
    }

    override fun doOnCancel(block: () -> Unit): Multi<T> {
        this.onCancelBlock = block
        return this
    }

    override fun doOnRequest(block: (Long) -> Unit): Multi<T> {
        this.onRequestBlock = block
        return this
    }

    override fun doFinally(block: () -> Unit): Multi<T> {
        this.finallyBlock = block
        return this
    }
}