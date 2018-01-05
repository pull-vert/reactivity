package reactivity.experimental

import kotlinx.coroutines.experimental.DisposableHandle
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.selects.whileSelect
import java.util.concurrent.TimeUnit

/**
 * Creates cold reactive [Multi] that runs a given [block] in a coroutine.
 * Every time the returned publisher is subscribed, it starts a new coroutine in the specified [scheduler].
 * Coroutine emits items with `send`. Unsubscribing cancels running coroutine.
 *
 * Invocations of `send` are suspended appropriately when subscribers apply back-pressure and to ensure that
 * `onNext` is not invoked concurrently.
 *
 * | **Coroutine action**                         | **Signal to subscriber**
 * | -------------------------------------------- | ------------------------
 * | `value in end of coroutine is not null`      | `onNext`
 * | Normal completion or `close` without cause   | `onComplete`
 * | Failure with exception or `close` with cause | `onError`
 */
actual fun <T> multi(
        scheduler: Scheduler,
        key: Multi.Key<*>? = null,
        parent: Job? = null,
        block: suspend ProducerScope<T>.() -> Unit
): Multi<T> = MultiImpl(publish(scheduler.context, parent, block), scheduler, key)

/**
 * Builder for [Multi], a multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
object MultiBuilder {
    // Static factory methods to create a Multi

    /**
     * Creates a [Multi] from a range of Int (starting from [start] and emmitting
     * [count] items)
     *
     * @return the [Multi]<Int> created
     */
    @JvmStatic
    fun fromRange(start: Int, count: Int): Multi<Int> = IntRange(start, start + count - 1).toMulti()

    /**
     * Creates a [Multi] from a range of Int (starting from [start] and emmitting
     * [count] items)
     *
     * @return the [Multi]<Int> created
     */
    @JvmStatic
    fun fromRange(scheduler: Scheduler, start: Int, count: Int) = IntRange(start, start + count - 1).toMulti(scheduler)

    /**
     * Creates a [Multi] from a [Iterable]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [iterable]
     */
    @JvmStatic
    fun <T> fromIterable(iterable: Iterable<T>) = iterable.toMulti()

    /**
     * Creates a [Multi] from a [Iterable]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [iterable]
     */
    @JvmStatic
    fun <T> fromIterable(scheduler: Scheduler, iterable: Iterable<T>) = iterable.toMulti(scheduler)

    /**
     * Creates a [Multi] from a [Array]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [array]
     */
    @JvmStatic
    fun <T> fromArray(array: Array<T>) = array.toMulti()

    /**
     * Creates a [Multi] from a [Array]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [array]
     */
    @JvmStatic
    fun <T> fromArray(scheduler: Scheduler, array: Array<T>) = array.toMulti(scheduler)

    /**
     * Creates a [Multi] from a [Publisher]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [Publisher]
     */
    @JvmStatic
    fun <T> fromPublisher(publisher: Publisher<T>) = publisher.toMulti()

    /**
     * Creates a [Multi] from a [Publisher]
     *
     * *To notice : no need for [Multi] coroutine here !*
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [Publisher]
     */
    @JvmStatic
    fun <T> fromPublisher(scheduler: Scheduler, publisher: Publisher<T>) = publisher.toMulti(scheduler)
}

/**
 * Multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
actual interface Multi<T>: CommonMultiOperators<T>, Publisher<T> {

    actual fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): DisposableHandle

    actual fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>

    actual override fun doOnNext(onNext: (T) -> Unit): Multi<T>

    actual override fun doOnError(onError: (Throwable) -> Unit): Multi<T>

    actual override fun doOnComplete(onComplete: () -> Unit): Multi<T>

    actual override fun doOnCancel(onCancel: () -> Unit): Multi<T>

    actual override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>

    actual override fun doFinally(finally: () -> Unit): Multi<T>

    actual override fun publishOn(delayError: Boolean): Multi<T>

    actual override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T>

    actual fun publishOn(delayError: Boolean, prefetch: Int): Multi<T>

    actual fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T>

    // Operators

    /**
     * Returns a [Multi] that delays each received element
     *
     * @param time the delay time
     * @param unit the delay time unit
     */
    fun delay(time: Long, unit: TimeUnit): Multi<T>

    actual fun <U> takeUntil(other: Publisher<U>): Multi<T>

    actual class Key<out R> actual constructor(val value: R)
}

internal class MultiImpl<T>(private val delegate: Publisher<T>,
                                 override val initialScheduler: Scheduler,
                                 override val key: Multi.Key<*>? = null)
    : Multi<T>, WithLambdaImpl<T>, Publisher<T> by delegate {

    override fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((org.reactivestreams.Subscription) -> Unit)?): DisposableHandle {
        return subscribeWith(SubscriberLambda(onNext, onError, onComplete, onSubscribe))
    }

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnNext(onNext: (T) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onNextBlock = onNext
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onErrorBlock = onError
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnComplete(onComplete: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCompleteBlock = onComplete
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnCancel(onCancel: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCancelBlock = onCancel
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onRequestBlock = onRequest
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doFinally(finally: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.finallyBlock = finally
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun publishOn(delayError: Boolean) = publishOn(initialScheduler, delayError)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean) = multi(scheduler, key) {
        val channel = PublisherPublishOn<T>(delayError, Int.MAX_VALUE)
        this@MultiImpl.subscribe(channel)
        channel.consumeEach {
            send(it)
        }
    }

    override fun publishOn(delayError: Boolean, prefetch: Int) = publishOn(initialScheduler, delayError, prefetch)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int) = multi(scheduler, key) {
        val channel = PublisherPublishOn<T>(delayError, prefetch)
        this@MultiImpl.subscribe(channel)
        // Prefetch provided number of items
        val list = mutableListOf<T>()
        channel.use { chan ->
            var count = 0
            for (x in chan) {
                list.add(x)
                count++
                if (count == prefetch) {
                    channel.subscription?.request(Long.MAX_VALUE)
                    break
                }
            }
            // send prefetched items
            for (x in list) {
                send(x)
            }
            // continue sending all next items
            for (x in chan) {
                send(x)
            }
        }
    }

    override fun delay(time: Long, unit: TimeUnit) = multi(initialScheduler, key) {
        consumeEach {
            // consume the source stream
            kotlinx.coroutines.experimental.delay(time, unit) // delay
            send(it) // send
        }
    }

    override fun <U> takeUntil(other: Publisher<U>) = multi(initialScheduler, key) {
        openSubscription().use { thisChannel ->
            // explicitly open channel to Publisher<T>
            other.openSubscription().use { otherChannel ->
                // explicitly open channel to Publisher<U>
                whileSelect {
                    otherChannel.onReceive { false }          // bail out on any received element from `other`
                    thisChannel.onReceive { send(it); true }  // resend element from this channel and continue
                }
            }
        }
    }
}
