package reactivity.experimental.common

expect interface Publisher<T> {
    fun subscribe(s: Subscriber<in T>)
}

expect interface Subscriber<T> {
    fun onSubscribe(s: Subscription)
    fun onNext(t: T)
    fun onError(t: Throwable)
    fun onComplete()
}

expect interface Subscription {
    fun request(n: Long)
    fun cancel()
}

expect interface Processor<T, R> : Subscriber<T>, Publisher<R>