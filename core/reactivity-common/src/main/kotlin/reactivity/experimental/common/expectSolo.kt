package reactivity.experimental.common

expect abstract class Solo<T>: DefaultSolo<T> {
    // functions from WithCallbacks
    abstract override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>
    abstract override fun doOnNext(onNext: (T) -> Unit): Solo<T>
    abstract override fun doOnError(onError: (Throwable) -> Unit): Solo<T>
    abstract override fun doOnComplete(onComplete: () -> Unit): Solo<T>
    abstract override fun doOnCancel(onCancel: () -> Unit): Solo<T>
    abstract override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>
    abstract override fun doFinally(finally: () -> Unit): Solo<T>

    // function from WithPublishOn
    abstract override fun publishOn(delayError: Boolean): Solo<T>
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>
}