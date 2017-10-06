package reactivity.experimental

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.reactive.asPublisher
import kotlinx.coroutines.experimental.reactive.consumeEach
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import reactivity.experimental.core.DefaultMulti
import reactivity.experimental.core.Scheduler
import reactivity.experimental.core.multiPublisher

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
): Multi<T> = MultiImpl(multiPublisher(scheduler, block))

/**
 * Multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
abstract class Multi<T> protected constructor(): DefaultMulti<T> {

    /**
     * Singleton builder for [Multi], a multi values Reactive Stream [Publisher]
     *
     * @author Frédéric Montariol
     */
    companion object {
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
        fun <T> fromIterable(iterable: Iterable<T>): Multi<T> = iterable.toMulti()

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

    // functions from WithCallbacks

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T> = MultiImpl(super.doOnSubscribe(onSubscribe))
    override fun doOnNext(onNext: (T) -> Unit): Multi<T> = MultiImpl(super.doOnNext(onNext))
    override fun doOnError(onError: (Throwable) -> Unit): Multi<T> = MultiImpl(super.doOnError(onError))
    override fun doOnComplete(onComplete: () -> Unit): Multi<T> = MultiImpl(super.doOnComplete(onComplete))
    override fun doOnCancel(onCancel: () -> Unit): Multi<T> = MultiImpl(super.doOnCancel(onCancel))
    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T> = MultiImpl(super.doOnRequest(onRequest))
    override fun doFinally(finally: () -> Unit): Multi<T> = MultiImpl(super.doFinally(finally))

    // Operators

    override fun publishOn(delayError: Boolean): Multi<T> = MultiImpl(super.publishOn(delayError))
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T> = MultiImpl(super.publishOn(scheduler, delayError))
    override fun publishOn(delayError: Boolean, prefetch: Int): Multi<T> = MultiImpl(super.publishOn(delayError, prefetch))
    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T> = MultiImpl(super.publishOn(scheduler, delayError, prefetch))
    override fun <R> map(mapper: (T) -> R): Multi<R> = MultiImpl(super.map(mapper))
    override fun <R> map(scheduler: Scheduler, mapper: (T) -> R): Multi<R> = MultiImpl(super.map(scheduler, mapper))
    override fun filter(predicate: (T) -> Boolean): Multi<T> = MultiImpl(super.filter(predicate))
    override fun filter(scheduler: Scheduler, predicate: (T) -> Boolean): Multi<T> = MultiImpl(super.filter(scheduler, predicate))
    override fun findFirst(predicate: (T) -> Boolean): Solo<T?> = SoloImpl(super.findFirst(predicate))
    override fun findFirst(scheduler: Scheduler, predicate: (T) -> Boolean): Solo<T?> = SoloImpl(super.findFirst(scheduler, predicate))
    override fun <R> flatMap(mapper: (T) -> Publisher<R>): Multi<R> = MultiImpl(super.flatMap(mapper))
    override fun <R> flatMap(scheduler: Scheduler, mapper: (T) -> Publisher<R>): Multi<R> = MultiImpl(super.flatMap(scheduler, mapper))
    override fun <U> takeUntil(other: Publisher<U>): Multi<T> = MultiImpl(super.takeUntil(other))
    override fun <U> takeUntil(scheduler: Scheduler, other: Publisher<U>): Multi<T> = MultiImpl(super.takeUntil(scheduler, other))
    override fun mergeWith(vararg others: Publisher<T>): Multi<T> = MultiImpl(super.mergeWith(*others))
    override fun mergeWith(scheduler: Scheduler, vararg others: Publisher<T>): Multi<T> = MultiImpl(super.mergeWith(scheduler, *others))
    override fun take(n: Long): Multi<T> = MultiImpl(super.take(n))
    override fun take(scheduler: Scheduler, n: Long): Multi<T> = MultiImpl(super.take(scheduler, n))

    // Combined Operators

    override fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R> = MultiImpl(super.fusedFilterMap(predicate, mapper))
    override fun <R> fusedFilterMap(scheduler: Scheduler, predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R> = MultiImpl(super.fusedFilterMap(scheduler, predicate, mapper))

    // Needs to be implemented
    override fun <R> groupBy(keyMapper: (T) -> R) = groupBy(initialScheduler, keyMapper)

    override fun <R> groupBy(scheduler: Scheduler, keyMapper: (T) -> R): Multi<MultiGrouped<T, R>> = multi(scheduler) {
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
                send(MultiGroupedImpl(channel.asPublisher(coroutineContext).toMulti(scheduler) as MultiImpl, key) as MultiGrouped<T, R>)
                channelMap[key] = channel // adds to Map
            }

            channel.send(it)
        }
        // when all the items from source stream are consumed, close every channels (to stop the computation loop)
        channelMap.forEach { u -> u.value.close() }
    }
}

internal open class MultiImpl<T> internal constructor(private val del: DefaultMulti<T>)
    : Multi<T>(), Publisher<T> by del.delegate {
    override val delegate: Publisher<T>
        get() = del.delegate
    override val initialScheduler: Scheduler
        get() = del.initialScheduler
}