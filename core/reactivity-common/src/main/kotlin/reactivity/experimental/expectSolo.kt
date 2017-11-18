package reactivity.experimental

expect abstract class Solo<T> : PublisherCommons<T> {
    // functions from WithCallbacks
    abstract override fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>
    abstract override fun doOnNext(onNext: (T) -> Unit): Solo<T>
    abstract override fun doOnError(onError: (Throwable) -> Unit): Solo<T>
    abstract override fun doOnComplete(onComplete: () -> Unit): Solo<T>
    abstract override fun doOnCancel(onCancel: () -> Unit): Solo<T>
    abstract override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>
    abstract override fun doFinally(finally: () -> Unit): Solo<T>

    // function from WithPublishOn
    /**
     * Returns a [Solo] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    abstract override fun publishOn(delayError: Boolean): Solo<T>

    /**
     * Returns a [Solo] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    abstract override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>
}