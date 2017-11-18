package reactivity.experimental.common

expect abstract class Multi<T>: DefaultMulti<T> {

    // functions from WithCallbacks

    abstract override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Multi<T>
    abstract override fun doOnNext(onNext: (T) -> Unit): Multi<T>
    abstract override fun doOnError(onError: (Throwable) -> Unit): Multi<T>
    abstract override fun doOnComplete(onComplete: () -> Unit): Multi<T>
    abstract override fun doOnCancel(onCancel: () -> Unit): Multi<T>
    abstract override fun doOnRequest(onRequest: (Long) -> Unit): Multi<T>
    abstract override fun doFinally(finally: () -> Unit): Multi<T>

    // Operators

    abstract override fun publishOn(delayError: Boolean): Multi<T>
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean): Multi<T>
    abstract override fun publishOn(delayError: Boolean, prefetch: Int): Multi<T>
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): Multi<T>
    abstract override fun <R> map(mapper: (T) -> R): Multi<R>
    abstract override fun filter(predicate: (T) -> Boolean): Multi<T>
    abstract override fun findFirst(predicate: (T) -> Boolean): Solo<T?>
    abstract override fun <R> flatMap(mapper: (T) -> Publisher<R>): Multi<R>
    abstract override fun <R> takeUntil(other: Publisher<R>): Multi<T>
    abstract override fun mergeWith(vararg others: Publisher<T>): Multi<T>
    abstract override fun take(n: Long): Multi<T>
    abstract override fun <R> groupBy(keyMapper: (T) -> R): Multi<out MultiGrouped<T, R>>

    // Combined Operators

    abstract override fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): Multi<R>
}