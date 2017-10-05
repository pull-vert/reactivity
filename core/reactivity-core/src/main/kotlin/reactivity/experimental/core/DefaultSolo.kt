package reactivity.experimental.core

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.newCoroutineContext
import kotlinx.coroutines.experimental.reactive.awaitSingle
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import reactivity.experimental.core.internal.coroutines.consumeUnique
import java.io.Closeable
import kotlin.coroutines.experimental.startCoroutine

/**
 * Creates cold reactive [SoloPublisher] that runs a given [block] in a coroutine.
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
// TODO provide a custom ProducerScope impl that checks send is only called once, and throws exception otherwise !
// TODO after that remove SoloCoroutine
fun <T> defaultSoloPublisher(
        scheduler: Scheduler,
        block: suspend ProducerScope<T>.() -> Unit
): DefaultSolo<T> = DefaultSoloImpl(Publisher { subscriber ->
    val newContext = newCoroutineContext(scheduler.context)
    val coroutine = SoloCoroutine(newContext, subscriber)
    coroutine.initParentJob(scheduler.context[Job])
    subscriber.onSubscribe(coroutine) // do it first (before starting coroutine), to avoid unnecessary suspensions
    block.startCoroutine(coroutine, coroutine)
}, scheduler)

///**
// * Singleton builder for [Solo], a single (or empty) value Reactive Stream [Publisher]
// *
// * @author Frédéric Montariol
// */
//object SoloBuilder : SoloPublisherBuilder() {
//    @JvmStatic
//    fun <T> fromValue(value: T): SoloPublisher<T> = Companion.fromValue(value)
//
//    @JvmStatic
//    fun <T> fromValue(scheduler: Scheduler, value: T): SoloPublisher<T> = Companion.fromValue(scheduler, value)
//}

/**
 * Builder for [DefaultSoloImpl], a single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
abstract class SoloPublisherBuilder {
    // protected Static factory methods to create a DefaultSoloImpl
    protected companion object {
        fun <T> fromValue(value: T) = fromValue(SECHEDULER_DEFAULT_DISPATCHER, value)

        fun <T> fromValue(scheduler: Scheduler, value: T) = defaultSoloPublisher(scheduler) {
            send(value)
        }
    }
}

/**
 * Subscribes to this [SoloPublisher] and performs the specified action for the unique received element.
 */
inline suspend fun <T> DefaultSolo<T>.consumeUnique(action: (T) -> Unit) {
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
 * Single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
interface DefaultSolo<T> : PublisherCommons<T> {

    val delegate: Publisher<T>
    override val initialScheduler: Scheduler

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): DefaultSolo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return DefaultSoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnNext(onNext: (T) -> Unit): DefaultSolo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onNextBlock = onNext
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return DefaultSoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnError(onError: (Throwable) -> Unit): DefaultSolo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onErrorBlock = onError
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return DefaultSoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnComplete(onComplete: () -> Unit): DefaultSolo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onCompleteBlock = onComplete
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return DefaultSoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnCancel(onCancel: () -> Unit): DefaultSolo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onCancelBlock = onCancel
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return DefaultSoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): DefaultSolo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onRequestBlock = onRequest
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return DefaultSoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doFinally(finally: () -> Unit): DefaultSolo<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).finallyBlock = finally
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return DefaultSoloImpl(publisherCallbacks, initialScheduler)
    }

    /**
     * Returns a [Solo][reactivity.experimental.Solo] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    override fun publishOn(delayError: Boolean) = publishOn(initialScheduler, delayError)

    /**
     * Returns a [Solo][reactivity.experimental.Solo] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    override fun publishOn(scheduler: Scheduler, delayError: Boolean) = defaultSoloPublisher(scheduler) {
        val completableConsumer = SoloPublishOn<T>(delayError)
        this@DefaultSolo.subscribe(completableConsumer)
        completableConsumer.consumeUnique {
            send(it)
        }
    }
}

private class DefaultSoloImpl<T>(override val delegate: Publisher<T>,
                                 override val initialScheduler: Scheduler)
    : DefaultSolo<T>, Publisher<T> by delegate