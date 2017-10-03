package reactivity.experimental.core

import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.selects.whileSelect
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription

/**
 * Creates cold reactive [MultiPublisherImpl] that runs a given [block] in a coroutine.
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
fun <T> multiPublisher(
        scheduler: Scheduler,
        block: suspend ProducerScope<T>.() -> Unit
) = MultiPublisherImpl(publish(scheduler.context, block), scheduler)

/**
 * Builder for [MultiPublisherImpl], a multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
abstract class MultiPublisherBuilder {
    // Static factory methods to create a MultiPublisherImpl

    companion object {
        /**
         * Creates a [Multi][reactivity.experimental.Multi] from a range of Int (starting from [start] and emmitting
         * [count] items)
         *
         * @return the [Multi][reactivity.experimental.Multi]<Int> created
         */
        @JvmStatic
        fun fromRange(start: Int, count: Int) = fromRange(DEFAULT_SCHEDULER, start, count)

        /**
         * Creates a [Multi][reactivity.experimental.Multi] from a range of Int (starting from [start] and emmitting
         * [count] items)
         *
         * @return the [Multi][reactivity.experimental.Multi]<Int> created
         */
        @JvmStatic
        fun fromRange(scheduler: Scheduler, start: Int, count: Int) = multiPublisher(scheduler) {
            for (x in start until start + count) send(x)
        }

        /**
         * Creates a [Multi][reactivity.experimental.Multi] from a [Iterable]
         *
         * @return the [Multi][reactivity.experimental.Multi]<T> created
         *
         * @param T the type of the input [iterable]
         */
        @JvmStatic
        fun <T> fromIterable(iterable: Iterable<T>) = fromIterable(DEFAULT_SCHEDULER, iterable)

        /**
         * Creates a [Multi][reactivity.experimental.Multi] from a [Iterable]
         *
         * @return the [Multi][reactivity.experimental.Multi]<T> created
         *
         * @param T the type of the input [iterable]
         */
        @JvmStatic
        fun <T> fromIterable(scheduler: Scheduler, iterable: Iterable<T>) = multiPublisher(scheduler) {
            for (x in iterable) send(x)
        }

        /**
         * Creates a [Multi][reactivity.experimental.Multi] from a [Publisher]
         *
         * @return the [Multi][reactivity.experimental.Multi]<T> created
         *
         * @param T the type of the input [Publisher]
         */
        @JvmStatic
        fun <T> fromPublisher(publisher: Publisher<T>) = fromPublisher(DEFAULT_SCHEDULER, publisher)

        /**
         * Creates a [Multi][reactivity.experimental.Multi] from a [Publisher]
         *
         * *To notice : no need for [Multi][reactivity.experimental.Multi] coroutine here !*
         *
         * @return the [Multi][reactivity.experimental.Multi]<T> created
         *
         * @param T the type of the input [Publisher]
         */
        @JvmStatic
        fun <T> fromPublisher(scheduler: Scheduler, publisher: Publisher<T>) = MultiPublisherImpl(publisher, scheduler)
    }
}

