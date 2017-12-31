package reactivity.experimental

import kotlinx.coroutines.experimental.DisposableHandle
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.asPublisher
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.selects.whileSelect
import java.util.concurrent.TimeUnit

//actual typealias ProducerScope<E> = kotlinx.coroutines.experimental.channels.ProducerScope<in E>

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
fun <T> multi(
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
actual interface Multi<T>: CommonPublisherCommons<T>, Publisher<T> {

    actual fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): DisposableHandle

    /**
     * Key for identifiying common grouped elements, used by [Multi.groupBy] operator
     */
    actual val key: Key<*>?

    actual fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>

    actual override fun doOnNext(onNext: (T) -> Unit): Multi<T>

    actual override fun doOnError(onError: (Throwable) -> Unit): Multi<T>

    actual override fun doOnComplete(onComplete: () -> Unit): Multi<T>

    actual override fun doOnCancel(onCancel: () -> Unit): Multi<T>

    actual override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>

    actual override fun doFinally(finally: () -> Unit): Multi<T>

    /**
     * Returns a [Multi] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    actual override fun publishOn(delayError: Boolean): Multi<T>

    /**
     * Returns a [Multi] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    actual override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T>

    /**
     * Returns a [Multi] that is published with [initialScheduler],
     * the [delayError] option and prefetch the [prefetch] first items before sending them
     *
     * @param delayError if error should be delayed
     * @param prefetch number of items to request at first. When obtained, request all remaining items
     */
    actual fun publishOn(delayError: Boolean, prefetch: Int): Multi<T>

    /**
     * Returns a [Multi] that is published with the provided [scheduler],
     * the [delayError] option and the [prefetch] items
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    actual fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T>

    // Operators

    /**
     * Returns a [Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param mapper the mapper function
     */
    actual fun <R> map(mapper: (T) -> R): Multi<R>

    /**
     * Returns a [Multi] that delays each received element
     *
     * @param time the delay time
     */
    actual fun delay(time: Int): Multi<T>

    /**
     * Returns a [Multi] that delays each received element
     *
     * @param time the delay time
     * @param unit the delay time unit
     */
    fun delay(time: Long, unit: TimeUnit): Multi<T>

    /**
     * Returns a [Multi] that performs the specified [action] for each received element.
     *
     * @param block the function
     */
    actual fun peek(action: (T) -> Unit): Multi<T>

    /**
     * Returns a [Multi] that filters received elements, sending it
     * only if [predicate] is satisfied
     *
     * @param predicate the filter predicate
     */
    actual fun filter(predicate: (T) -> Boolean): Multi<T>

    /**
     * Returns a [Solo] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param predicate the filter predicate
     */
    actual fun findFirst(predicate: (T) -> Boolean): Solo<T?>

    /**
     * Returns a [Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param mapper the mapper function
     */
    actual fun <R> flatMap(mapper: (T) -> Publisher<R>): Multi<R>

    /**
     * Returns a [Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param other the other publisher
     */
    actual fun <U> takeUntil(other: Publisher<U>): Multi<T>

    /**
     * Returns a [Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param others the other publishers
     */
    actual fun mergeWith(vararg others: Publisher<T>): Multi<T>

    /**
     * Returns a [Multi] that will send the [n] first received elements from the source stream
     *
     * @param n number of items to send
     */
    actual fun take(n: Long): Multi<T>

    /**
     * Returns a [Multi] that can contain several [Multi]
     * , each is a group of received elements from the source stream that are related with the same [Key]
     *
     * @param keyMapper a function that extracts the key for each item
     */
    actual fun <R> groupBy(keyMapper: (T) -> R): Multi<out Multi<T>>

    // Combined Operators

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    actual fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R>

    /**
     * Key for identifiying common grouped elements of this [Multi]. [E] is key type.
     */
    actual class Key<R> actual constructor(val value: R)
}

