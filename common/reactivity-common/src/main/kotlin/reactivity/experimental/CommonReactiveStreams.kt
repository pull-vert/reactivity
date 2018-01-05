package reactivity.experimental

expect interface Subscription {
    fun request(n: Long)
    fun cancel()
}

expect interface Subscriber<T> {
    fun onSubscribe(s: Subscription)
    fun onNext(t: T)
    fun onError(t: Throwable)
    fun onComplete()
}

expect interface Publisher<T>