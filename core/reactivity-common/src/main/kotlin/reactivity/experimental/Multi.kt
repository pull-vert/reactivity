package reactivity.experimental

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
): Multi<T> = MultiImpl(publish(scheduler.context, block), scheduler)

interface IMulti<T> : PublisherCommons<T> {

    // functions from WithCallbacks

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>
    override fun doOnNext(onNext: (T) -> Unit): Multi<T>
    override fun doOnError(onError: (Throwable) -> Unit): Multi<T>
    override fun doOnComplete(onComplete: () -> Unit): Multi<T>
    override fun doOnCancel(onCancel: () -> Unit): Multi<T>
    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>
    override fun doFinally(finally: () -> Unit): Multi<T>

    // Operators

    /**
     * Returns a [Multi] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    override fun publishOn(delayError: Boolean): Multi<T>
    /**
     * Returns a [Multi] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T>
    /**
     * Returns a [Multi] that is published with [initialScheduler],
     * the [delayError] option and the [prefetch] items
     *
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    fun publishOn(delayError: Boolean, prefetch: Int): Multi<T>
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

    // Operators

    /**
     * Returns a [Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param mapper the mapper function
     */
    fun <R> map(mapper: (T) -> R): Multi<R>
    /**
     * Returns a [Multi] that filters each received element, sending it
     * only if [predicate] is satisfied
     *
     * @param predicate the filter predicate
     */
    fun filter(predicate: (T) -> Boolean): Multi<T>
    /**
     * Returns a [Solo] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param predicate the filter predicate
     */
    fun findFirst(predicate: (T) -> Boolean): Solo<T?>
    /**
     * Returns a [Multi]<R> that useCloseable the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param mapper the mapper function
     */
    fun <R> flatMap(mapper: (T) -> Publisher<R>): Multi<R>
    /**
     * Returns a [Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param other the other publisher
     */
    fun <R> takeUntil(other: Publisher<R>): Multi<T>
    /**
     * Returns a [Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param others the other publishers
     */
    fun mergeWith(vararg others: Publisher<T>): Multi<T>
    /**
     * Returns a [Multi] that will send the [n] first received elements from the source stream
     *
     * @param n number of items to send
     */
    fun take(n: Long): Multi<T>
    /**
     * Returns a [Multi] that can contain several [MultiGrouped]
     * , each is a group of received elements from the source stream that are related with the same key
     *
     * @param keyMapper a function that extracts the key for each item
     */
    fun <R> groupBy(keyMapper: (T) -> R): Multi<out MultiGrouped<T, R>>

    // Combined Operators

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R>
}

internal class MultiImpl<T>(override val delegate: Publisher<T>,
                        override val initialScheduler: Scheduler)
    : AMulti<T>, Publisher<T> by delegate

