package reactivity.experimental

actual interface Subscription {
    actual fun request(n: Long)
    actual fun cancel()
}

actual interface Subscriber<T> {
    actual fun onSubscribe(s: Subscription)
    actual fun onNext(t: T)
    actual fun onError(t: Throwable)
    actual fun onComplete()
}

actual interface Publisher<T> {
    actual fun subscribe(s: Subscriber<in T>)
}