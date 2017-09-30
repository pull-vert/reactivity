package reactivity.experimental

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.newCoroutineContext
import kotlinx.coroutines.experimental.reactive.awaitSingle
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import reactivity.experimental.internal.coroutines.consumeUnique
import java.io.Closeable
import kotlin.coroutines.experimental.startCoroutine

/**
 * Creates cold reactive [Solo] that runs a given [block] in a coroutine.
 * Every time the returned publisher is subscribed, it starts a new coroutine in the specified [scheduler].
 * Coroutine emits items with `produce`. Unsubscribing cancels running coroutine.
 *
 * Invocation of `produce` is suspended appropriately when subscribers apply back-pressure and to ensure that
 * `onNext` is not invoked concurrently.
 *
 * | **Coroutine action**                         | **Signal to subscriber**
 * | -------------------------------------------- | ------------------------
 * | `value in end of coroutine is not null`      | `onNext`
 * | Normal completion or `close` without cause   | `onComplete`
 * | Failure with exception or `close` with cause | `onError`
 */
fun <T> solo(
        scheduler: Scheduler,
        block: suspend ProducerScope<T>.() -> Unit
): Solo<T> = SoloImpl(Publisher { subscriber ->
    val newContext = newCoroutineContext(scheduler.context)
    val coroutine = SoloCoroutine(newContext, subscriber)
    coroutine.initParentJob(scheduler.context[Job])
    subscriber.onSubscribe(coroutine) // do it first (before starting coroutine), to avoid unnecessary suspensions
    block.startCoroutine(coroutine, coroutine)
})

object SoloBuilder {
    // Static factory methods to create a Solo

    /**
     * Creates a [Solo] from a [value]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [value]
     */
    @JvmStatic
    fun <T> fromValue(value: T,
                  scheduler: Scheduler = Schedulers.emptyThreadContext()
    ) = solo(scheduler) {
        send(value)
    }
}

/**
 * Subscribes to this [Solo] and performs the specified action for the unique received element.
 */
inline suspend fun <T> Solo<T>.consumeUnique(action: (T) -> Unit) {
    action.invoke(awaitSingle())
}

/**
 * 2 in 1 Type that can be used to [await] elements from the
 * open producer and to [close] it to unsubscribe.
 */
interface DeferredCloseable<out T> : Deferred<T>, Closeable {
    /**
     * Closes this deferred.
     */
    override fun close()
}

/**
 * @author Frédéric Montariol
 */
interface Solo<T> : PublisherCommons<T> {
    // functions from WithCallbacks
    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>

    override fun doOnNext(onNext: (T) -> Unit): Solo<T>
    override fun doOnError(onError: (Throwable) -> Unit): Solo<T>
    override fun doOnComplete(onComplete: () -> Unit): Solo<T>
    override fun doOnCancel(onCancel: () -> Unit): Solo<T>
    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>
    override fun doFinally(finally: () -> Unit): Solo<T>

    // function from WithPublishOn
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>
}

internal class SoloImpl<T> internal constructor(private val delegate: Publisher<T>) : Solo<T>, Publisher<T> by delegate {
    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return SoloImpl(publisherCallbacks)
    }

    override fun doOnNext(onNext: (T) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onNextBlock = onNext
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return SoloImpl(publisherCallbacks)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onErrorBlock = onError
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return SoloImpl(publisherCallbacks)
    }

    override fun doOnComplete(onComplete: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCompleteBlock = onComplete
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return SoloImpl(publisherCallbacks)
    }

    override fun doOnCancel(onCancel: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCancelBlock = onCancel
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return SoloImpl(publisherCallbacks)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onRequestBlock = onRequest
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return SoloImpl(publisherCallbacks)
    }

    override fun doFinally(finally: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.finallyBlock = finally
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return SoloImpl(publisherCallbacks)
    }

    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T> {
        return solo(scheduler) {
            val completableConsumer = SoloPublishOn<T>(delayError)
            this@SoloImpl.subscribe(completableConsumer)
            completableConsumer.consumeUnique {
                send(it)
            }
        }
    }

}