/**
 * Multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
interface MultiPublisher<T> : PublisherCommons<T> {

    // functions from WithCallbacks
    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): MultiPublisher<T>

    override fun doOnNext(onNext: (T) -> Unit): MultiPublisher<T>
    override fun doOnError(onError: (Throwable) -> Unit): MultiPublisher<T>
    override fun doOnComplete(onComplete: () -> Unit): MultiPublisher<T>
    override fun doOnCancel(onCancel: () -> Unit): MultiPublisher<T>
    override fun doOnRequest(onRequest: (Long) -> Unit): MultiPublisher<T>
    override fun doFinally(finally: () -> Unit): MultiPublisher<T>

    // Operators

    // function from WithPublishOn
    /**
     * Returns a [Multi][reactivity.experimental.Multi] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    override fun publishOn(delayError: Boolean): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): MultiPublisher<T>

    // Operators specific to Multi

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that is published with [initialScheduler],
     * the [delayError] option and the [prefetch] items
     *
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    fun publishOn(delayError: Boolean, prefetch: Int): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that is published with the provided [scheduler],
     * the [delayError] option and the [prefetch] items
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param mapper the mapper function
     */
    fun <R> map(mapper: (T) -> R): MultiPublisher<R>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param mapper the mapper function
     */
    fun <R> map(scheduler: Scheduler, mapper: (T) -> R): MultiPublisher<R>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that filters each received element, sending it
     * only if [predicate] is satisfied
     *
     * @param predicate the filter predicate
     */
    fun filter(predicate: (T) -> Boolean): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that filters each received element, sending it
     * only if [predicate] is satisfied
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     */
    fun filter(scheduler: Scheduler, predicate: (T) -> Boolean): MultiPublisher<T>

    /**
     * Returns a [SoloPublisher] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param predicate the filter predicate
     */
    fun findFirst(predicate: (T) -> Boolean): SoloPublisherImpl<T?>

    /**
     * Returns a [SoloPublisher] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     */
    fun findFirst(scheduler: Scheduler, predicate: (T) -> Boolean): SoloPublisherImpl<T?>

    /**
     * Returns a [Multi][reactivity.experimental.Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param mapper the mapper function
     */
    fun <R> flatMap(mapper: (T) -> Publisher<R>): MultiPublisher<R>

    /**
     * Returns a [Multi][reactivity.experimental.Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param mapper the mapper function
     */
    fun <R> flatMap(scheduler: Scheduler, mapper: (T) -> Publisher<R>): MultiPublisher<R>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param other the other publisher
     */
    fun <U> takeUntil(other: Publisher<U>): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param other the other publisher
     */
    fun <U> takeUntil(scheduler: Scheduler, other: Publisher<U>): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param others the other publishers
     */
    fun mergeWith(vararg others: Publisher<T>): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param others the other publishers
     */
    fun mergeWith(scheduler: Scheduler, vararg others: Publisher<T>): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that can contain several [MultiPublisherGrouped], each is a group of received elements from
     * the source stream that are related with the same key
     *
     * @param keyMapper a function that extracts the key for each item
     */
    fun <R> groupBy(keyMapper: (T) -> R): MultiPublisher<out MultiPublisherGrouped<T, R>>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that can contain several [MultiPublisherGrouped], each is a group of received elements from
     * the source stream that are related with the same key
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param keyMapper a function that extracts the key for each item
     */
    fun <R> groupBy(scheduler: Scheduler, keyMapper: (T) -> R): MultiPublisher<out MultiPublisherGrouped<T, R>>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that will send the [n] first received elements from the source stream
     *
     * @param n number of items to send
     */
    fun take(n: Long): MultiPublisher<T>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that will send the [n] first received elements from the source stream
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param n number of items to send
     */
    fun take(scheduler: Scheduler, n: Long): MultiPublisher<T>

    // Combined Operators

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): MultiPublisher<R>

    /**
     * Returns a [Multi][reactivity.experimental.Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    fun <R> fusedFilterMap(scheduler: Scheduler, predicate: (T) -> Boolean, mapper: (T) -> R): MultiPublisher<R>
}

open class MultiPublisherImpl<T> (val delegate: Publisher<T>,
                                             val initialScheduler: Scheduler)
    : MultiPublisher<T>, Publisher<T> by delegate {

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): MultiPublisherImpl<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onSubscribeBlock = onSubscribe
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return MultiPublisherImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnNext(onNext: (T) -> Unit): MultiPublisherImpl<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onNextBlock = onNext
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return MultiPublisherImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnError(onError: (Throwable) -> Unit): MultiPublisherImpl<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onErrorBlock = onError
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return MultiPublisherImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnComplete(onComplete: () -> Unit): MultiPublisherImpl<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCompleteBlock = onComplete
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return MultiPublisherImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnCancel(onCancel: () -> Unit): MultiPublisherImpl<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onCancelBlock = onCancel
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return MultiPublisherImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): MultiPublisherImpl<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.onRequestBlock = onRequest
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return MultiPublisherImpl(publisherCallbacks, initialScheduler)
    }

    override fun doFinally(finally: () -> Unit): MultiPublisherImpl<T> {
        if (delegate is PublisherWithCallbacks) {
            delegate.finallyBlock = finally
            return this
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return MultiPublisherImpl(publisherCallbacks, initialScheduler)
    }

    override fun publishOn(delayError: Boolean) = publishOn(initialScheduler, delayError)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean) = multiPublisher(scheduler) {
        val channel = MultiPublishOn<T>(delayError, Int.MAX_VALUE)
        this@MultiPublisherImpl.subscribe(channel)
        channel.consumeEach {
            send(it)
        }
    }

    override fun publishOn(delayError: Boolean, prefetch: Int) = publishOn(initialScheduler, delayError, prefetch)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int) = multiPublisher(scheduler) {
        val channel = MultiPublishOn<T>(delayError, prefetch)
        this@MultiPublisherImpl.subscribe(channel)
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

    // Operators

    override fun <R> map(mapper: (T) -> R) = map(initialScheduler, mapper)

    override fun <R> map(scheduler: Scheduler, mapper: (T) -> R) = multiPublisher(scheduler) {
        consumeEach {
            // consume the source stream
            send(mapper(it))     // map
        }
    }

    override fun filter(predicate: (T) -> Boolean) = filter(initialScheduler, predicate)

    override fun filter(scheduler: Scheduler, predicate: (T) -> Boolean) = multiPublisher(scheduler) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter
                send(it)
        }
    }

    override fun findFirst(predicate: (T) -> Boolean) = findFirst(initialScheduler, predicate)

    override fun findFirst(scheduler: Scheduler, predicate: (T) -> Boolean) = soloPublisher(scheduler) {
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

    override fun <R> flatMap(mapper: (T) -> Publisher<R>) = flatMap(initialScheduler, mapper)

    override fun <R> flatMap(scheduler: Scheduler, mapper: (T) -> Publisher<R>) = multiPublisher(scheduler) {
        consumeEach {
            // consume the source stream
            val pub = mapper(it)
            launch(coroutineContext) {
                // launch a child coroutine
                pub.consumeEach { send(it) }    // send every element from this publisher
            }
        }
    }

    override fun <U> takeUntil(other: Publisher<U>) = takeUntil(initialScheduler, other)

    override fun <U> takeUntil(scheduler: Scheduler, other: Publisher<U>) = multiPublisher(scheduler) {
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

    override fun mergeWith(vararg others: Publisher<T>) = mergeWith(initialScheduler, *others)

    override fun mergeWith(scheduler: Scheduler, vararg others: Publisher<T>) = multiPublisher(scheduler) {
        launch(coroutineContext) {
            /** launch a first child coroutine for this [Multi][reactivity.experimental.Multi] */
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

    override fun take(n: Long) = take(initialScheduler, n)

    override fun take(scheduler: Scheduler, n: Long) = multiPublisher(scheduler) {
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

    override fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R)
            = fusedFilterMap(initialScheduler, predicate, mapper)

    override fun <R> fusedFilterMap(scheduler: Scheduler, predicate: (T) -> Boolean, mapper: (T) -> R) = multiPublisher(scheduler) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter part
                send(mapper(it))     // map part
        }
    }

    // Needs to be implemented in subclass
    override fun <R> groupBy(keyMapper: (T) -> R): MultiPublisher<out MultiPublisherGrouped<T, R>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <R> groupBy(scheduler: Scheduler, keyMapper: (T) -> R): MultiPublisher<out MultiPublisherGrouped<T, R>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}