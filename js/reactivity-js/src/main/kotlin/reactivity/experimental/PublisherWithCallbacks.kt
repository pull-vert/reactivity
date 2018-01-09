package reactivity.experimental

internal class PublisherWithCallbacks<T> internal constructor(private val delegate: Publisher<T>) : Publisher<T> {

    override fun subscribe(s: Subscriber<in T>) {
        delegate.subscribe(SubscriberCallbacks(this, s))
    }

    // callbacks
    internal var onSubscribeBlock: ((Subscription) -> Unit)? = null
    internal var onNextBlock: ((T) -> Unit)? = null
    internal var onErrorBlock: ((Throwable) -> Unit)? = null
    internal var onCompleteBlock: (() -> Unit)? = null
    internal var onCancelBlock: (() -> Unit)? = null
    internal var onRequestBlock: ((Long) -> Unit)? = null
    internal var finallyBlock: (() -> Unit)? = null
}

private class SubscriberCallbacks<T> internal constructor(val parent: PublisherWithCallbacks<T>,
                                                          val actual: Subscriber<in T>) : Subscription, Subscriber<T> {

    var subscription: Subscription? = null
    @Volatile
    var done: Boolean = false
    @Volatile
    var _finally: Boolean = false

    // Subscription functions
    override fun request(n: Long) {
        try {
            parent.onRequestBlock?.invoke(n)
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e))
            return
        }
        subscription?.request(n)
    }

    override fun cancel() {
        try {
            parent.onCancelBlock?.invoke()
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e))
            return
        }
        subscription?.cancel()

        if (parent.finallyBlock != null) runFinally()
    }

    // Subscriber functions
    override fun onSubscribe(s: Subscription) {
        if (validateSubscription(subscription, s)) {
            try {
                parent.onSubscribeBlock?.invoke(s)
            } catch (e: Throwable) {
                errorInOnSubscribe(actual, onOperatorError(s, e))
                return
            }
            subscription = s
            actual.onSubscribe(this)
        }
    }

    override fun onNext(t: T) {
        if (done) {
            onNextDropped()
            return
        }
        try {
            parent.onNextBlock?.invoke(t)
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e))
            return
        }
        actual.onNext(t)
    }

    override fun onError(t: Throwable) {
        if (done) {
            onErrorDropped(t)
            return
        }
        done = true
        var throwable = t
        if (parent.onErrorBlock != null) {
            Exceptions.throwIfFatal(t)
            try {
                parent.onErrorBlock?.invoke(t)
            } catch (e: Throwable) {
                //this performs a throwIfFatal or suppresses t in e
                throwable = onOperatorError(null, e)
            }
        }
        try {
            actual.onError(throwable)
        } catch (use: UnsupportedOperationException) {
            if (parent.onErrorBlock == null || !Exceptions.isErrorCallbackNotImplemented(use) && use.cause !== throwable) {
                throw use
            }
        }

        if (parent.finallyBlock != null) runFinally()
    }

    override fun onComplete() {
        if (done) {
            return
        }
        try {
            parent.onCompleteBlock?.invoke()
        } catch (e: Throwable) {
            onError(onOperatorError(subscription, e))
            return
        }
        done = true

        actual.onComplete()

        if (parent.finallyBlock != null) runFinally()
    }

    private fun runFinally() {
        if (!_finally) {
            _finally = true
            try {
                parent.finallyBlock?.invoke()
            } catch (e: Throwable) {
                Exceptions.throwIfFatal(e)
                onErrorDropped(e)
            }
        }
    }

}