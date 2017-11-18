package reactivity.experimental

expect abstract class Multi<T> : PublisherCommons<T> {

    // functions from WithCallbacks

    abstract override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>
    abstract override fun doOnNext(onNext: (T) -> Unit): Multi<T>
    abstract override fun doOnError(onError: (Throwable) -> Unit): Multi<T>
    abstract override fun doOnComplete(onComplete: () -> Unit): Multi<T>
    abstract override fun doOnCancel(onCancel: () -> Unit): Multi<T>
    abstract override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>
    abstract override fun doFinally(finally: () -> Unit): Multi<T>

    // Operators

    /**
     * Returns a [Multi] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    abstract override fun publishOn(delayError: Boolean): Multi<T>
    /**
     * Returns a [Multi] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T>
    /**
     * Returns a [Multi] that is published with [initialScheduler],
     * the [delayError] option and the [prefetch] items
     *
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    abstract fun publishOn(delayError: Boolean, prefetch: Int): Multi<T>
    /**
     * Returns a [Multi] that is published with the provided [scheduler],
     * the [delayError] option and the [prefetch] items
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     * @param prefetch number of items to request. When obtained, request this number again and so on
     * until all items are received
     */
    abstract fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T>

    // Operators

    /**
     * Returns a [Multi] that uses the [mapper] to transform each received element from [T]
     * to [R] and then send it when transformation is done
     *
     * @param mapper the mapper function
     */
    abstract fun <R> map(mapper: (T) -> R): Multi<R>
    /**
     * Returns a [Multi] that filters each received element, sending it
     * only if [predicate] is satisfied
     *
     * @param predicate the filter predicate
     */
    abstract fun filter(predicate: (T) -> Boolean): Multi<T>
    /**
     * Returns a [Solo] containing the first received element that satisfies the given [predicate],
     * or empty if no received element satisfies it
     *
     * @param predicate the filter predicate
     */
    abstract fun findFirst(predicate: (T) -> Boolean): Solo<T?>
    /**
     * Returns a [Multi]<R> that use the [mapper] to transform each received element from [T]
     * to [Publisher]<R> and then send each received element of this [Publisher]
     *
     * @param mapper the mapper function
     */
    abstract fun <R> flatMap(mapper: (T) -> Publisher<R>): Multi<R>
    /**
     * Returns a [Multi] that relay all the received elements from the source stream until the
     * other stream either completes or emits anything
     *
     * @param other the other publisher
     */
    abstract fun <R> takeUntil(other: Publisher<R>): Multi<T>
    /**
     * Returns a [Multi] that flattens the source streams with the parameter [Publisher] into
     * a single Publisher, without any transformation
     *
     * @param others the other publishers
     */
    abstract fun mergeWith(vararg others: Publisher<T>): Multi<T>
    /**
     * Returns a [Multi] that will send the [n] first received elements from the source stream
     *
     * @param n number of items to send
     */
    abstract fun take(n: Long): Multi<T>
    /**
     * Returns a [Multi] that can contain several [MultiGrouped]
     * , each is a group of received elements from the source stream that are related with the same key
     *
     * @param keyMapper a function that extracts the key for each item
     */
    abstract fun <R> groupBy(keyMapper: (T) -> R): Multi<out MultiGrouped<T, R>>

    // Combined Operators

    /**
     * Returns a [Multi] that filters each received element, sending it only if [predicate] is satisfied,
     * if so it uses the [mapper] to transform each element from [T] to [R] type
     *
     * @param predicate the filter predicate
     * @param mapper the mapper function
     */
    abstract fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R>
}