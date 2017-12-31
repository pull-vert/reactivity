package reactivity.experimental

/**
 * This is the interface declaring the callback functions
 * related to each functions of [Subscriber] & [Subscription]
 * will be implemented in both [Multi] and [SoloPublisher]
 */
interface WithCallbacks<T> {

    fun doOnNext(onNext: (T) -> Unit): WithCallbacks<T>

    fun doOnError(onError: (Throwable) -> Unit): WithCallbacks<T>

    fun doOnComplete(onComplete: () -> Unit): WithCallbacks<T>

    fun doOnCancel(onCancel: () -> Unit): WithCallbacks<T>

    fun doOnRequest(onRequest: (Long) -> Unit): WithCallbacks<T>

    fun doFinally(finally: () -> Unit): WithCallbacks<T>
}