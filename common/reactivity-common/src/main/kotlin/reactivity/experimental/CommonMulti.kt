package reactivity.experimental

import kotlinx.coroutines.experimental.DisposableHandle
import kotlinx.coroutines.experimental.Job

///**
// * Creates cold reactive [Multi] that runs a given [block] in a coroutine.
// * Every time the returned publisher is subscribed, it starts a new coroutine in the specified [scheduler].
// * Coroutine emits items with `send`. Unsubscribing cancels running coroutine.
// *
// * Invocations of `send` are suspended appropriately when subscribers apply back-pressure and to ensure that
// * `onNext` is not invoked concurrently.
// *
// * | **Coroutine action**                         | **Signal to subscriber**
// * | -------------------------------------------- | ------------------------
// * | `value in end of coroutine is not null`      | `onNext`
// * | Normal completion or `close` without cause   | `onComplete`
// * | Failure with exception or `close` with cause | `onError`
// */
//expect fun <T> multi(
//        scheduler: Scheduler,
//        key: Multi.Key<*>? = null,
//        parent: Job? = null,
//        block: suspend ProducerScope<T>.() -> Unit
//): Multi<T>
//
//expect interface ProducerScope<in T>

/**
 * Multi values Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
expect interface Multi<T>: CommonPublisherCommons<T>, Publisher<T> {

    /**
     * Subscribe to this [Publisher], the Reactive Stream starts
     * emitting items until [Subscriber.onComplete] or [Subscriber.onError]
     * @param onNext the function to execute for each data of the stream
     * @param onError the function to execute if the stream ends with an error
     * @param onComplete the function to execute if stream ends successfully
     * @param onSubscribe the function to execute every time the stream is subscribed
     */
    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): DisposableHandle

    /**
     * Key for identifiying common grouped elements, used by [Multi.groupBy] operator
     */
    val key: Key<*>?

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
     * Returns a [Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param mapper the mapper function
     */
    fun <R> map(mapper: (T) -> R): Multi<R>

    /**
     * Returns a [Multi] that delays each received element
     *
     * @param time the delay time
     */
    fun delay(time: Int): Multi<T>

    /**
     * Returns a [Multi] that performs the specified [action] for each received element.
     *
     * @param block the function
     */
    fun peek(action: (T) -> Unit): Multi<T>

    /**
     * Returns a [Multi] that filters received elements, sending it
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
     * Returns a [Multi]<R> that use the [mapper] to transform each received element from [T]
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
    fun <U> takeUntil(other: Publisher<U>): Multi<T>

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
     * Returns a [Multi] that can contain several [Multi]
     * , each is a group of received elements from the source stream that are related with the same [Key]
     *
     * @param keyMapper a function that extracts the key for each item
     */
    fun <R> groupBy(keyMapper: (T) -> R): Multi<out Multi<T>>

    // Combined Operators

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R>

    /**
     * Key for identifiying common grouped elements of this [Multi]. [E] is key type.
     */
    class Key<R>(value: R)
}