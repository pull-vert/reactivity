package reactivity.experimental

abstract class MultiGrouped<T, out U> : Multi<T>() {
    abstract val key: U
    // functions from WithCallbacks

    abstract override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): MultiGrouped<T, U>
    abstract override fun doOnNext(onNext: (T) -> Unit): MultiGrouped<T, U>
    abstract override fun doOnError(onError: (Throwable) -> Unit): MultiGrouped<T, U>
    abstract override fun doOnComplete(onComplete: () -> Unit): MultiGrouped<T, U>
    abstract override fun doOnCancel(onCancel: () -> Unit): MultiGrouped<T, U>
    abstract override fun doOnRequest(onRequest: (Long) -> Unit): MultiGrouped<T, U>
    abstract override fun doFinally(finally: () -> Unit): MultiGrouped<T, U>

    // Operators

    abstract override fun publishOn(delayError: Boolean): MultiGrouped<T, U>
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean): MultiGrouped<T, U>
    abstract override fun publishOn(delayError: Boolean, prefetch: Int): MultiGrouped<T, U>
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): MultiGrouped<T, U>
    abstract override fun <R> map(mapper: (T) -> R): MultiGrouped<R, U>
    abstract override fun filter(predicate: (T) -> Boolean): MultiGrouped<T, U>
    abstract override fun findFirst(predicate: (T) -> Boolean): Solo<T?>
    abstract override fun <R> flatMap(mapper: (T) -> Publisher<R>): Multi<R>
    abstract override fun <R> takeUntil(other: Publisher<R>): MultiGrouped<T, U>
    abstract override fun mergeWith(vararg others: Publisher<T>): MultiGrouped<T, U>
    abstract override fun take(n: Long): MultiGrouped<T, U>
    abstract override fun <R> groupBy(keyMapper: (T) -> R): MultiGrouped<out MultiGrouped<T, R>, U>

    // Combined Operators

    abstract override fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R): MultiGrouped<R, U>
}

internal class MultiGroupedImpl<T, out U> internal constructor(private val del: Multi<T>, override val key: U)
    : MultiGrouped<T, U>(), Publisher<T> by del.delegate {
    override val delegate: Publisher<T>
        get() = del.delegate
    override val initialScheduler: Scheduler
        get() = del.initialScheduler

    // functions from WithCallbacks

    override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).doOnSubscribe(onSubscribe), key)
    override fun doOnNext(onNext: (T) -> Unit): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).doOnNext(onNext), key)
    override fun doOnError(onError: (Throwable) -> Unit): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).doOnError(onError), key)
    override fun doOnComplete(onComplete: () -> Unit): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).doOnComplete(onComplete), key)
    override fun doOnCancel(onCancel: () -> Unit): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).doOnCancel(onCancel), key)
    override fun doOnRequest(onRequest: (Long) -> Unit): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).doOnRequest(onRequest), key)
    override fun doFinally(finally: () -> Unit): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).doFinally(finally), key)

    // Operators

    override fun publishOn(delayError: Boolean): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).publishOn(delayError), key)
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).publishOn(scheduler, delayError), key)
    override fun publishOn(delayError: Boolean, prefetch: Int): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).publishOn(delayError, prefetch), key)
    override fun publishOn(scheduler: Scheduler, delayError: Boolean, prefetch: Int): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).publishOn(scheduler, delayError, prefetch), key)
    override fun <R> map(mapper: (T) -> R): MultiGrouped<R, U> = MultiGroupedImpl((this as Multi<T>).map(mapper), key)
    override fun filter(predicate: (T) -> Boolean): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).filter(predicate), key)
    override fun findFirst(predicate: (T) -> Boolean): Solo<T?> = SoloImpl((this as Multi<T>).findFirst(predicate), initialScheduler)
    override fun <R> flatMap(mapper: (T) -> Publisher<R>): MultiGrouped<R, U> = MultiGroupedImpl((this as Multi<T>).flatMap(mapper), key)
    override fun <R> takeUntil(other: Publisher<R>): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).takeUntil(other), key)
    override fun mergeWith(vararg others: Publisher<T>): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).mergeWith(*others), key)
    override fun take(n: Long): MultiGrouped<T, U> = MultiGroupedImpl((this as Multi<T>).take(n), key)
    override fun <R> groupBy(keyMapper: (T) -> R): MultiGroupedImpl<out MultiGrouped<T, R>, U> = MultiGroupedImpl((this as Multi<T>).groupBy(keyMapper), key)

    // Combined Operators

    override fun <R> fusedFilterMap(predicate: (T) -> Boolean, mapper: (T) -> R):MultiGrouped<R, U> = MultiGroupedImpl((this as Multi<T>).fusedFilterMap(predicate, mapper), key)
}