package reactivity.experimental

import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.reactive.publish
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlinx.coroutines.experimental.reactive.consumeEach

fun <T> multi(
        context: CoroutineContext,
        block: suspend ProducerScope<T>.() -> Unit
): Multi<T> = MultiImpl(publish(context, block))

abstract class Multi<T> : Publisher<T>, PublisherCommons<T>, WithCallbacks<T>, WithLambda<T>, WithPublishOn {
    companion object {
        @JvmStatic
        fun range(start: Int, count: Int,
                  context: CoroutineContext = EmptyCoroutineContext
        ) = multi(context) {
            for (x in start until start + count) send(x)
        }
    }

    // functions from WithCallbacks
    override abstract fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>

    override abstract fun doOnNext(onNext: (T) -> Unit): Multi<T>
    override abstract fun doOnError(onError: (Throwable) -> Unit): Multi<T>
    override abstract fun doOnComplete(onComplete: () -> Unit): Multi<T>
    override abstract fun doOnCancel(onCancel: () -> Unit): Multi<T>
    override abstract fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>
    override abstract fun doFinally(finally: () -> Unit): Multi<T>

    // functions from WithPublishOn
    override abstract fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T>
}

internal class MultiImpl<T> internal constructor(override val delegate: Publisher<T>) : Multi<T>(), PublisherDelegated<T> {

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnNext(onNext: (T) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onNextBlock = onNext
            return this
        }
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onErrorBlock = onError
            return this
        }
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnComplete(onComplete: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCompleteBlock = onComplete
            return this
        }
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnCancel(onCancel: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCancelBlock = onCancel
            return this
        }
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onRequestBlock = onRequest
            return this
        }
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return MultiImpl(publisherCallbacks)
    }

    override fun doFinally(finally: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.finallyBlock = finally
            return this
        }
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return MultiImpl(publisherCallbacks)
    }

    override fun subscribe(onNext: (T) -> Unit): Disposable {
        return subscribeWith(SubscriberLambda(onNext))
    }

    override fun subscribe(onNext: ((T) -> Unit)?, onError: (Throwable) -> Unit): Disposable {
        return subscribeWith(SubscriberLambda(onNext, onError))
    }

    override fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?): Disposable {
        return subscribeWith(SubscriberLambda(onNext, onError, onComplete))
    }

    override fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): Disposable {
        return subscribeWith(SubscriberLambda(onNext, onError, onComplete, onSubscribe))
    }

    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T> {
        return multi(scheduler.context) {
            val channel = SubscriberPublishOn<T>(delayError, prefetch)
            this@MultiImpl.subscribe(channel)
            channel.use { chan ->
                var count = 0
                for (x in chan) {
                    count++
                    send(x)
                    if (count == prefetch) {
                        count = 0
                        channel.subscription?.request(prefetch.toLong())
                    }
                }
            }
        }
    }

    fun <R> fusedFilterMap(
            context: CoroutineContext,   // the context to execute this coroutine in
            predicate: (T) -> Boolean,   // the filter predicate
            mapper: (T) -> R             // the mapper function
    ) = multi(context) {
        consumeEach {                // consume the source stream
            if (predicate(it))       // filter part
                send(mapper(it))     // map part
        }
    }
}