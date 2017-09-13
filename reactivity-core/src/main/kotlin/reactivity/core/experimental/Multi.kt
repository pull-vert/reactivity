package reactivity.core.experimental

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.reactive.consumeEach
import kotlinx.coroutines.experimental.reactive.openSubscription
import kotlinx.coroutines.experimental.reactive.publish
import kotlinx.coroutines.experimental.selects.whileSelect
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription

fun <T> multi(
        scheduler: Scheduler,
        block: suspend ProducerScope<T>.() -> Unit
): Multi<T> = MultiImpl(publish(scheduler.context, block))

/**
 * @author Frédéric Montariol
 */
abstract class Multi<T> protected constructor() : Publisher<T>, PublisherCommons<T>, WithCallbacks<T>, WithPublishOn {
    companion object {
        // Static factory methods to create a Multi
        @JvmStatic
        fun fromRange(start: Int, count: Int,
                      scheduler: Scheduler = Schedulers.emptyThreadContext()
        ) = multi(scheduler) {
            for (x in start until start + count) send(x)
        }

        @JvmStatic
        fun <T> fromIterable(iterable: Iterable<T>,
                             scheduler: Scheduler = Schedulers.emptyThreadContext()
        ) = multi(scheduler) {
            for (x in iterable) send(x)
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

    // function from WithPublishOn
    override abstract fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T>

    // Operators specific to Multi

    /**
     * Returns a [Multi] that use the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param mapper the mapper function
     */
    abstract fun <R> map(scheduler: Scheduler, mapper: (T) -> R): Multi<R>

    /**
     * Returns a [Multi] that filters each received element, sending it
     * only if [predicate] is satisfied
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     */
    abstract fun filter(scheduler: Scheduler, predicate: (T) -> Boolean): Multi<T>

    /**
     * Returns a [Solo] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     */
    abstract fun findFirst(scheduler: Scheduler, predicate: (T) -> Boolean): Solo<T?>

    /**
     * Returns a [Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param mapper the mapper function
     */
    abstract fun <R> flatMap(scheduler: Scheduler, mapper: (T) -> Publisher<R>): Multi<R>

    /**
     * Returns a [Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param other the other publisher
     */
    abstract fun <U> takeUntil(scheduler: Scheduler, other: Publisher<U>): Multi<T>

    /**
     * Returns a [Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param other the other publisher
     */
    abstract fun mergeWith(scheduler: Scheduler, other: Publisher<T>): Multi<T>

    /**
     * Returns a [Multi] that can contain several [MultiChannel], each is a group of received elements from
     * the source stream that are related with the same key
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param keyMapper a function that extracts the key for each item
     */
    abstract fun <R> groupBy(scheduler: Scheduler, keyMapper: (T) -> R): Multi<MultiChannel<T>>

    /**
     * Returns a [Multi] that will send the [n] first received elements from the source stream
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param n number of items to send
     */
    abstract fun take(scheduler: Scheduler, n: Long): Multi<T>

    // Combined Operators

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    abstract fun <R> fusedFilterMap(scheduler: Scheduler, predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R>
}

open class MultiImpl<T> internal constructor(override final val delegate: Publisher<T>) : Multi<T>(), PublisherDelegated<T> {

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

    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T> {
        return multi(scheduler) {
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
                    produce(x)
                    produced = true
                    break
                }
                // `use` will close the channel when this block of code is complete
            }
            if (!produced) produce(null)
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

    override fun mergeWith(scheduler: Scheduler, other: Publisher<T>) = multi(scheduler) {
        launch(coroutineContext) {
            /** launch a first child coroutine for this [Multi] */
            consumeEach {
                send(it)
            }
        }
        launch(coroutineContext) {
            /** launch a second child coroutine for the [other] [Publisher] */
            other.consumeEach {
                send(it)
            }
        }
    }

    override fun <R> groupBy(scheduler: Scheduler, keyMapper: (T) -> R) = multi(scheduler) {
        var key: R
//        var multiChannel: GroupedMulti<T>
        var multiChannel: MultiChannel<T>
//        val groupedMultiMap = mutableMapOf<R, GroupedMulti<T>>()
        val groupedMultiMap = mutableMapOf<R, MultiChannel<T>>()
        consumeEach {
            // consume the source stream
            key = keyMapper(it)
            if (groupedMultiMap.containsKey(key)) { // this GroupedMulti exists already
                multiChannel = groupedMultiMap[key]!!
            } else { // have to create a new GroupedMulti
                val channel = Channel<T>(Channel.UNLIMITED)
//                val jobProduce = produce<T>(coroutineContext, 0) {
//                    // TODO test without the line under this (but will certainly not work)
//                    while (isActive) { } // cancellable computation loop
//                }
//                multiChannel = GroupedMulti(jobProduce, coroutineContext, key) // creates the new GroupedMulti
                multiChannel = MultiChannel(coroutineContext, channel)
                groupedMultiMap[key] = multiChannel
                send(multiChannel)      // sends the newly created GroupedMulti
            }

//            (multiChannel.producerJob as ProducerCoroutineScope<T>).send(it)
            multiChannel.channel.send(it)
        }
        // when all the items from current channel are consumed, cancel every GroupedMulti (to stop the computation loop)
//        groupedMultiMap.forEach { _, u -> u.producerJob.cancel()  }
        groupedMultiMap.forEach { entry -> entry.value.channel.close() }
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