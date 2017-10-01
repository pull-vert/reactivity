package reactivity.experimental

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.asPublisher
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.selects.whileSelect
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription

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
        block: suspend ProducerScope<T>.() -> Unit
): Multi<T> = MultiImpl(publish(scheduler.context, block))

/**
 * Singleton builder for [Multi], a multi values Reactive Stream [Publisher]
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
    fun fromRange(start: Int, count: Int) = fromRange(DEFAULT_SCHEDULER, start, count)

    /**
     * Creates a [Multi] from a range of Int (starting from [start] and emmitting
     * [count] items)
     *
     * @return the [Multi]<Int> created
     */
    @JvmStatic
    fun fromRange(scheduler: Scheduler, start: Int, count: Int) = multi(scheduler) {
        for (x in start until start + count) send(x)
    }

    /**
     * Creates a [Multi] from a [Iterable]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [iterable]
     */
    @JvmStatic
    fun <T> fromIterable(iterable: Iterable<T>) = fromIterable(DEFAULT_SCHEDULER, iterable)

    /**
     * Creates a [Multi] from a [Iterable]
     *
     * @return the [Multi]<T> created
     *
     * @param T the type of the input [iterable]
     */
    @JvmStatic
    fun <T> fromIterable(scheduler: Scheduler, iterable: Iterable<T>) = multi(scheduler) {
        for (x in iterable) send(x)
    }
}

/**
 * Multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
interface Multi<T> : PublisherCommons<T> {

    // functions from WithCallbacks
    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>

    override fun doOnNext(onNext: (T) -> Unit): Multi<T>
    override fun doOnError(onError: (Throwable) -> Unit): Multi<T>
    override fun doOnComplete(onComplete: () -> Unit): Multi<T>
    override fun doOnCancel(onCancel: () -> Unit): Multi<T>
    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>
    override fun doFinally(finally: () -> Unit): Multi<T>

    // Operators

    // function from WithPublishOn
    /**
     * Returns a [Multi] that is published with [DEFAULT_SCHEDULER] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    override fun publishOn(delayError: Boolean) = publishOn(DEFAULT_SCHEDULER, delayError)

    /**
     * Returns a [Multi] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T>

    // Operators specific to Multi

    /**
     * Returns a [Multi] that is published with [DEFAULT_SCHEDULER],
     * the [delayError] option and the [prefetch] items
     *
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    fun publishOn(delayError: Boolean, prefetch: Int) = publishOn(DEFAULT_SCHEDULER, delayError, prefetch)

    /**
     * Returns a [Multi] that is published with the provided [scheduler],
     * the [delayError] option and the [prefetch] items
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T>

    /**
     * Returns a [Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param mapper the mapper function
     */
    fun <R> map(mapper: (T) -> R) = map(DEFAULT_SCHEDULER, mapper)

    /**
     * Returns a [Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param mapper the mapper function
     */
    fun <R> map(scheduler: Scheduler, mapper: (T) -> R): Multi<R>

    /**
     * Returns a [Multi] that filters each received element, sending it
     * only if [predicate] is satisfied
     *
     * @param predicate the filter predicate
     */
    fun filter(predicate: (T) -> Boolean) = filter(DEFAULT_SCHEDULER, predicate)

    /**
     * Returns a [Multi] that filters each received element, sending it
     * only if [predicate] is satisfied
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     */
    fun filter(scheduler: Scheduler, predicate: (T) -> Boolean): Multi<T>

    /**
     * Returns a [Solo] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param predicate the filter predicate
     */
    fun findFirst(predicate: (T) -> Boolean) = findFirst(DEFAULT_SCHEDULER, predicate)

    /**
     * Returns a [Solo] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     */
    fun findFirst(scheduler: Scheduler, predicate: (T) -> Boolean): Solo<T?>

    /**
     * Returns a [Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param mapper the mapper function
     */
    fun <R> flatMap(mapper: (T) -> Publisher<R>) = flatMap(DEFAULT_SCHEDULER, mapper)

    /**
     * Returns a [Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param mapper the mapper function
     */
    fun <R> flatMap(scheduler: Scheduler, mapper: (T) -> Publisher<R>): Multi<R>

    /**
     * Returns a [Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param other the other publisher
     */
    fun <U> takeUntil(other: Publisher<U>) = takeUntil(DEFAULT_SCHEDULER, other)

    /**
     * Returns a [Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param other the other publisher
     */
    fun <U> takeUntil(scheduler: Scheduler, other: Publisher<U>): Multi<T>

    /**
     * Returns a [Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param others the other publishers
     */
    fun mergeWith(vararg others: Publisher<T>) = mergeWith(DEFAULT_SCHEDULER, *others)

    /**
     * Returns a [Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param others the other publishers
     */
    fun mergeWith(scheduler: Scheduler, vararg others: Publisher<T>): Multi<T>

    /**
     * Returns a [Multi] that can contain several [MultiGrouped], each is a group of received elements from
     * the source stream that are related with the same key
     *
     * @param keyMapper a function that extracts the key for each item
     */
    fun <R> groupBy(keyMapper: (T) -> R) = groupBy(DEFAULT_SCHEDULER, keyMapper)

    /**
     * Returns a [Multi] that can contain several [MultiGrouped], each is a group of received elements from
     * the source stream that are related with the same key
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param keyMapper a function that extracts the key for each item
     */
    fun <R> groupBy(scheduler: Scheduler, keyMapper: (T) -> R): Multi<MultiGrouped<T, R>>

    /**
     * Returns a [Multi] that will send the [n] first received elements from the source stream
     *
     * @param n number of items to send
     */
    fun take(n: Long) = take(DEFAULT_SCHEDULER, n)

    /**
     * Returns a [Multi] that will send the [n] first received elements from the source stream
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param n number of items to send
     */
    fun take(scheduler: Scheduler, n: Long): Multi<T>

    // Combined Operators

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R)
            = fusedFilterMap(DEFAULT_SCHEDULER, predicate, mapper)

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    fun <R> fusedFilterMap(scheduler: Scheduler, predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R>
}