internal class MultiImpl<T>(val delegate: Publisher<T>,
                                 override val initialScheduler: Scheduler,
                                 override val key: Multi.Key<*>? = null)
    : Multi<T>, WithLambdaImpl<T>, Publisher<T> by delegate {

    override fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((org.reactivestreams.Subscription) -> Unit)?): DisposableHandle {
        return subscribeWith(SubscriberLambda(onNext, onError, onComplete, onSubscribe))
    }

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnNext(onNext: (T) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onNextBlock = onNext
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onErrorBlock = onError
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnComplete(onComplete: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onCompleteBlock = onComplete
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnCancel(onCancel: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onCancelBlock = onCancel
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).onRequestBlock = onRequest
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return MultiImpl(publisherCallbacks, initialScheduler, key)
    }

    override fun doFinally(finally: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).finallyBlock = finally
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

    override fun <R> map(mapper: (T) -> R) = multi(initialScheduler, key) {
        consumeEach {
            // consume the source stream
            send(mapper(it))     // map
        }
    }

    override fun delay(time: Int) = delay(time.toLong(), TimeUnit.MILLISECONDS)

    override fun delay(time: Long, unit: TimeUnit) = multi(initialScheduler, key) {
        consumeEach {
            // consume the source stream
            kotlinx.coroutines.experimental.delay(time, unit) // delay
            send(it) // send
        }
    }

    override fun peek(action: (T) -> Unit) = multi(initialScheduler, key) {
        consumeEach {
            // consume the source stream
            action(it)
            send(it)     // peek
        }
    }

    override fun filter(predicate: (T) -> Boolean) = multi(initialScheduler, key) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter
                send(it)
        }
    }

    override fun findFirst(predicate: (T) -> Boolean) = solo(initialScheduler) {
        var produced = false
        openSubscription().use { channel ->
            // open channel to the source
            for (x in channel) { // iterate over the channel to receive elements from it
                if (predicate(x)) {       // filter 1 item
                    send(x)
                    produced = true
                    break
                }
                // `use` will close the channel when this block of code is complete
            }
            if (!produced) send(null)
        }
        // TODO make a unit test to verify what happends when no item satisfies the predicate
    }

    override fun <R> flatMap(mapper: (T) -> Publisher<R>) = multi(initialScheduler, key) {
        consumeEach {
            // consume the source stream
            val pub = mapper(it)
            launch(coroutineContext) {
                // launch a child coroutine
                pub.consumeEach { send(it) }    // send every element from this publisher
            }
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

    override fun mergeWith(vararg others: Publisher<T>) = multi(initialScheduler, key) {
        launch(coroutineContext) {
            /** launch a first child coroutine for this [Multi] */
            consumeEach {
                send(it)
            }
        }
        for (other in others) {
            launch(coroutineContext) {
                /** launch a new child coroutine for each of the [others] [Publisher] */
                other.consumeEach {
                    send(it) // resend all element from this publisher
                }
            }
        }
    }

    override fun take(n: Long) = multi(initialScheduler, key) {
        openSubscription().use { channel ->
            // explicitly open channel to Publisher<T>
            var count = 0L
            for (c in channel) {
                send(c)
                count++
                if (count == n) break
            }
            // `use` will close the channel when this block of code is complete
        }
    }

    override fun <R> groupBy(keyMapper: (T) -> R) = multi(initialScheduler, key) {
        var key: R
        var channel: Channel<T>
        val channelMap = mutableMapOf<R, Channel<T>>()
        consumeEach {
            // consume the source stream
            key = keyMapper(it)
            if (channelMap.containsKey(key)) { // this channel exists already
                channel = channelMap[key]!!
            } else { // have to create a new MultiPublisherGrouped
                /** Creates a [kotlinx.coroutines.experimental.channels.LinkedListChannel] */
                channel = Channel(Channel.UNLIMITED)
                // Converts a stream of elements received from the channel to the hot reactive publisher
                val multiGrouped = channel.asPublisher(coroutineContext).toMulti(initialScheduler, Multi.Key(key))
                send(multiGrouped)
                channelMap[key] = channel // adds to Map
            }

            channel.send(it)
        }
        // when all the items from source stream are consumed, close every channels (to stop the computation loop)
        channelMap.forEach { u -> u.value.close() }
    }


    // Combined Operators

    override fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R) = multi(initialScheduler, key) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter part
                send(mapper(it))     // map part
        }
    }
}