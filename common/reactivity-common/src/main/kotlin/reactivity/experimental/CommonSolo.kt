package reactivity.experimental

import kotlinx.coroutines.experimental.DisposableHandle

/**
 * Single (or empty) value Reactive Stream [Publisher]
 *
 * @author Frédéric Montariol
 */
expect interface Solo<T>: CommonPublisherCommons<T>, Publisher<T> {

    /**
     * Subscribe to this [Publisher], the Reactive Stream starts
     * emitting items until [Subscriber.onComplete] or [Subscriber.onError]
     * @param onNext the function to execute for each data of the stream
     * @param onError the function to execute if the stream ends with an error
     * @param onComplete the function to execute if stream ends successfully
     * @param onSubscribe the function to execute every time the stream is subscribed
     */
    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?, onSubscribe: ((Subscription) -> Unit)?): DisposableHandle

    fun doOnSubscribe(onSubscribe: (Subscription) -> Unit): Solo<T>

    override fun doOnNext(onNext: (T) -> Unit): Solo<T>

    override fun doOnError(onError: (Throwable) -> Unit): Solo<T>

    override fun doOnComplete(onComplete: () -> Unit): Solo<T>

    override fun doOnCancel(onCancel: () -> Unit): Solo<T>

    override fun doOnRequest(onRequest: (Long) -> Unit): Solo<T>

    override fun doFinally(finally: () -> Unit): Solo<T>

    /**
     * Returns a [Solo] that is published with [initialScheduler] and the [delayError] option
     *
     * @param delayError if error should be delayed
     */
    override fun publishOn(delayError: Boolean): Solo<T>

    /**
     * Returns a [Solo] that is published with the provided [scheduler] and the [delayError] option
     *
     * @param scheduler the scheduler containing the coroutine context to execute this coroutine in
     * @param delayError if error should be delayed
     */
    override fun publishOn(scheduler: Scheduler, delayError: Boolean): Solo<T>
}