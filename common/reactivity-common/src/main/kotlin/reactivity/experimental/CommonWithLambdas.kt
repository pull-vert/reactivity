package reactivity.experimental

import kotlinx.coroutines.experimental.DisposableHandle

/**
 * This is the interface declaring the callback functions
 * related to each functions of [Subscriber] & [Subscription]
 * will be implemented in both [Multi] and [SoloPublisher]
 */
expect interface WithLambdas<T> {
    // Methods for Publisher with lambdas
    /**
     * Subscribe to this [Publisher], the Reactive Stream starts
     * emitting items until [Subscriber.onComplete] or [Subscriber.onError]
     */
    fun subscribe(): DisposableHandle

    /**
     * Subscribe to this [Publisher], the Reactive Stream starts
     * emitting items until [Subscriber.onComplete] or [Subscriber.onError]
     * @param onNext the function to execute for each data of the stream
     */
    fun subscribe(onNext: (T) -> Unit): DisposableHandle

    /**
     * Subscribe to this [Publisher], the Reactive Stream starts
     * emitting items until [Subscriber.onComplete] or [Subscriber.onError]
     * @param onNext the function to execute for each data of the stream
     * @param onError the function to execute if stream ends with an error
     */
    fun subscribe(onNext: ((T) -> Unit)?, onError: (Throwable) -> Unit): DisposableHandle

    /**
     * Subscribe to this [Publisher], the Reactive Stream starts
     * emitting items until [Subscriber.onComplete] or [Subscriber.onError]
     * @param onNext the function to execute for each data of the stream
     * @param onError the function to execute if the stream ends with an error
     * @param onComplete the function to execute if stream ends successfully
     */
    fun subscribe(onNext: ((T) -> Unit)?, onError: ((Throwable) -> Unit)?, onComplete: (() -> Unit)?): DisposableHandle
}