internal open class MultiImpl<T> internal constructor(private val delegate: Publisher<T>) : Multi<T>, Publisher<T> by delegate {

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
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onErrorBlock = onError
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnComplete(onComplete: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCompleteBlock = onComplete
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnCancel(onCancel: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCancelBlock = onCancel
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return MultiImpl(publisherCallbacks)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onRequestBlock = onRequest
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return MultiImpl(publisherCallbacks)
    }

    override fun doFinally(finally: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.finallyBlock = finally
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return MultiImpl(publisherCallbacks)
    }

    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T> {
        return multi(scheduler) {
            val channel = MultiPublishOn<T>(delayError, Int.MAX_VALUE)
            this@MultiImpl.subscribe(channel)
            channel.consumeEach {
                send(it)
            }
        }
    }

    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T> {
        return multi(scheduler) {
            val channel = MultiPublishOn<T>(delayError, prefetch)
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

    // Operators

    override fun <R> map(scheduler: Scheduler, mapper: (T) -> R) = multi(scheduler) {
        consumeEach {
            // consume the source stream
            send(mapper(it))     // map
        }
    }

    override fun filter(scheduler: Scheduler, predicate: (T) -> Boolean) = multi(scheduler) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter
                send(it)
        }
    }

    override fun findFirst(scheduler: Scheduler, predicate: (T) -> Boolean) = solo(scheduler) {
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

    override fun <R> flatMap(scheduler: Scheduler, mapper: (T) -> Publisher<R>) = multi(scheduler) {
        consumeEach {
            // consume the source stream
            val pub = mapper(it)
            launch(coroutineContext) {
                // launch a child coroutine
                pub.consumeEach { send(it) }    // send every element from this publisher
            }
        }
    }

    override fun <U> takeUntil(scheduler: Scheduler, other: Publisher<U>) = multi(scheduler) {
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

    override fun mergeWith(scheduler: Scheduler, vararg others: Publisher<T>) = multi(scheduler) {
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

    override fun <R> groupBy(scheduler: Scheduler, keyMapper: (T) -> R) = multi(scheduler) {
        var key: R
        var channel: Channel<T>
        val channelMap = mutableMapOf<R, Channel<T>>()
        consumeEach {
            // consume the source stream
            key = keyMapper(it)
            if (channelMap.containsKey(key)) { // this channel exists already
                channel = channelMap[key]!!
            } else { // have to create a new MultiGrouped
                channel = Channel(Channel.UNLIMITED)
                /** Creates a [kotlinx.coroutines.experimental.channels.LinkedListChannel] */
                // Converts a stream of elements received from the channel to the hot reactive publisher
                send(MultiGroupedImpl(channel.asPublisher(coroutineContext), key) as MultiGrouped<T, R>)
                channelMap[key] = channel // adds to Map
            }

            channel.send(it)
        }
        // when all the items from source stream are consumed, close every channels (to stop the computation loop)
        channelMap.forEach { u -> u.value.close() }
    }

    override fun take(scheduler: Scheduler, n: Long) = multi(scheduler) {
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

    // Combined Operators

    override fun <R> fusedFilterMap(scheduler: Scheduler, predicate: (T) -> Boolean, mapper: (T) -> R) = multi(scheduler) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter part
                send(mapper(it))     // map part
        }
    }
}