interface IMultiImpl<T> : IMulti<T> {

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Multi<*>)) {
            (delegate as PublisherWithCallbacks<T>).onSubscribeBlock = onSubscribe
            return this as Multi<T>
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onSubscribeBlock = onSubscribe
        return MultiImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnNext(onNext: (T) -> Unit): Multi<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Multi<*>)) {
            (delegate as PublisherWithCallbacks<T>).onNextBlock = onNext
            return this as Multi<T>
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onNextBlock = onNext
        return MultiImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnError(onError: (Throwable) -> Unit): Multi<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Multi<*>)) {
            (delegate as PublisherWithCallbacks<T>).onErrorBlock = onError
            return this as Multi<T>
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onErrorBlock = onError
        return MultiImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnComplete(onComplete: () -> Unit): Multi<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Multi<*>)) {
            (delegate as PublisherWithCallbacks<T>).onCompleteBlock = onComplete
            return this as Multi<T>
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCompleteBlock = onComplete
        return MultiImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnCancel(onCancel: () -> Unit): Multi<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Multi<*>)) {
            (delegate as PublisherWithCallbacks<T>).onCancelBlock = onCancel
            return this as Multi<T>
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onCancelBlock = onCancel
        return MultiImpl(publisherCallbacks, initialScheduler)
    }

    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T> {
        if ((delegate is PublisherWithCallbacks) && (this is Multi<*>)) {
            (delegate as PublisherWithCallbacks<T>).onRequestBlock = onRequest
            return this as Multi<T>
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.onRequestBlock = onRequest
        return MultiImpl(publisherCallbacks, initialScheduler)
    }

    override fun doFinally(finally: () -> Unit): Multi<T> {
        if (delegate is PublisherWithCallbacks) {
            (delegate as PublisherWithCallbacks<T>).finallyBlock = finally
            return this as Multi<T>
        }
        // otherwise this is not a PublisherWithCallbacks
        val publisherCallbacks = PublisherWithCallbacks(this)
        publisherCallbacks.finallyBlock = finally
        return MultiImpl(publisherCallbacks, initialScheduler)
    }

    override fun publishOn(delayError: Boolean) = publishOn(initialScheduler, delayError)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean) = multi(scheduler) {
        val channel = PublisherPublishOn<T>(delayError, Int.MAX_VALUE)
        this@IMultiImpl.subscribe(channel)
        channel.consumeEach {
            send(it)
        }
    }

    override fun publishOn(delayError: Boolean, prefetch: Int) = publishOn(initialScheduler, delayError, prefetch)

    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int) = multi(scheduler) {
        val channel = PublisherPublishOn<T>(delayError, prefetch)
        this@IMultiImpl.subscribe(channel)
        channel.useCloseable { chan ->
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

    override fun <R> map(mapper: (T) -> R) = multi(initialScheduler) {
        consumeEach {
            // consume the source stream
            send(mapper(it))     // map
        }
    }

    override fun filter(predicate: (T) -> Boolean) = multi(initialScheduler) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter
                send(it)
        }
    }

    override fun findFirst(predicate: (T) -> Boolean) = solo(initialScheduler) {
        var produced = false
        openSubscription().useCloseable { channel ->
            // open channel to the source
            for (x in channel) { // iterate over the channel to receive elements from it
                if (predicate(x)) {       // filter 1 item
                    send(x)
                    produced = true
                    break
                }
                // `useCloseable` will close the channel when this block of code is complete
            }
            if (!produced) send(null)
        }
        // TODO make a unit test to verify what happends when no item satisfies the predicate
    }

    override fun <R> flatMap(mapper: (T) -> Publisher<R>) = multi(initialScheduler) {
        consumeEach {
            // consume the source stream
            val pub = mapper(it)
            launch(coroutineContext) {
                // launch a child coroutine
                pub.consumeEach { send(it) }    // send every element from this publisher
            }
        }
    }

    override fun <R> takeUntil(other: Publisher<R>) = multi(initialScheduler) {
        openSubscription().useCloseable { thisChannel ->
            // explicitly open channel to Publisher<T>
            other.openSubscription().useCloseable { otherChannel ->
                // explicitly open channel to Publisher<U>
                whileSelect {
                    otherChannel.onReceive { false }          // bail out on any received element from `other`
                    thisChannel.onReceive { send(it); true }  // resend element from this channel and continue
                }
            }
        }
    }

    override fun mergeWith(vararg others: Publisher<T>) = multi(initialScheduler) {
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

    override fun take(n: Long) = multi(initialScheduler) {
        openSubscription().useCloseable { channel ->
            // explicitly open channel to Publisher<T>
            var count = 0L
            for (c in channel) {
                send(c)
                count++
                if (count == n) break
            }
            // `useCloseable` will close the channel when this block of code is complete
        }
    }

    override fun <R> groupBy(keyMapper: (T) -> R) = multi(initialScheduler) {
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
                channel = Channel(Int.MAX_VALUE) // To avoid having to expect the companion object UNLIMITED of Channel
                // Converts a stream of elements received from the channel to the hot reactive publisher
                send(MultiGroupedImpl(channel.asPublisher(coroutineContext).toMulti(initialScheduler), key) as MultiGrouped<T, R>)
                channelMap[key] = channel // adds to Map
            }

            channel.send(it)
        }
        // when all the items from source stream are consumed, close every channels (to stop the computation loop)
        channelMap.forEach { u -> u.value.close(null) }
    }

    // Combined Operators

    override fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R) = multi(initialScheduler) {
        consumeEach {
            // consume the source stream
            if (predicate(it))       // filter part
                send(mapper(it))     // map part
        }
    }
}