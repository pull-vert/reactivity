package reactivity.experimental

import kotlinx.coroutines.experimental.DisposableHandle
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

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
@Suppress("EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER")
expect fun <T> multi(
        scheduler: Scheduler,
        key: Multi.Key<*>? = null,
        parent: Job? = null,
        block: suspend ProducerScope<T>.() -> Unit
): Multi<T>

interface CommonMultiOperators<T>: CommonPublisher<T> {

    /**
     * Key for identifiying common grouped elements, used by [Multi.groupBy] operator
     */
    val key: Multi.Key<*>?

    // Combined Operators

    /**
     * Returns a [Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param mapper the mapper function
     */
    fun <R> map(mapper: (T) -> R) = multi(initialScheduler, key) {
        println("initial scheduler = ${initialScheduler.context}")
        commonConsumeEach {
            // consume the source stream
            send(mapper(it))     // map
        }
    }

    /**
     * Returns a [Multi] that delays each received element
     *
     * @param time the delay time
     */
    fun delay(time: Int) = multi(initialScheduler, key) {
        commonConsumeEach {
            // consume the source stream
            kotlinx.coroutines.experimental.delay(time) // delay
            send(it) // send
        }
    }

    /**
     * Returns a [Multi] that performs the specified [action] for each received element.
     *
     * @param action the function
     */
    fun peek(action: (T) -> Unit) = multi(initialScheduler, key) {
        commonConsumeEach {
            // consume the source stream
            action(it)
            send(it)     // peek
        }
    }

    /**
     * Returns a [Multi] that filters received elements, sending it
     * only if [predicate] is satisfied
     *
     * @param predicate the filter predicate
     */
    fun filter(predicate: (T) -> Boolean) = multi(initialScheduler, key) {
        commonConsumeEach {
            // consume the source stream
            if (predicate(it))       // filter
                send(it)
        }
    }

    /**
     * Returns a [Solo] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param predicate the filter predicate
     */
    fun findFirst(predicate: (T) -> Boolean) = solo(initialScheduler) {
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

    /**
     * Returns a [Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param mapper the mapper function
     */
    fun <R> flatMap(mapper: (T) -> Publisher<R>) = multi(initialScheduler, key) {
        commonConsumeEach {
            // consume the source stream
            val pub = mapper(it)
            launch(coroutineContext) {
                // launch a child coroutine
                pub.consumeEach { send(it) }    // send every element from this publisher
            }
        }
    }

    /**
     * Returns a [Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param others the other publishers
     */
    fun mergeWith(vararg others: Publisher<T>) = multi(initialScheduler, key) {
        launch(coroutineContext) {
            /** launch a first child coroutine for this [Multi] */
            commonConsumeEach {
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

    /**
     * Returns a [Multi] that will send the [n] first received elements from the source stream
     *
     * @param n number of items to send
     */
    fun take(n: Long) = multi(initialScheduler, key) {
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

    /**
     * Returns a [Multi] that can contain several [Multi]
     * , each is a group of received elements from the source stream that are related with the same [Multi.Key]
     *
     * @param keyMapper a function that extracts the key for each item
     */
    fun <R> groupBy(keyMapper: (T) -> R) = multi(initialScheduler, key) {
        var key: R
        var channel: Channel<T>
        val channelMap = mutableMapOf<R, Channel<T>>()
        commonConsumeEach {
            // consume the source stream
            key = keyMapper(it)
            if (channelMap.containsKey(key)) { // this channel exists already
                channel = channelMap[key]!!
            } else { // have to create a new MultiPublisherGrouped
                /** Creates a [kotlinx.coroutines.experimental.channels.LinkedListChannel] */
                channel = Channel(Int.MAX_VALUE)
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

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R) = multi(initialScheduler, key) {
        commonConsumeEach {
            // consume the source stream
            if (predicate(it))       // filter part
                send(mapper(it))     // map part
        }
    }
}

/**
 * Multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
expect interface Multi<T>: CommonMultiOperators<T>, Publisher<T> {

    /**
     * Subscribe to this [Publisher], the Reactive Stream starts
     * emitting items until [Subscriber.onComplete] or [Subscriber.onError]
     * @param onNext the function to execute for each data of the stream
     * @param onError the function to execute if the stream ends with an error
     * @param onComplete the function to execute if stream ends successfully
     * @param onSubscribe the function to execute every time the stream is subscribed
     */
    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): DisposableHandle

    fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>

    override fun doOnNext(onNext: (T) -> Unit): Multi<T>

    override fun doOnError(onError: (Throwable) -> Unit): Multi<T>

    override fun doOnComplete(onComplete: () -> Unit): Multi<T>

    override fun doOnCancel(onCancel: () -> Unit): Multi<T>

    override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>

    override fun doFinally(finally: () -> Unit): Multi<T>

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
     * the [delayError] option and prefetch the [prefetch] first items before sending them
     *
     * @param delayError if error should be delayed
     * @param prefetch number of items to request at first. When obtained, request all remaining items
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
     * Returns a [Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param other the other publisher
     */
    fun <U> takeUntil(other: Publisher<U>): Multi<T>

    /**
     * Key for identifiying common grouped elements of this [Multi]. [R] is key type.
     */
    class Key<out R>(value: R)
}