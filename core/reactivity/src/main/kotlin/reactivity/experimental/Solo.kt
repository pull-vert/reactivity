package reactivity.experimental

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.reactive.awaitSingle
import kotlinx.coroutines.experimental.reactive.publish
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription

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
// TODO provide a custom ProducerScope impl that checks send is only called once, and throws exception otherwise !
fun <T> solo(
        scheduler: Scheduler,
        parent: Job? = null,
        block: suspend ProducerScope<T>.() -> Unit
): Solo<T> = SoloImpl(publish(scheduler.context, parent, block), scheduler)

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
 * Builder for [Solo], a single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
object SoloBuilder {
    // protected Static factory methods to create a Solo

    /**
     * Creates a [Solo] from a [value]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [value]
     */
    @JvmStatic
    fun <T> fromValue(value: T) = value.toSolo()

    /**
     * Creates a [Solo] from a [value]
     *
     * @return the [Solo]<T> created
     *
     * @param T the type of the input [value]
     */
    @JvmStatic
    fun <T> fromValue(scheduler: Scheduler, value: T) = value.toSolo(scheduler)
}

/**
 * Subscribes to this [Solo] and performs the specified action for the unique received element.
 */
inline suspend fun <T> Solo<T>.consumeUnique(action: (T) -> Unit) {
    action.invoke(awaitSingle())
}

/**
 * Single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
interface Solo<T> : PublisherCommons<T> {

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>

    override fun doOnNext(onNext: (T) -> Unit): Solo<T>

    override fun doOnError(onError: (Throwable) -> Unit): Solo<T>

    override fun doOnComplete(onComplete: () -> Unit): Solo<T>

    override fun doOnCancel(onCancel: () -> Unit): Solo<T>

    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>

    override fun doFinally(finally: () -> Unit): Solo<T>

    /**
     * Returns a [Solo] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    override fun publishOn(delayError: Boolean) = publishOn(initialScheduler, delayError)

    /**
     * Returns a [Solo] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>
}

internal class SoloImpl<T>(val delegate: Publisher<T>,
                          override val initialScheduler: Scheduler)
    : Solo<T>, Publisher<T> by delegate {

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnNext(onNext: (T) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onNextBlock = onNext
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onErrorBlock = onError
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnComplete(onComplete: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCompleteBlock = onComplete
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnCancel(onCancel: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCancelBlock = onCancel
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onRequestBlock = onRequest
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun doFinally(finally: () -> Unit): Solo<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.finallyBlock = finally
            return this
        }
        // otherwise creates a new PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return SoloImpl(publisherCallbacks, initialScheduler)
    }

    override fun publishOn(scheduler: Scheduler, delayError: Boolean) = solo(scheduler) {
        val channel = PublisherPublishOn<T>(delayError, Int.MAX_VALUE)
        this@SoloImpl.subscribe(channel)
        channel.consumeEach {
            send(it)
        }
    }
}